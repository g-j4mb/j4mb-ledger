import { Routes, Route, Navigate } from 'react-router'
import { FiscalYearListPage } from './pages/FiscalYearListPage'
import { FiscalPeriodListPage } from './pages/FiscalPeriodListPage'

export default function Fiscal() {
  return (
    <Routes>
      <Route index element={<Navigate to="years" replace />} />
      <Route path="years" element={<FiscalYearListPage />} />
      <Route path="periods" element={<FiscalPeriodListPage />} />
    </Routes>
  )
}
