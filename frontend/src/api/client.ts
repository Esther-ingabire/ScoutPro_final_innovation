import type { AuthResponse, PageResult } from './types'
import { tokenStorage } from '../auth/tokenStorage'

/**
 * Every API call in the app goes through `api()`. Having one place means:
 *  - the Bearer token is attached automatically,
 *  - errors become one predictable type (ApiError) with a message to show,
 *  - an expired session (401) logs the user out everywhere.
 */

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

// AuthContext registers what to do on an expired session (clear state, go to /login).
// The client cannot import AuthContext directly: React hooks only work inside components.
let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

// AuthContext updates React state when a refresh replaces the access token.
let onTokenRefreshed: (token: string) => void = () => {}
export function setTokenRefreshedHandler(handler: (token: string) => void) {
  onTokenRefreshed = handler
}

let refreshing: Promise<string | null> | null = null

/** Ask the backend for a new access token using the stored refresh token. */
export function refreshSession(): Promise<string | null> {
  if (!refreshing) {
    refreshing = doRefresh().finally(() => {
      refreshing = null
    })
  }
  return refreshing
}

async function doRefresh(): Promise<string | null> {
  const refreshToken = tokenStorage.getRefresh()
  if (!refreshToken) return null
  try {
    const res = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) return null
    const data = (await res.json()) as AuthResponse
    tokenStorage.set(data.token)
    tokenStorage.setRefresh(data.refreshToken)
    onTokenRefreshed(data.token)
    return data.token
  } catch {
    return null
  }
}

/** Tell the backend this refresh token is no longer valid. Safe to call with no token. */
export function revokeRefreshToken() {
  const refreshToken = tokenStorage.getRefresh()
  if (!refreshToken) return
  void fetch('/api/v1/auth/logout', {
    method: 'POST',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  }).catch(() => {})
}

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
type Params = Record<string, string | number | undefined | null>

interface Options {
  method?: Method
  body?: unknown
  params?: Params
}

export async function api<T>(path: string, { method = 'GET', body, params }: Options = {}): Promise<T> {
  const url = '/api/v1' + path + toQueryString(params)
  const token = tokenStorage.get()

  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check your connection and that the backend is running.')
  }

  // 401 on a request that carried a token = the access JWT is missing, expired or invalid.
  // Try the refresh token once. A 401 on /auth/login is a wrong password: do not refresh.
  if (res.status === 401 && token && !path.startsWith('/auth/')) {
    const renewed = await refreshSession()
    if (renewed) {
      headers.Authorization = `Bearer ${renewed}`
      try {
        res = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
      } catch {
        throw new ApiError(0, 'Cannot reach the server. Check your connection and that the backend is running.')
      }
    }
    if (res.status === 401) {
      tokenStorage.clear()
      onUnauthorized()
      throw new ApiError(401, 'Your session has expired. Sign in again.')
    }
  }

  if (!res.ok) throw new ApiError(res.status, await readErrorMessage(res))

  if (res.status === 204) return undefined as T
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

function toQueryString(params?: Params): string {
  if (!params) return ''
  const qs = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') qs.set(key, String(value))
  }
  const s = qs.toString()
  return s ? '?' + s : ''
}

// Backend error body: { timestamp, status, error, message, path } -> we show `message`.
async function readErrorMessage(res: Response): Promise<string> {
  try {
    const data: unknown = await res.json()
    if (data && typeof data === 'object' && 'message' in data) {
      const message = (data as { message: unknown }).message
      // Spring's generic "Access Denied" is not helpful to a user; use our own wording.
      if (typeof message === 'string' && message && message !== 'Access Denied') return message
    }
  } catch {
    // body was empty or not JSON: fall through to the default text
  }
  return defaultMessage(res.status)
}

function defaultMessage(status: number): string {
  switch (status) {
    case 400: return 'Some of the information is invalid. Check the form and try again.'
    case 401: return 'Invalid email or password.'
    case 403: return "You don't have permission to do that."
    case 404: return 'Not found.'
    case 409: return 'This conflicts with existing data.'
    case 422: return 'This action is not allowed right now.'
    case 502:
    case 503:
    case 504: return 'The server is not responding. Check that the backend is running.'
    default: return `Something went wrong (status ${status}).`
  }
}

/** Turn any thrown value into text for the UI. */
export function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  return 'Something went wrong.'
}

/** Save a PDF the signed-in user is allowed to read. The token is sent the same way as api(). */
export async function downloadPdf(path: string, filename: string): Promise<void> {
  const blob = await pdfBlob(path)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

async function pdfBlob(path: string): Promise<Blob> {
  const headers: Record<string, string> = { Accept: 'application/pdf' }
  const token = tokenStorage.get()
  if (token) headers.Authorization = `Bearer ${token}`

  let res: Response
  try {
    res = await fetch('/api/v1' + path, { headers })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check your connection and that the backend is running.')
  }
  if (res.status === 401 && token && !path.startsWith('/auth/')) {
    const renewed = await refreshSession()
    if (renewed) {
      headers.Authorization = `Bearer ${renewed}`
      res = await fetch('/api/v1' + path, { headers })
    }
    if (res.status === 401) {
      tokenStorage.clear()
      onUnauthorized()
      throw new ApiError(401, 'Your session has expired. Sign in again.')
    }
  }
  if (!res.ok) throw new ApiError(res.status, await readErrorMessage(res))
  return res.blob()
}

/** List endpoints return a page. The screens use the rows in `content` (first 100). */
export async function listOf<T>(path: string, params?: Params): Promise<T[]> {
  const page = await api<PageResult<T> | T[]>(path, { params: { page: 0, size: 100, ...params } })
  return Array.isArray(page) ? page : page.content
}

/** For "optional" resources: 404 means "none yet", so return null instead of failing. */
export async function orNull<T>(promise: Promise<T>): Promise<T | null> {
  try {
    return await promise
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null
    throw e
  }
}
