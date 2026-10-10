# ScoutPro — what was added, and how a request moves through the system

Hibernate still updates the database itself (`spring.jpa.hibernate.ddl-auto=update`). There is no Flyway. On the next startup Hibernate creates the new `refresh_tokens` table and any new columns.

These additions follow the project documentation (requirement 6, FR1, FR9, the RBAC matrix, and the audit/notification design). A new password signup now emails a 6-digit code and the user enters it before any token is issued. Google sign-in still issues tokens immediately, because Google has already confirmed the address. The API still does **not** keep an HTTP session for a signed-in user. Accounts that already existed keep a null `email_verified` and can still sign in with their password.

---

## 1. What was added

### Access token and refresh token

- The access JWT now lasts **15 minutes** (`jwt.expiration-minutes`, default 15).
- Login, registration, and Google sign-in also return a **refresh token** that lasts **7 days**.
- Only the SHA-256 hash of that refresh token is stored, in PostgreSQL table `refresh_tokens`. The raw token is given to the browser once. SHA-256 is used because the server must find the row by the hash. BCrypt cannot be searched, so it is the wrong tool here. Passwords are still BCrypt.
- `POST /api/v1/auth/refresh` checks the hash, revokes that refresh token, and returns a new access token and a new refresh token.
- `POST /api/v1/auth/logout` revokes the refresh token.
- The React app stores both tokens in `localStorage` (`scoutpro_token` and `scoutpro_refresh`). About 30 seconds before the access token expires, or when an API call returns 401, the app calls `/auth/refresh` and keeps the user signed in. If the refresh token is missing, revoked, or older than 7 days, the user goes back to the login page.
- Google still opens a short HTTP session only for the handshake with Google. That session is thrown away as soon as the two tokens are issued. Later API calls use the access JWT.

### Email, including a real inbox

