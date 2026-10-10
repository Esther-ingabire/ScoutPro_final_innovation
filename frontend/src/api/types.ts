// TypeScript shapes of the JSON the backend sends and accepts (CLAUDE.md section 6).
// Types only exist at compile time: they catch typos like athlete.fullname
// before the code runs, but they do not validate real responses.

export type Role = 'ADMIN' | 'SCOUT' | 'CLUB_MANAGER' | 'ATHLETE'
export const ALL_ROLES: Role[] = ['ADMIN', 'SCOUT', 'CLUB_MANAGER', 'ATHLETE']

/** Fields every PostgreSQL entity has (BaseEntity on the backend). */
export interface BaseEntity {
  id: string
  createdAt: string
  updatedAt: string
}

// ---------- auth & users ----------
export interface AuthResponse {
  token: string
  refreshToken: string
  email: string
  roles: Role[]
  expiresInMinutes: number
}

/** Register does not sign the user in. They enter the emailed code first. */
export interface RegisterResponse {
  email: string
  message: string
}

/** One page from a list endpoint. `content` is the rows; the rest is for paging. */
export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface Credentials {
  email: string
  password: string
}

export interface RoleEntity extends BaseEntity {
  name: Role
}

export interface User extends BaseEntity {
  email: string
  provider: 'LOCAL' | 'GOOGLE'
  providerId: string | null
  enabled: boolean
  roles: RoleEntity[]
}

// ---------- sports, criteria, teams ----------
export interface Sport extends BaseEntity {
  name: string
  code: string
}
export interface SportRequest {
  name: string
  code: string
}

export interface Criterion extends BaseEntity {
  name: string
  weight: number
  sport: Sport
}
export interface CriterionRequest {
  name: string
  weight: number
}

export type TeamLevel = 'CLUB' | 'ACADEMY'
export interface Team extends BaseEntity {
  name: string
  city: string
  level: TeamLevel
  sport: Sport
}
export interface TeamRequest {
  name: string
  city: string
  level: TeamLevel
}

// ---------- athletes ----------
export interface Athlete extends BaseEntity {
  athleteCode: string
  fullName: string
  dateOfBirth: string // YYYY-MM-DD
  position: string
  nationality: string
  contactNumber: string
  active: boolean
  sport: Sport
  team: Team | null
  user: User | null
}
export interface AthleteRequest {
  athleteCode: string
  fullName: string
  dateOfBirth: string
  position: string
  nationality: string
  contactNumber: string
}

export type DominantSide = 'LEFT' | 'RIGHT' | 'BOTH'
export interface PhysicalProfile extends BaseEntity {
  heightCm: number
  weightKg: number
  dominantSide: DominantSide
  measuredAt: string | null
}
export interface PhysicalProfileRequest {
  heightCm: number
  weightKg: number
  dominantSide: DominantSide
  measuredAt?: string
}

// ---------- scouts ----------
export interface Scout extends BaseEntity {
  scoutCode: string
  fullName: string
  email: string
  phoneNumber: string
  organization: string
  active: boolean
  user: User
}
export interface ScoutRequest {
  scoutCode: string
  fullName: string
  phoneNumber: string
  organization: string
}

// ---------- assessments ----------
export interface ScoreRequest {
  criterionId: string
  score: number
}
export interface AssessmentRequest {
  athleteId: string
  assessmentDate?: string
  remarks: string
  scores: ScoreRequest[]
}
export interface ScoreResponse {
  criterionId: string
  criterionName: string
  weight: number
  score: number
}
export interface AssessmentResponse {
  id: string
  assessmentCode: string
  athleteId: string
  athleteName: string
  scoutId: string
  scoutName: string
  assessmentDate: string
  overallScore: number
  remarks: string
  scores: ScoreResponse[]
}

// ---------- scouting reports (MongoDB) ----------
export type MediaType = 'VIDEO' | 'IMAGE' | 'LINK'
export interface MediaItem {
  url: string
  type: MediaType
  startSec: number | null
  note: string
}
export interface ScoutingReportRequest {
  summary: string
  strengths: string[]
  weaknesses: string[]
  tags: string[]
  media: MediaItem[]
}
export interface ScoutingReport extends ScoutingReportRequest {
  id: string
  assessmentId: string
  athleteId: string
  scoutId: string
  createdAt: string
  updatedAt: string
}

// ---------- shortlists ----------
export interface ShortlistAthlete {
  id: string
  athleteCode: string
  fullName: string
  position: string
  sportName: string
}
export interface Shortlist {
  id: string
  name: string
  notes: string
  ownerEmail: string
  athleteCount: number
  athletes: ShortlistAthlete[]
}
export interface ShortlistRequest {
  name: string
  notes: string
}
