import { apiClient } from '@/shell/api/client'
import type { ClosingRecord } from '../types'
import type { Page } from '@/shell/api/types'

export const closingApi = {
  list: () => apiClient.get<Page<ClosingRecord>>('/api/v1/closing').then((r) => r.data),
}
