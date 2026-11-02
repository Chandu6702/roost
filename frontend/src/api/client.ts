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

// baseURL '' → requests are same-origin and the Vite dev proxy forwards them
// to the Spring Boot API. In production the SPA is served next to the API.
export const api = axios.create({ baseURL: '' })

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// On 401 the token is gone/expired — drop it and bounce to login.
api.interceptors.response.use(
  (res) => res,
  (error) => {
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
