import { apiClient } from '@/shell/api/client'
import type { JournalEntry, CreateJournalRequest } from '../types'
import type { Page } from '@/shell/api/types'

export const journalApi = {
  list: (status?: string) => {
    const params = status ? `?status=${status}` : ''
    return apiClient.get<Page<JournalEntry>>(`/api/v1/journals${params}`).then((r) => r.data)
  },
  get: (id: string) => apiClient.get<JournalEntry>(`/api/v1/journals/${id}`).then((r) => r.data),
  create: (data: CreateJournalRequest) => apiClient.post<JournalEntry>('/api/v1/journals', data).then((r) => r.data),
}
