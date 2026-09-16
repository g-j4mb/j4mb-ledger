import { http, HttpResponse } from 'msw'
import { fiscalYearSeed, fiscalPeriodSeed } from './seed'

const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const fiscalHandlers = [
  http.get(`${base}/api/v1/fiscal/years`, () => HttpResponse.json(fiscalYearSeed)),

  http.get(`${base}/api/v1/fiscal/years/:yearId/periods`, ({ params }) => {
    const periods = fiscalPeriodSeed.filter((p) => p.fiscalYearId === params.yearId)
    return HttpResponse.json(periods)
  }),

  http.get(`${base}/api/v1/fiscal/periods`, () => HttpResponse.json(fiscalPeriodSeed)),
]
