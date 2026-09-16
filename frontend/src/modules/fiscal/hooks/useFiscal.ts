import { useQuery } from '@tanstack/react-query'
import { fiscalApi } from '../api/fiscalApi'

export function useFiscalYears() {
  return useQuery({ queryKey: ['fiscal', 'years'], queryFn: fiscalApi.getYears })
}

export function useFiscalPeriods(yearId?: string) {
  return useQuery({
    queryKey: ['fiscal', 'periods', yearId ?? 'all'],
    queryFn: () => fiscalApi.getPeriods(yearId),
  })
}
