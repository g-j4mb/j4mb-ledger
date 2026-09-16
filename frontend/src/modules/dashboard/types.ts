export interface DashboardSummary {
  totalAccounts: number
  openFiscalPeriod: string
  journalEntriesLast30d: number
  lastClosingDate: string
}

export interface RecentJournal {
  id: string
  referenceNo: string
  description: string
  entryDate: string
  status: 'DRAFT' | 'POSTED' | 'REVERSED'
  totalDebit: number
  currency: string
}
