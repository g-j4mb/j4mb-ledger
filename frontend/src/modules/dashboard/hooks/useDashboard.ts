import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../api/dashboardApi'

export function useDashboardSummary() {
  return useQuery({ queryKey: ['dashboard', 'summary'], queryFn: dashboardApi.getSummary })
}

export function useRecentJournals() {
  return useQuery({ queryKey: ['dashboard', 'recent-journals'], queryFn: dashboardApi.getRecentJournals })
}
