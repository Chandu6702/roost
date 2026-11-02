import react from '@vitejs/plugin-react'
import { defineConfig, type ProxyOptions } from 'vite'

// The SPA calls the API under /api (see src/api/client.ts) so API paths never
// collide with client-side routes like /incidents. In dev we strip /api and
// forward to the Spring Boot backend — this also sidesteps CORS, since the
// browser only ever talks to the Vite origin.
const API_TARGET = process.env.VITE_API_TARGET ?? 'http://localhost:8080'

// The public status page resolves its tenant from the Host header (e.g.
// acme.roost.local). Browsers can't set Host, so for local demos point
// VITE_PUBLIC_HOST at a seeded tenant subdomain and the proxy forwards it.
const PUBLIC_HOST = process.env.VITE_PUBLIC_HOST

const configurePublicHost: ProxyOptions['configure'] = (proxy) => {
  proxy.on('proxyReq', (proxyReq, req) => {
    // req.url is already rewritten (/api stripped) by the time this runs.
    if (PUBLIC_HOST && req.url?.startsWith('/public')) {
      proxyReq.setHeader('Host', PUBLIC_HOST)
    }
  })
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: API_TARGET,
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api/, ''),
        configure: PUBLIC_HOST ? configurePublicHost : undefined,
      } satisfies ProxyOptions,
    },
  },
})
