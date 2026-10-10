// The access JWT and the refresh token live in localStorage so a page refresh
// keeps you signed in. Trade-off: any JavaScript on the page can read them, so
// an XSS bug could steal them. An httpOnly cookie avoids that but needs CSRF
// protection. Acceptable for this local dev app.
// The refresh token is what lasts 7 days. The access JWT lasts about 15 minutes.

const ACCESS = 'scoutpro_token'
const REFRESH = 'scoutpro_refresh'

export const tokenStorage = {
  get(): string | null {
    try {
      return localStorage.getItem(ACCESS)
    } catch {
      return null
    }
  },
  set(token: string) {
    localStorage.setItem(ACCESS, token)
  },
  getRefresh(): string | null {
    try {
      return localStorage.getItem(REFRESH)
    } catch {
      return null
    }
  },
  setRefresh(token: string) {
    localStorage.setItem(REFRESH, token)
  },
  clear() {
    try {
      localStorage.removeItem(ACCESS)
      localStorage.removeItem(REFRESH)
    } catch {
      // storage unavailable: nothing to clear
    }
  },
}
