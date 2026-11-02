import react from '@vitejs/plugin-react'
import { defineConfig, type ProxyOptions } from 'vite'

// The Spring Boot API serves routes at the root (/auth, /monitors, ...), so in
// dev we proxy those prefixes to :8080. This sidesteps CORS entirely — the
// browser only ever talks to the Vite origin.
const API_TARGET = process.env.VITE_API_TARGET ?? 'http://localhost:8080'

// The public status page resolves its tenant from the Host header (e.g.
// acme.roost.local). Browsers can't set Host, so for local demos point
// VITE_PUBLIC_HOST at a seeded tenant subdomain and the proxy forwards it.
const PUBLIC_HOST = process.env.VITE_PUBLIC_HOST

const apiPrefixes = ['/auth', '/monitors', '/workspaces', '/incidents', '/public']

const configurePublicHost: ProxyOptions['configure'] = (proxy) => {
  proxy.on('proxyReq', (proxyReq, req) => {
    if (PUBLIC_HOST && req.url?.startsWith('/public')) {
      proxyReq.setHeader('Host', PUBLIC_HOST)
    }
  })
}

const proxy: Record<string, ProxyOptions> = Object.fromEntries(
  apiPrefixes.map((p) => [
    p,
    {
      target: API_TARGET,
      changeOrigin: true,
      configure: PUBLIC_HOST ? configurePublicHost : undefined,
    } satisfies ProxyOptions,
  ]),
)

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: { proxy },
})
