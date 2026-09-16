import { useQuery } from '@tanstack/react-query'
import { currencyApi } from '../api/currencyApi'

export function useCurrencies() {
  return useQuery({ queryKey: ['currencies'], queryFn: currencyApi.list })
}

export function useExchangeRates() {
  return useQuery({ queryKey: ['exchange-rates'], queryFn: currencyApi.listRates })
}
