import { apiClient } from '@/shell/api/client'
import type { FiscalYear, FiscalPeriod } from '../types'

export const fiscalApi = {
  getYears: () => apiClient.get<FiscalYear[]>('/api/v1/fiscal/years').then((r) => r.data),
  getPeriods: (yearId?: string) => {
    const url = yearId ? `/api/v1/fiscal/years/${yearId}/periods` : '/api/v1/fiscal/periods'
    return apiClient.get<FiscalPeriod[]>(url).then((r) => r.data)
  },
}
