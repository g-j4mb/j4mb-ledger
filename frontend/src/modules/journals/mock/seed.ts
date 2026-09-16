import type { JournalEntry, JournalLine, TransactionType } from '../types'

let lineId = 1
function lines(...pairs: [string, number, number, string][]): JournalLine[] {
  return pairs.map(([accountId, debitAmount, creditAmount, description]) => ({
    id: `l${lineId++}`,
    accountId,
    entryType: debitAmount > 0 ? 'DEBIT' : 'CREDIT',
    amount: debitAmount > 0 ? debitAmount : creditAmount,
    currencyCode: 'USD',
    description,
  }))
}

function entry(
  id: string, journalNumber: string, transactionType: TransactionType, description: string,
  status: JournalEntry['status'], total: number, entryLines: JournalLine[]
): JournalEntry {
  return {
    id, journalNumber, transactionType, status, reversed: status === 'CANCELLED',
    fiscalPeriodId: 'fy2026-p6', currencyCode: 'USD', description, reference: journalNumber,
    totalDebit: total, totalCredit: total, postedBy: status === 'POSTED' ? 'admin@j4mb.com' : undefined,
    lines: entryLines,
  }
}

export const journalSeed: JournalEntry[] = [
  entry('je-001', 'JE-2026-0087', 'PAYMENT', 'Payroll June W1', 'POSTED', 48500,
    lines(['a12', 48500, 0, 'June W1 salaries'], ['a01', 0, 48500, 'Payment'])),
  entry('je-002', 'JE-2026-0086', 'PURCHASE', 'Office Supplies Purchase', 'POSTED', 320,
    lines(['a15', 320, 0, 'Q2 supplies'], ['a01', 0, 320, 'Payment'])),
  entry('je-003', 'JE-2026-0085', 'PAYMENT', 'Rent Expense May', 'POSTED', 12000,
    lines(['a13', 12000, 0, 'May rent'], ['a01', 0, 12000, 'Payment'])),
  entry('je-004', 'JE-2026-0084', 'SALE', 'Client Invoice #1042', 'POSTED', 95000,
    lines(['a02', 95000, 0, 'Invoice 1042'], ['a10', 0, 95000, 'Revenue recognized'])),
  entry('je-005', 'JE-2026-0083', 'ADJUSTMENT', 'Utility Bills', 'DRAFT', 1870,
    lines(['a14', 1870, 0, 'May utilities'], ['a06', 0, 1870, 'Bill pending'])),
  entry('je-006', 'JE-2026-0082', 'PURCHASE', 'Inventory Purchase', 'POSTED', 35000,
    lines(['a03', 35000, 0, 'Q2 stock'], ['a06', 0, 35000, 'Supplier invoice'])),
  entry('je-007', 'JE-2026-0081', 'SALE', 'Service Revenue Q2', 'POSTED', 28000,
    lines(['a02', 28000, 0, 'Q2 services'], ['a11', 0, 28000, 'Revenue'])),
  entry('je-008', 'JE-2026-0080', 'PAYMENT', 'Payroll May', 'POSTED', 48500,
    lines(['a12', 48500, 0, 'May salaries'], ['a01', 0, 48500, 'Payment'])),
  entry('je-009', 'JE-2026-0079', 'ADJUSTMENT', 'Depreciation May', 'CANCELLED', 5000,
    lines(['a14', 5000, 0, 'PP&E depreciation'], ['a04', 0, 5000, 'Accumulated depreciation'])),
  entry('je-010', 'JE-2026-0078', 'RECEIPT', 'Bank Interest Income', 'POSTED', 1200,
    lines(['a01', 1200, 0, 'Interest credited'], ['a10', 0, 1200, 'Interest income'])),
]

let nextId = 11

export function createJournalMock(data: Omit<JournalEntry, 'id' | 'journalNumber'>): JournalEntry {
  const created: JournalEntry = {
    ...data,
    id: `je-${String(nextId).padStart(3, '0')}`,
    journalNumber: `JE-2026-${String(nextId + 87).padStart(4, '0')}`,
  }
  nextId++
  journalSeed.unshift(created)
  return created
}
