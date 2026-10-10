# CLAUDE.md — ScoutPro

Sports Talent Scouting & Evaluation System. Web Technology final project (AUCA), Ingabire Esther (27202).
This file briefs Claude Code. Read it fully before doing anything.

---

## 0. Ground rules (read first)

1. **Work in small stages and STOP after each stage** so the student can review, test and commit. Never do several stages in one go.
2. **Explain what you build.** The student must defend every line in an oral exam. After each stage, give a short plain-language explanation of each file and the key decisions.
3. **Do not modify the backend (`ScoutPro_27202/`) unless explicitly asked.** It is finished and tested. If the frontend needs a backend change, stop and propose it first.
4. **Never open, print, copy or commit `ScoutPro_27202/secrets.properties`.** It holds Google OAuth credentials and is git-ignored.
5. **Never commit** `frontend/node_modules/`, `frontend/dist/`, `.env` files, or anything under `target/`.
6. **Git workflow:** one branch per stage, branched from `develop` (e.g. `feature/frontend-setup`, `feature/frontend-auth`). Conventional commits (`feat(frontend): ...`, `fix(auth): ...`). Pull requests target **`develop`**, never `main`. Before creating a branch: `git checkout develop && git pull`.
7. **Shell is Windows PowerShell.** Use full paths (`cd C:\Users\Administrator\Desktop\ScoutProFinal\...`). Use `curl.exe`, not `curl`.
8. After each stage run `npm run build` (must pass with no TypeScript errors) and tell the student what to check in the browser.

---

## 1. Repository layout

```
ScoutProFinal/                 ← git root (monorepo)
├── ScoutPro_27202/            ← Spring Boot backend (DONE)
│   ├── pom.xml
│   ├── secrets.properties     ← git-ignored, NEVER touch
│   └── src/main/java/rw/ac/auca/scoutpro_27202/
│       ├── domain/            JPA entities (PostgreSQL)
│       ├── document/          MongoDB documents
│       ├── repository/        Spring Data repositories
│       ├── service/           business rules, @PreAuthorize RBAC
│       ├── controller/        REST controllers (/api/v1/...)
│       ├── dto/               request/response records
│       ├── security/          JWT, OAuth2, CurrentUser
│       ├── messaging/         RabbitMQ topology, publisher, consumers
│       └── config/            DataInitializer (roles + default admin)
├── frontend/                  ← React app (TO BUILD)
├── .gitignore
└── README.md
```

---

## 2. Running the backend (needed for frontend work)

