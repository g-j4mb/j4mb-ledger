import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { journalApi } from '../api/journalApi'
import type { CreateJournalRequest } from '../types'

export function useJournals(status?: string) {
  return useQuery({ queryKey: ['journals', status ?? 'all'], queryFn: () => journalApi.list(status) })
}

export function useJournal(id: string) {
  return useQuery({ queryKey: ['journals', id], queryFn: () => journalApi.get(id), enabled: !!id })
}

export function useCreateJournal() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateJournalRequest) => journalApi.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['journals'] }),
  })
}
