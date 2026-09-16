import { http, HttpResponse } from 'msw'
import { journalSeed, createJournalMock } from './seed'
import type { CreateJournalRequest } from '../types'

const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const journalHandlers = [
  http.get(`${base}/api/v1/journals`, ({ request }) => {
    const url = new URL(request.url)
    const status = url.searchParams.get('status') ?? ''
    let results = journalSeed
    if (status) results = results.filter((j) => j.status === status)
    return HttpResponse.json({ content: results, totalElements: results.length, totalPages: 1, number: 0, size: 20 })
  }),

  http.get(`${base}/api/v1/journals/:id`, ({ params }) => {
    const entry = journalSeed.find((j) => j.id === params.id)
    if (!entry) return new HttpResponse(null, { status: 404 })
    return HttpResponse.json(entry)
  }),

  http.post(`${base}/api/v1/journals`, async ({ request }) => {
    const body = await request.json() as CreateJournalRequest
    const totalDebit = body.lines.filter((l) => l.entryType === 'DEBIT').reduce((s, l) => s + l.amount, 0)
    const totalCredit = body.lines.filter((l) => l.entryType === 'CREDIT').reduce((s, l) => s + l.amount, 0)
    const entry = createJournalMock({
      transactionType: body.transactionType,
      fiscalPeriodId: body.fiscalPeriodId,
      currencyCode: body.currencyCode,
      description: body.description,
      reference: body.reference,
      status: 'DRAFT',
      reversed: false,
      totalDebit,
      totalCredit,
      lines: body.lines.map((l, i) => ({ ...l, id: `new-l-${i}` })),
    })
    return HttpResponse.json(entry, { status: 201 })
  }),
]
