import axios from 'axios'

const TOKEN_KEY = 'roost.token'

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch {
    /* private mode / blocked storage — auth just won't persist across reloads */
  }
}

// All API calls go under /api so they never collide with client-side routes
// (e.g. the /incidents page vs. the /incidents endpoint). The Vite dev proxy
// strips /api and forwards to the Spring Boot API; in production the same-origin
// reverse proxy (ALB/nginx) routes /api to the backend.
export const api = axios.create({ baseURL: '/api' })

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Cold-start resilience: a free-tier backend spins down when idle, and the
// first request(s) after it wakes can be dropped (connection closed / timeout)
// with no HTTP response at all. Retry those a few times with a short backoff so
// a wake-up reads as a brief load instead of an error. Anything that gets a real
// HTTP response (including 4xx/5xx) falls through to the handling below and is
// never retried here.
const MAX_COLD_RETRIES = 3

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const config = error?.config as (typeof error.config & { __coldRetry?: number }) | undefined
    if (config && !error?.response) {
      config.__coldRetry = (config.__coldRetry ?? 0) + 1
      if (config.__coldRetry <= MAX_COLD_RETRIES) {
        await new Promise((resolve) => setTimeout(resolve, config.__coldRetry * 1500))
        return api.request(config)
      }
    }
    // On 401 the token is gone/expired — drop it and bounce to login.
    if (error?.response?.status === 401 && getToken()) {
      setToken(null)
      if (!location.pathname.startsWith('/login')) {
        location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

// Pull a human-readable message out of an RFC 7807 ProblemDetail or axios error.
export function errorMessage(err: unknown): string {
  const e = err as { response?: { data?: { detail?: string; message?: string } }; message?: string }
  return e?.response?.data?.detail ?? e?.response?.data?.message ?? e?.message ?? 'Something went wrong'
}
