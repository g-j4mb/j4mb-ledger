import { createContext, useContext, useState, useRef, useEffect, type ReactNode } from 'react'
import { setTokenGetter, setTenantGetter } from '@/shell/api/client'

export interface AuthUser {
  id: string
  name: string
  email: string
  roles: string[]
  tenantCode: string
}

interface AuthContextValue {
  user: AuthUser | null
  accessToken: string | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [accessToken, setAccessToken] = useState<string | null>(null)

  // The api client holds these getters outside React so every request reads the
  // latest values without re-creating the axios interceptor on each render.
  const tokenRef = useRef<string | null>(null)
  const tenantRef = useRef<string | null>(null)
  tokenRef.current = accessToken
  tenantRef.current = user?.tenantCode ?? null

  useEffect(() => {
    setTokenGetter(() => tokenRef.current)
    setTenantGetter(() => tenantRef.current)
  }, [])

  const login = async (email: string, password: string) => {
    // Fake auth — replace with Keycloak/OAuth2 when backend is ready
    if (email === 'admin@j4mb.com' && password === 'demo123') {
      const fakeUser: AuthUser = {
        id: '1',
        name: 'Admin User',
        email,
        roles: ['ADMIN', 'LEDGER_MANAGER'],
        tenantCode: 'acme',
      }
      setUser(fakeUser)
      setAccessToken('fake-jwt-token-in-memory-only')
    } else {
      throw new Error('Invalid email or password')
    }
  }

  const logout = () => {
    setUser(null)
    setAccessToken(null)
  }

  return (
    <AuthContext.Provider value={{ user, accessToken, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
