import { http, HttpResponse } from 'msw'
import { currencySeed, exchangeRateSeed } from './seed'

const base = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const currencyHandlers = [
  http.get(`${base}/api/v1/currencies`, () => HttpResponse.json(currencySeed)),
  http.get(`${base}/api/v1/exchange-rates`, () => HttpResponse.json(exchangeRateSeed)),
]
