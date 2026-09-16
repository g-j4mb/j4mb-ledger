import { Routes, Route } from 'react-router'
import { ClosingPage } from './pages/ClosingPage'

export default function Closing() {
  return (
    <Routes>
      <Route path="period" element={<ClosingPage />} />
      <Route path="year" element={<ClosingPage />} />
    </Routes>
  )
}
