export type JournalStatus = 'DRAFT' | 'POSTED' | 'CANCELLED'
export type TransactionType =
  | 'SALE' | 'PURCHASE' | 'PAYMENT' | 'RECEIPT' | 'ADJUSTMENT'
  | 'OPENING' | 'CLOSING' | 'REVERSAL' | 'TRANSFER'
export type EntryType = 'DEBIT' | 'CREDIT'

export interface JournalLine {
  id: string
  accountId: string
  entryType: EntryType
  amount: number
  currencyCode: string
  description?: string
}

export interface JournalEntry {
  id: string
  journalNumber: string
  transactionType: TransactionType
  status: JournalStatus
  reversed: boolean
  fiscalPeriodId: string
  currencyCode: string
  description?: string
  reference?: string
  totalDebit: number
  totalCredit: number
  postedBy?: string
  postedAtUtc?: string
  lines: JournalLine[]
}

export interface CreateJournalLineRequest {
  accountId: string
  entryType: EntryType
  amount: number
  currencyCode: string
  description?: string
}

export interface CreateJournalRequest {
  fiscalPeriodId: string
  transactionType: TransactionType
  currencyCode: string
  description?: string
  reference?: string
  lines: CreateJournalLineRequest[]
}
