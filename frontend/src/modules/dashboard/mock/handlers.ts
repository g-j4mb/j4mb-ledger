import { http, HttpResponse } from 'msw'
import { dashboardSummary, recentJournals } from './seed'

const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const dashboardHandlers = [
  http.get(`${base}/api/v1/dashboard/summary`, () => HttpResponse.json(dashboardSummary)),
  http.get(`${base}/api/v1/dashboard/recent-journals`, () => HttpResponse.json(recentJournals)),
]
