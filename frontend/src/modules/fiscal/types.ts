export type FiscalStatus = 'OPEN' | 'CLOSED' | 'LOCKED'

export interface FiscalYear {
  id: string
  yearName: string
  startDate: string
  endDate: string
  status: FiscalStatus
}

export interface FiscalPeriod {
  id: string
  fiscalYearId: string
  periodName: string
  startDate: string
  endDate: string
  periodNumber: number
  status: FiscalStatus
}
