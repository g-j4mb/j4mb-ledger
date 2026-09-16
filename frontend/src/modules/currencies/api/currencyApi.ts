import { apiClient } from '@/shell/api/client'
import type { Currency, ExchangeRate } from '../types'

// Tenant currency/rate lists are small reference data — the backend returns
// them as plain arrays, not paginated.
export const currencyApi = {
  list: () => apiClient.get<Currency[]>('/api/v1/currencies').then((r) => r.data),
  listRates: () => apiClient.get<ExchangeRate[]>('/api/v1/exchange-rates').then((r) => r.data),
}
