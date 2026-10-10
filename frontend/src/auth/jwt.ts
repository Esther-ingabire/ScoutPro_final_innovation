import type { Role } from '../api/types'

/**
 * A JWT is three base64url parts: header.payload.signature.
 * We only DECODE the payload to read who the user is. We do not (and cannot)
 * verify the signature here: that needs the secret key, which only the backend has.
 * So the frontend trusts the token for display only; the backend checks it on every request.
 */
export interface JwtPayload {
  iss: string
  sub: string // email
  userId: string
  roles: Role[]
  iat: number // issued at, seconds since 1970
  exp: number // expires at, seconds since 1970
}

export function decodeJwt(token: string): JwtPayload | null {
  try {
    const part = token.split('.')[1]
    if (!part) return null
    // base64url -> base64: swap URL-safe characters back and restore the "=" padding
    const base64 = part.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0))
    const payload = JSON.parse(new TextDecoder().decode(bytes))
    if (typeof payload.exp !== 'number' || typeof payload.sub !== 'string') return null
    return { ...payload, roles: Array.isArray(payload.roles) ? payload.roles : [] }
  } catch {
    return null
  }
}

export function isExpired(payload: JwtPayload): boolean {
  return payload.exp * 1000 <= Date.now() // exp is in seconds, Date.now() in milliseconds
}
