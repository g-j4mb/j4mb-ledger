import { http, HttpResponse } from 'msw'
import { postingRuleSeed } from './seed'

const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const postingRuleHandlers = [
  http.get(`${base}/api/v1/posting-rules`, () =>
    HttpResponse.json({ content: postingRuleSeed, totalElements: postingRuleSeed.length, totalPages: 1, number: 0, size: 50 })
  ),
]
