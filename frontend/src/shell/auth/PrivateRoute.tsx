import { Navigate, useLocation } from 'react-router'
import { useAuth } from '@/shell/auth/AuthContext'

export function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <>{children}</>
}
