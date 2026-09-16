import type { ClosingRecord } from '../types'

export const closingSeed: ClosingRecord[] = [
  { id: 'cl-01', type: 'PERIOD', period: 'May 2026', closedAt: '2026-05-31T23:59:00Z', closedBy: 'Admin User', status: 'COMPLETED', notes: 'May 2026 period closed successfully.' },
  { id: 'cl-02', type: 'PERIOD', period: 'Apr 2026', closedAt: '2026-04-30T23:59:00Z', closedBy: 'Admin User', status: 'COMPLETED', notes: 'April 2026 period closed.' },
  { id: 'cl-03', type: 'YEAR', period: 'FY 2025', closedAt: '2025-12-31T23:59:00Z', closedBy: 'Admin User', status: 'COMPLETED', notes: 'FY 2025 year-end closing completed.' },
]
