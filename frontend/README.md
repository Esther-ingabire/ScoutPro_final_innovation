# ScoutPro frontend

React 19 + TypeScript (Vite), Tailwind CSS v4, React Router, TanStack Query, React Hook Form + Zod.

```powershell
cd C:\Users\Administrator\Desktop\ScoutProFinal\frontend
npm install
npm run dev      # http://localhost:5173 (backend must run on 8080)
npm run build    # type-check + production build
npm run lint     # oxlint
```

`/api` is proxied to `http://localhost:8080` (see `vite.config.ts`). Google login links straight to port 8080.

## Structure

```
src/
  main.tsx              providers: QueryClient > BrowserRouter > AuthProvider
  App.tsx               routes + role guards
  api/client.ts         fetch wrapper (Bearer token, ApiError, 401 -> sign out)
  api/types.ts          TypeScript shapes of the backend JSON
  auth/                 token storage, JWT decode, AuthContext, RequireAuth, OAuth callback
  components/           Layout (sidebar / bottom nav), ui kit, Modal, ScoreDisplay, Icon
  hooks/                TanStack Query hooks per domain (sports, athletes, assessments, reports, shortlists, users)
  lib/format.ts         dates, weighted score formula, small helpers
  features/             one folder per page group: auth, dashboard, athletes, assessments,
                        ranking, shortlists, reports, admin
```

See `../CLAUDE.md` for the API contract and RBAC rules.
