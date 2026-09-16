import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router'
import { AppShell } from '@/shell/layout/AppShell'
import { PrivateRoute } from '@/shell/auth/PrivateRoute'
import { LoadingOverlay } from '@/shell/components/feedback/LoadingOverlay'
import { ErrorBoundary } from '@/shell/components/feedback/ErrorBoundary'
import { LoginPage } from '@/modules/auth/pages/LoginPage'

const Dashboard = lazy(() => import('@/modules/dashboard'))
const Coa = lazy(() => import('@/modules/coa'))
const Accounts = lazy(() => import('@/modules/accounts'))
const Fiscal = lazy(() => import('@/modules/fiscal'))
const Journals = lazy(() => import('@/modules/journals'))
const Currencies = lazy(() => import('@/modules/currencies'))
const PostingRules = lazy(() => import('@/modules/postingRules'))
const Closing = lazy(() => import('@/modules/closing'))

function Wrap({ children }: { children: React.ReactNode }) {
  return (
    <ErrorBoundary>
      <Suspense fallback={<LoadingOverlay />}>{children}</Suspense>
    </ErrorBoundary>
  )
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route
        element={
          <PrivateRoute>
            <AppShell />
          </PrivateRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Wrap><Dashboard /></Wrap>} />
        <Route path="ledger/coa" element={<Wrap><Coa /></Wrap>} />
        <Route path="ledger/accounts/*" element={<Wrap><Accounts /></Wrap>} />
        <Route path="ledger/fiscal/*" element={<Wrap><Fiscal /></Wrap>} />
        <Route path="ledger/journals/*" element={<Wrap><Journals /></Wrap>} />
        <Route path="ledger/currencies" element={<Wrap><Currencies /></Wrap>} />
        <Route path="ledger/exchange-rates" element={<Wrap><Currencies /></Wrap>} />
        <Route path="ledger/posting-rules" element={<Wrap><PostingRules /></Wrap>} />
        <Route path="ledger/closing/*" element={<Wrap><Closing /></Wrap>} />
        <Route path="admin/tenant" element={<Wrap><Dashboard /></Wrap>} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Route>
    </Routes>
  )
}
