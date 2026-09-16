import { Routes, Route } from 'react-router'
import { AccountListPage } from './pages/AccountListPage'
import { AccountDetailPage } from './pages/AccountDetailPage'

export default function Accounts() {
  return (
    <Routes>
      <Route index element={<AccountListPage />} />
      <Route path=":id" element={<AccountDetailPage />} />
    </Routes>
  )
}
