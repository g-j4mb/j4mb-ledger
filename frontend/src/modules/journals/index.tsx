import { Routes, Route } from 'react-router'
import { JournalListPage } from './pages/JournalListPage'
import { JournalDetailPage } from './pages/JournalDetailPage'
import { JournalFormPage } from './pages/JournalFormPage'

export default function Journals() {
  return (
    <Routes>
      <Route index element={<JournalListPage />} />
      <Route path="new" element={<JournalFormPage />} />
      <Route path=":id" element={<JournalDetailPage />} />
    </Routes>
  )
}
