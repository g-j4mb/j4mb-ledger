import { useQuery } from '@tanstack/react-query'
import { accountApi } from '../api/accountApi'

export function useAccounts() {
  return useQuery({ queryKey: ['accounts'], queryFn: accountApi.list })
}

export function useAccount(id: string) {
  return useQuery({ queryKey: ['accounts', id], queryFn: () => accountApi.get(id), enabled: !!id })
}
