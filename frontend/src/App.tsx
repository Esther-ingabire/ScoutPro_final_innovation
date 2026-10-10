import { lazy, Suspense } from 'react'
import { Link, Route, Routes } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth'
import OAuthCallback from './auth/OAuthCallback'
import Layout from './components/Layout'
import { buttonClass, Spinner } from './components/ui'
import LoginPage from './features/auth/LoginPage'
import RegisterPage from './features/auth/RegisterPage'
import VerifyEmailPage from './features/auth/VerifyEmailPage'
import DashboardPage from './features/dashboard/DashboardPage'

// Pages behind the login are loaded on demand (code splitting): the login page
// downloads less JavaScript, which helps on slow mobile connections.
const AthletesPage = lazy(() => import('./features/athletes/AthletesPage'))
const AthleteDetailPage = lazy(() => import('./features/athletes/AthleteDetailPage'))
const AssessmentFormPage = lazy(() => import('./features/assessments/AssessmentFormPage'))
const AssessmentDetailPage = lazy(() => import('./features/assessments/AssessmentDetailPage'))
const RankingPage = lazy(() => import('./features/ranking/RankingPage'))
const ShortlistsPage = lazy(() => import('./features/shortlists/ShortlistsPage'))
const ReportsPage = lazy(() => import('./features/reports/ReportsPage'))
const UsersPage = lazy(() => import('./features/admin/UsersPage'))
const SportsPage = lazy(() => import('./features/admin/SportsPage'))

const STAFF = ['ADMIN', 'SCOUT', 'CLUB_MANAGER'] as const

export default function App() {
  return (
    <Suspense fallback={<Spinner />}>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/oauth2/callback" element={<OAuthCallback />} />

        {/* Signed in: the shell has no role check. Each page below names the roles that may open it. */}
        <Route element={<RequireAuth><Layout /></RequireAuth>}>
          <Route index element={<DashboardPage />} />
          {/* Any signed-in user can open an athlete page. The backend rejects an athlete opening someone else. */}
          <Route path="athletes/:id" element={<AthleteDetailPage />} />

          <Route path="athletes" element={<RequireAuth roles={[...STAFF]}><AthletesPage /></RequireAuth>} />
          <Route path="ranking" element={<RequireAuth roles={[...STAFF]}><RankingPage /></RequireAuth>} />
          <Route path="reports" element={<RequireAuth roles={[...STAFF]}><ReportsPage /></RequireAuth>} />
          {/* Static paths before :id, so "new" is not treated as an assessment id. */}
          <Route path="assessments/new" element={<RequireAuth roles={['SCOUT']}><AssessmentFormPage /></RequireAuth>} />
          <Route path="assessments/:id/edit" element={<RequireAuth roles={['ADMIN', 'SCOUT']}><AssessmentFormPage /></RequireAuth>} />
          <Route path="assessments/:id" element={<RequireAuth roles={[...STAFF]}><AssessmentDetailPage /></RequireAuth>} />

          <Route path="shortlists" element={<RequireAuth roles={['ADMIN', 'CLUB_MANAGER']}><ShortlistsPage /></RequireAuth>} />
          <Route path="admin/users" element={<RequireAuth roles={['ADMIN']}><UsersPage /></RequireAuth>} />
          <Route path="admin/sports" element={<RequireAuth roles={['ADMIN']}><SportsPage /></RequireAuth>} />

          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </Suspense>
  )
}

function NotFound() {
  return (
    <div className="py-16 text-center">
      <h1 className="font-['Barlow_Condensed',sans-serif] text-4xl font-bold">Page not found</h1>
      <p className="mt-2 text-zinc-600">This address does not match any page in ScoutPro.</p>
      <Link to="/" className={`${buttonClass('secondary')} mt-6`}>Back to dashboard</Link>
    </div>
  )
}
