import { http, HttpResponse } from 'msw'
import { closingSeed } from './seed'

const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const closingHandlers = [
  http.get(`${base}/api/v1/closing`, () =>
    HttpResponse.json({ content: closingSeed, totalElements: closingSeed.length, totalPages: 1, number: 0, size: 50 })
  ),
]
