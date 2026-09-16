import { apiClient } from '@/shell/api/client'
import type { DashboardSummary, RecentJournal } from '../types'

export const dashboardApi = {
  getSummary: () => apiClient.get<DashboardSummary>('/api/v1/dashboard/summary').then((r) => r.data),
  getRecentJournals: () => apiClient.get<RecentJournal[]>('/api/v1/dashboard/recent-journals').then((r) => r.data),
}