Docker containers (must be running): `scoutpro_db` (PostgreSQL 16, port 5432), `scoutpro_mongo` (MongoDB 7, 27017), `scoutpro_rabbitmq` (RabbitMQ 4 + management, 5672/15672), `scoutpro_mailpit` (fake SMTP 1025, inbox UI http://localhost:8025).

```powershell
docker start scoutpro_db scoutpro_mongo scoutpro_rabbitmq scoutpro_mailpit
cd C:\Users\Administrator\Desktop\ScoutProFinal\ScoutPro_27202
.\mvnw spring-boot:run
```

Backend: **http://localhost:8080**. Only one instance may run (port 8080).
Default admin (seeded at startup): `admin@scoutpro.rw` / `Admin@12345`.

Tech: Spring Boot **4.1** (Spring Security 7, Hibernate 7, Jackson 3), Java 21.

---

## 3. Frontend stack (decided — do not substitute)

| Tool | Purpose |
|---|---|
| React + TypeScript (Vite, `react-ts` template) | UI |
| Tailwind CSS v4 via `@tailwindcss/vite` | Styling; `src/index.css` contains only `@import "tailwindcss";` |
| React Router (`react-router-dom`) | Pages |
| TanStack Query (`@tanstack/react-query`) | Server state, caching, loading/error states |
| React Hook Form + Zod | Forms and validation mirroring backend rules (add in the auth stage) |

**Dev server:** port **5173** (the backend's OAuth redirect points to `http://localhost:5173`).

**API access via Vite proxy (no CORS):** `vite.config.ts` proxies `/api` → `http://localhost:8080`. All API calls use relative URLs (`/api/v1/...`). Do **not** proxy `/oauth2` or `/login/oauth2` — Google login must go directly to port 8080 (proxying it would change the redirect_uri and Google would reject it).

```ts
// vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 5173, proxy: { '/api': 'http://localhost:8080' } },
})
```

---

## 4. Authentication — how the frontend must handle it

The backend is an OAuth2 **resource server**: every protected request needs `Authorization: Bearer <JWT>`.

### Password login / register
- `POST /api/v1/auth/login` `{ "email", "password" }` → `200 AuthResponse`
- `POST /api/v1/auth/register` `{ "email", "password" }` (password ≥ 8 chars) → `201 AuthResponse`; new users always get role `ATHLETE`
- `AuthResponse = { token: string, email: string, roles: string[], expiresInMinutes: number }`

### Google login (OAuth2 authorization code flow)
- The "Sign in with Google" button is a **plain link / full-page navigation** to `http://localhost:8080/oauth2/authorization/google` (NOT fetch, NOT via proxy).
- On success the backend redirects the browser to: `http://localhost:5173/oauth2/callback#token=<JWT>` (token in the URL **fragment**).
- On failure: `http://localhost:5173/login?error=<code>` where code ∈ `email_not_verified`, `account_disabled`, `oauth_failed`.
- The `/oauth2/callback` page must: read `window.location.hash`, extract `token`, store it, **remove it from the address bar** (`history.replaceState`), then navigate to the dashboard.

### Token handling
- Store the JWT in `localStorage` (key `scoutpro_token`). This is a local dev app; mention the XSS trade-off in the explanation.
- JWT payload (decode the middle part, base64url): `{ iss: "scoutpro", sub: <email>, userId: <uuid>, roles: string[], iat, exp }` (exp in seconds).
- If the token is expired on load → treat as logged out.
- Any API response **401** → clear token, redirect to `/login`. **403** → show "You don't have permission" (do not log out).
- Role changes only apply after re-login (roles live inside the token).
- `GET /api/v1/users/me` → `{ email, userId, roles, expiresAt }` — good for validating a token.

---

## 5. Roles and what the UI shows (RBAC)

Roles: `ADMIN`, `SCOUT`, `CLUB_MANAGER`, `ATHLETE`. **The UI only hides actions for convenience; the backend enforces every rule.**

| Feature | ADMIN | SCOUT | CLUB_MANAGER | ATHLETE |
|---|---|---|---|---|
| Users & roles (list, assign roles, create scout profile) | ✓ | | | |
| Sports / criteria / teams — create, edit, delete | ✓ | | | |
| Sports / criteria / teams — read | ✓ | ✓ | ✓ | ✓ |
| Athletes — create, edit | ✓ | ✓ | | |
| Athletes — read | ✓ | ✓ | ✓ | ✗ (own-only not implemented yet) |
| Athletes — deactivate, delete | ✓ | | | |
| Physical profile — edit | ✓ | ✓ | | |
| Assessments — create | | ✓ | | |
| Assessments — edit/delete | ✓ | own only | | |
| Ranking / assessment history — read | ✓ | ✓ | ✓ | |
| Scouting reports — write | | own assessments | | |
| Scouting reports — read/search | ✓ | ✓ | ✓ | |
| Shortlists | ✓ (all) | | own only | |

ATHLETE users currently have almost no data access → their dashboard shows their email/roles and a "Your profile will appear here once linked by an admin" message. Do not change the backend for this.

---

## 6. API reference (all under `/api/v1`, JSON)

All entities from PostgreSQL include `id` (uuid string), `createdAt`, `updatedAt` (ISO date-times). Dates like `dateOfBirth` are `YYYY-MM-DD`.

**Error body** (400/403/404/409/422): `{ timestamp, status, error, message, path }` — show `message` to the user. **401 from a missing/expired token has an empty body** (reason is in the `WWW-Authenticate` header).

Status meanings used consistently: 400 invalid input · 401 not logged in · 403 wrong role / not owner · 404 not found · 409 duplicate / in use · 422 business rule (e.g. already assessed today).

### Users (ADMIN)
- `GET /users` → `User[]`
- `GET /users/{id}` → `User`
- `PUT /users/{id}/roles` `{ roles: ["SCOUT"] }` → `User` (admin cannot remove own ADMIN → 422)
- `User = { id, createdAt, updatedAt, email, provider: "LOCAL"|"GOOGLE", providerId, enabled, roles: [{ id, name, createdAt, updatedAt }] }` (password never returned)

### Sports
- `GET /sports` → `Sport[]` · `GET /sports/{id}`
- `POST /sports` `{ name, code }` → 201 (code stored uppercase, unique → 409)
- `PUT /sports/{id}` `{ name, code }` · `DELETE /sports/{id}` → 204 (409 if used)
- `Sport = { id, name, code, createdAt, updatedAt }`

### Criteria (weighted scoring rules per sport)
- `GET /sports/{sportId}/criteria` → `Criterion[]`
- `POST /sports/{sportId}/criteria` `{ name, weight }` (weight > 0) → 201
- `GET|PUT|DELETE /criteria/{id}` (PUT body `{ name, weight }`)
- `Criterion = { id, name, weight: number, sport: Sport, ... }`

### Teams
- `GET /sports/{sportId}/teams` · `POST /sports/{sportId}/teams` `{ name, city, level: "CLUB"|"ACADEMY" }`
- `GET|PUT|DELETE /teams/{id}`
- `Team = { id, name, city, level, sport: Sport, ... }`

### Athletes
- `GET /athletes` → `Athlete[]` · `GET /athletes/{id}`
- `POST /sports/{sportId}/athletes?teamId={optional}` `{ athleteCode, fullName, dateOfBirth, position, nationality, contactNumber }` → 201 (team must play same sport → 422)
- `PUT /athletes/{id}?teamId={optional}` same body (omitting teamId removes the team; sport never changes)
- `PATCH /athletes/{id}/deactivate` → Athlete · `DELETE /athletes/{id}` → 204 (409 if has assessments/shortlists: deactivate instead)
- `Athlete = { id, athleteCode, fullName, dateOfBirth, position, nationality, contactNumber, active, sport: Sport, team: Team|null, user: User|null, ... }`

### Physical profile (one per athlete, upsert)
- `GET /athletes/{athleteId}/physical-profile` (404 if none yet)
- `PUT /athletes/{athleteId}/physical-profile` `{ heightCm (50–250), weightKg (20–200), dominantSide: "LEFT"|"RIGHT"|"BOTH", measuredAt? }`

### Scouts
- `POST /users/{userId}/scout` (ADMIN; user must already have role SCOUT → else 422) `{ scoutCode, fullName, phoneNumber, organization }`
- `GET /scouts` (ADMIN) · `GET /scouts/{id}` (ADMIN) · `GET /scouts/me` (SCOUT; 404 if no profile yet)
- `PUT /scouts/{id}` `{ fullName, phoneNumber, organization }` (admin or own) · `PATCH /scouts/{id}/deactivate` · `DELETE /scouts/{id}`
- `Scout = { id, scoutCode, fullName, email, phoneNumber, organization, active, user: User, ... }`

### Assessments (core feature, US1)
- `POST /assessments` (SCOUT only):
  ```json
  { "athleteId": "uuid", "assessmentDate": "YYYY-MM-DD (optional, default today)", "remarks": "text",
    "scores": [ { "criterionId": "uuid", "score": 0-100 } ] }
  ```
  Must contain exactly one score per criterion of the athlete's sport. Errors: 400 "Missing scores for: X, Y", 400 score out of range, 422 same scout+athlete+date, 422 athlete inactive, 422 sport has no criteria, 404 "You don't have a scout profile yet".
- `GET /assessments/{id}` · `PUT /assessments/{id}` (same body; admin or own) · `DELETE /assessments/{id}`
- `GET /athletes/{athleteId}/assessments?page=0&size=20` → history, newest first
- `GET /assessments/ranking?page=0&size=20` → ordered by overallScore desc. **Note:** it ranks assessments, so one athlete can appear several times; the UI should keep only the best (first) entry per `athleteId`.
- `AssessmentResponse = { id, assessmentCode, athleteId, athleteName, scoutId, scoutName, assessmentDate, overallScore, remarks, scores: [{ criterionId, criterionName, weight, score }] }`
- Overall score = Σ(score × weight) ÷ Σ(weight), computed server-side. The form may show a **live preview** using the same formula, but always display the server's value after saving.

### Scouting reports (MongoDB)
- `POST|GET|PUT|DELETE /assessments/{assessmentId}/report` — body `{ summary, strengths: string[], weaknesses: string[], tags: string[], media: [{ url, type: "VIDEO"|"IMAGE"|"LINK", startSec, note }] }`; only the scout who made the assessment can create (403 otherwise); one per assessment (409)
- `GET /athletes/{athleteId}/reports` · `GET /reports/search?q=winger` (full-text over summary + tags)
- `ScoutingReport = { id, assessmentId, athleteId, scoutId, summary, strengths, weaknesses, tags, media, createdAt, updatedAt }`

### Shortlists (US2; ADMIN, CLUB_MANAGER)
- `GET /shortlists` (own; admin sees all) · `POST /shortlists` `{ name, notes }` (409 duplicate name)
- `GET|PUT|DELETE /shortlists/{id}`
- `POST /shortlists/{id}/athletes/{athleteId}` (409 already on list; 422 inactive athlete) · `DELETE /shortlists/{id}/athletes/{athleteId}`
- `ShortlistResponse = { id, name, notes, ownerEmail, athleteCount, athletes: [{ id, athleteCode, fullName, position, sportName }] }`

---

## 7. Pages and responsive layout (from the design doc)

Breakpoints to design and test: **mobile 360 px, tablet 768 px, desktop 1280 px**. Mobile-first.

| Page / route | Roles | Mobile (360 px) | Desktop (1280 px) |
|---|---|---|---|
| `/login` | all | single column, Google button first | split screen: brand panel + form |
| `/register` | all | single column | same as login |
| `/oauth2/callback` | all | handles the token, shows a spinner | — |
| `/` Dashboard | all (content per role) | stacked stat cards, bottom nav | sidebar nav, 4-column stat grid |
| `/athletes` | admin, scout, manager | cards + search, filters in a drawer | table with filters |
| `/athletes/:id` | admin, scout, manager | tabs: Info, Physical, History | two columns: profile + assessment history |
| `/assessments/new` | scout | **one criterion per row, large sliders, sticky Save button** | form left, live overall-score preview right |
| `/ranking` | admin, scout, manager | ranked cards, filter chips | table + "Add to shortlist" action (managers) |
| `/shortlists` | manager, admin | list, remove athlete | board: shortlists side by side |
| `/reports` | admin, scout, manager | search + stacked reports | report beside scores |
| `/admin/users`, `/admin/sports` | admin | lists with edit sheet | tables with modal editors |

Quality targets the UI must respect: a scout records a full assessment in **under 2 minutes on a 360 px phone**; Lighthouse accessibility **≥ 90** (labels on inputs, sufficient contrast, keyboard focus states, semantic HTML).

---

## 8. Suggested frontend structure

```
frontend/src/
├── main.tsx               QueryClientProvider + BrowserRouter + AuthProvider
├── App.tsx                routes
├── index.css              @import "tailwindcss";
├── api/
│   ├── client.ts          fetch wrapper: adds Bearer token, parses JSON, throws ApiError(status, message); 401 → logout
│   └── types.ts           TypeScript types matching section 6
├── auth/
│   ├── AuthContext.tsx    token state, login/logout, decoded user (email, userId, roles)
│   ├── jwt.ts             decode payload, isExpired
│   ├── RequireAuth.tsx    route guard (+ optional roles prop)
│   └── OAuthCallback.tsx
├── components/            Layout (sidebar/bottom nav), Button, Card, Modal, EmptyState, ErrorMessage, Spinner
├── features/              one folder per domain: athletes/, assessments/, ranking/, shortlists/, reports/, admin/
└── hooks/                 TanStack Query hooks (useAthletes, useCreateAssessment, ...)
```

---

## 9. Stages (do ONE at a time, stop after each)

1. **Setup** — Vite react-ts in `frontend/`, Tailwind v4, proxy, a test page showing `GET /api/v1/sports` → 401 and a Google link. Branch `feature/frontend-setup`.
2. **Auth** — API client, AuthContext, `/login`, `/register`, `/oauth2/callback`, route guard, logout, 401 handling. Test with `admin@scoutpro.rw` and with Google. Branch `feature/frontend-auth`.
3. **Layout + dashboard** — responsive shell (sidebar ≥ 768 px, bottom nav < 768 px), role-based menu, dashboard cards. Branch `feature/frontend-layout`.
4. **Athletes** — list (search/filter), detail (info, physical profile, history), create/edit (admin, scout). Branch `feature/frontend-athletes`.
5. **New assessment** — mobile-first scoring form with sliders + live preview, error messages for 400/422. Then optional scouting-report form. Branch `feature/frontend-assessments`.
6. **Ranking + shortlists** — ranking (dedupe per athlete), add to shortlist, shortlist board. Branch `feature/frontend-shortlists`.
7. **Admin** — users & roles, create scout profile, sports/criteria/teams management. Branch `feature/frontend-admin`.
8. **Polish** — loading/empty/error states everywhere, accessibility pass, Lighthouse check, test at 360/768/1280 px.

For every stage, finish with: files created/changed, how to test in the browser (which user, which clicks, what to expect), `npm run build` result, and the git commands to commit, push and open the PR to `develop`.

---

## 10. Backend facts that commonly trip people up

- Spring Boot 4 renamed things; don't "fix" the backend based on Boot 3 tutorials (e.g. `spring.web.error.include-message`, `spring-boot-starter-security-oauth2-*`, `spring.rabbitmq.listener.simple.retry.max-retries`, `spring.mongodb.uri`).
- Events (emails/SMS) are sent asynchronously via RabbitMQ after the DB commit; the UI does not wait for them. Emails can be viewed at http://localhost:8025.
- `ddl-auto=update`: the schema is generated by Hibernate from the entities (no Flyway).
- Test users created during development may exist (`scout1@test.rw` / `Password1` has role SCOUT and a scout profile). Create new ones via `/register` + admin role assignment if needed.
