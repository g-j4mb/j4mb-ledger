import { http, HttpResponse } from 'msw'
import { accountSeed } from './seed'

const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const accountHandlers = [
  http.get(`${base}/api/v1/accounts`, () => {
    const results = accountSeed
    return HttpResponse.json({ content: results, totalElements: results.length, totalPages: 1, number: 0, size: 50 })
  }),

  http.get(`${base}/api/v1/accounts/:id`, ({ params }) => {
    const account = accountSeed.find((a) => a.id === params.id)
    if (!account) return new HttpResponse(null, { status: 404 })
    return HttpResponse.json(account)
  }),
]