- Defaults are unchanged: mail goes to Mailpit (`localhost:1025`, inbox http://localhost:8025).
- To deliver to a real inbox, set these in `ScoutPro_27202/secrets.properties` (that file is git-ignored) and restart the backend:

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your.address@gmail.com
MAIL_PASSWORD=the-app-password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_FROM=your.address@gmail.com
```

Use a provider app password, not a normal mailbox password. Do not commit those values.

- `user.registered` now sends two emails: a welcome message to the new user, and an alert to `app.admin.email` (default `admin@scoutpro.rw`) asking an admin to assign a role.
- `athlete.deactivated` emails every scout who has assessed that athlete.

### Athlete sees only their own profile

- `GET /api/v1/athletes/me` returns the athlete row linked to the logged-in user, or 404 if none is linked.
- `GET /api/v1/athletes/{id}`, the physical profile, the assessment history, and the reports allow role `ATHLETE` only when that row’s `user` is the caller. Staff behavior is unchanged.
- Report JSON for an athlete contains the summary. Strengths, weaknesses, tags, and media are left empty.
- `PATCH /api/v1/athletes/me/contact` lets the athlete change their own contact number.
- `PUT /api/v1/athletes/{id}/account` with `{ "userId": "<uuid>" }` (admin) links a login to an athlete. `userId: null` removes the link. The athlete detail page has this control for an admin.

### Paged lists

Every list endpoint accepts `?page=0&size=20` (size is capped at 100) and returns:

```json
{ "content": [], "totalElements": 0, "totalPages": 0, "number": 0, "size": 20 }
```

The React screens request the first page of 100 rows and display `content`. That covers a demo dataset. Page 2 is available from the API (`page=1`) even though the screens do not yet show a next-page button.

### Redis cache

Ranking, the sports list, and criteria are cached in Redis for 10 minutes. Each cached value includes its Java class name, so Redis can rebuild a `PageResponse` instead of a plain map. Creating or editing an assessment clears the ranking cache. Creating or editing a sport or criterion clears the matching cache.

Start Redis once:

```powershell
docker run -d --name scoutpro_redis -p 6379:6379 redis:7-alpine
```

Later: `docker start scoutpro_redis`.

If Redis is stopped, the API still reads PostgreSQL. The log says the Redis read failed. Nothing is cached until Redis is back.

### Report PDF

`GET /assessments/{assessmentId}/report.pdf` downloads the report the signed-in user is allowed to read. An athlete receives the summary. Staff also receive the scores, strengths, weaknesses and tags. The screen button is **Download PDF**.

### Audit log

Create, update, and delete publish `entity.changed`. After the database commit, RabbitMQ’s audit consumer writes a MongoDB `audit_logs` document with `actorId`, `entity`, `entityId`, `action`, `before`, and `after`. `before` and `after` are small JSON snapshots of the fields that matter, not the whole JPA object.

The older events (`assessment.created`, `athlete.deactivated`, and so on) are still published as well.

---

## 2. Flow of the system

### Password registration and login

1. The browser `POST`s email and password to `/api/v1/auth/register`.
2. Register checks the password length, stores a BCrypt hash, sets `emailVerified` to false, and gives the role `ATHLETE`.
3. It stores the SHA-256 hash of a 6-digit code and publishes `user.registered`. The HTTP response does not wait for the email, and it does not contain a token.
4. The React app opens `/verify-email`. The user types the code from the email. `POST /api/v1/auth/verify-otp` checks the hash, marks the email confirmed, and returns an access JWT (15 minutes) and a refresh token (7 days).
5. Login of an account that already exists (`emailVerified` null or true) still returns those two tokens immediately. An unconfirmed account is told to enter the code.
6. Later calls send `Authorization: Bearer <access JWT>`. Spring Security checks the signature, the expiry, and the roles.

### Staying signed in

1. The access JWT expires.
2. The browser `POST`s the refresh token to `/api/v1/auth/refresh`.
3. The server hashes it, finds the row, rejects it if it is revoked or expired, then revokes it.
4. A new access JWT and a new refresh token are returned. The old refresh token cannot be used again.

Logout revokes the current refresh token and the browser deletes both tokens.

### Google

1. The browser navigates to `http://localhost:8080/oauth2/authorization/google`.
2. Spring holds a temporary session only while Google redirects back.
3. The success handler finds or creates the user (new users are `ATHLETE`), issues both tokens, invalidates that session, and redirects to `http://localhost:5173/oauth2/callback#token=...&refreshToken=...`.
4. The callback page stores the tokens and removes them from the address bar.

### Email and SMS

1. A service publishes a `DomainEvent` inside the application.
2. `RabbitEventRelay` sends it to the exchange `scoutpro.events` only after the database transaction commits.
3. The email queue, SMS queue, and audit queue each receive the routing keys bound in `RabbitConfig`.
4. The email consumer sends through `JavaMailSender` (Mailpit, or the real SMTP host you configured). The SMS consumer still writes the text to the server log.
5. Each attempt is stored in MongoDB `notification_logs`. A repeated delivery of the same event id is ignored. Three failures move the message to a dead-letter queue.

### Athlete profile

1. An admin opens an athlete and chooses a user under “Link a login account”.
2. That user signs in (they already have role `ATHLETE` if they registered themselves).
3. The dashboard calls `GET /athletes/me` and links to their profile.
4. They can change the contact number. They cannot open the full athlete list, ranking, or another person’s profile.

### Cache and audit around a write

1. A scout saves an assessment. PostgreSQL commits.
2. The ranking cache is cleared.
3. `assessment.created` is published (email + the existing audit path) and `entity.changed` is published (the richer audit row).
4. The next ranking request misses the cache, reads PostgreSQL, and stores the page in Redis.
---

## 3. What was left unchanged on purpose

- `ddl-auto=update` (no Flyway). On the next startup Hibernate adds `email_verified`, `otp_hash`, `otp_expires_at`, and `otp_attempts` on `users`.
- Email confirmation for new password accounts. `POST /api/v1/auth/register` saves the user with `emailVerified=false`, stores only the SHA-256 hash of a 6-digit code (10 minutes, 5 tries), and publishes `user.registered`. The welcome email contains the code. The admin alert does not. `POST /api/v1/auth/verify-otp` checks the hash and then returns the access token and refresh token. `POST /api/v1/auth/resend-otp` publishes `user.otp.sent`. Login of an unconfirmed account returns 403 with the message that asks for the code. Google users are stored as confirmed.
- No server session for API calls. The 15-minute JWT plus the 7-day refresh token is the design in the project document. The code page is an extra step before those tokens exist.
