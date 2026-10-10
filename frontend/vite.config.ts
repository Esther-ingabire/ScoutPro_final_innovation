import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Must be 5173: the backend's OAuth2 success redirect points here.
    port: 5173,
    strictPort: true,
    // Forward /api/* to Spring Boot so the browser sees one origin (no CORS).
    // /oauth2 and /login/oauth2 are deliberately NOT proxied: Google login
    // must talk to port 8080 directly or the redirect_uri would not match.
    proxy: { '/api': 'http://localhost:8080' },
  },
})
