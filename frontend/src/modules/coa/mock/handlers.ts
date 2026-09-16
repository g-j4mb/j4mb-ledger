import { http, HttpResponse } from 'msw'
import { coaSeed } from './seed'

const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const coaHandlers = [
  http.get(`${base}/api/v1/coa`, () => HttpResponse.json(coaSeed)),
]
