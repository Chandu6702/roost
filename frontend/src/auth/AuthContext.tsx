import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { api, getToken, setToken } from '../api/client'
import type { LoginResponse } from '../api/types'

interface AuthState {
  token: string | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(getToken())

  const value = useMemo<AuthState>(
    () => ({
      token,
      isAuthenticated: !!token,
      async login(email, password) {
        const { data } = await api.post<LoginResponse>('/auth/login', { email, password })
        setToken(data.token)
        setTokenState(data.token)
      },
      async register(email, password) {
        await api.post('/auth/register', { email, password })
      },
      logout() {
        setToken(null)
        setTokenState(null)
      },
    }),
    [token],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
