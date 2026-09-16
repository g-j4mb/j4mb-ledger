import type { FiscalYear, FiscalPeriod } from '../types'

export const fiscalYearSeed: FiscalYear[] = [
  { id: 'fy2026', yearName: 'FY 2026', startDate: '2026-01-01', endDate: '2026-12-31', status: 'OPEN' },
  { id: 'fy2025', yearName: 'FY 2025', startDate: '2025-01-01', endDate: '2025-12-31', status: 'CLOSED' },
]

const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
const daysInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]

function makePeriods(yearId: string, year: number, yearStatus: 'OPEN' | 'CLOSED'): FiscalPeriod[] {
  return months.map((m, i) => ({
    id: `${yearId}-p${i + 1}`,
    fiscalYearId: yearId,
    periodName: `${m} ${year}`,
    startDate: `${year}-${String(i + 1).padStart(2, '0')}-01`,
    endDate: `${year}-${String(i + 1).padStart(2, '0')}-${daysInMonth[i]}`,
    periodNumber: i + 1,
    status: yearStatus === 'CLOSED' ? 'CLOSED' : i < 5 ? 'CLOSED' : i === 5 ? 'OPEN' : 'LOCKED' as const,
  }))
}

export const fiscalPeriodSeed: FiscalPeriod[] = [
  ...makePeriods('fy2026', 2026, 'OPEN'),
  ...makePeriods('fy2025', 2025, 'CLOSED'),
]
