import { apiClient } from '@/shell/api/client'
import type { Account } from '../types'
import type { Page } from '@/shell/api/types'

// GET /api/v1/accounts only accepts pagination (no search/type/status query
// params on the backend yet) — filtering below happens client-side instead.
export const accountApi = {
  list: () => apiClient.get<Page<Account>>('/api/v1/accounts').then((r) => r.data),
  get: (id: string) => apiClient.get<Account>(`/api/v1/accounts/${id}`).then((r) => r.data),
}
