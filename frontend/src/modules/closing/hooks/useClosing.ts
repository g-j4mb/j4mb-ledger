import { useQuery } from '@tanstack/react-query'
import { closingApi } from '../api/closingApi'

export function useClosingRecords() {
  return useQuery({ queryKey: ['closing'], queryFn: closingApi.list })
}
