export type ClosingType = 'PERIOD' | 'YEAR'
export type ClosingStatus = 'COMPLETED' | 'PENDING' | 'FAILED'

export interface ClosingRecord {
  id: string
  type: ClosingType
  period: string
  closedAt: string
  closedBy: string
  status: ClosingStatus
  notes: string
}
