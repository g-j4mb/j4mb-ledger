import type { CoaNode } from '../types'

const debit = 'DEBIT' as const
const credit = 'CREDIT' as const

export const coaSeed: CoaNode[] = [
  // Assets
  { id: 'c01', code: '1000', name: 'Assets', accountType: 'ASSET', normalBalance: debit, depth: 0, parentId: null, postable: false, frozen: false, fullPath: '1000' },
  { id: 'c02', code: '1100', name: 'Current Assets', accountType: 'ASSET', normalBalance: debit, depth: 1, parentId: 'c01', postable: false, frozen: false, fullPath: '1000/1100' },
  { id: 'c03', code: '1110', name: 'Cash and Cash Equivalents', accountType: 'ASSET', normalBalance: debit, depth: 2, parentId: 'c02', postable: true, frozen: false, fullPath: '1000/1100/1110' },
  { id: 'c04', code: '1120', name: 'Accounts Receivable', accountType: 'ASSET', normalBalance: debit, depth: 2, parentId: 'c02', postable: true, frozen: false, fullPath: '1000/1100/1120' },
  { id: 'c05', code: '1130', name: 'Inventory', accountType: 'ASSET', normalBalance: debit, depth: 2, parentId: 'c02', postable: true, frozen: false, fullPath: '1000/1100/1130' },
  { id: 'c06', code: '1200', name: 'Non-Current Assets', accountType: 'ASSET', normalBalance: debit, depth: 1, parentId: 'c01', postable: false, frozen: false, fullPath: '1000/1200' },
  { id: 'c07', code: '1210', name: 'Property, Plant & Equipment', accountType: 'ASSET', normalBalance: debit, depth: 2, parentId: 'c06', postable: true, frozen: false, fullPath: '1000/1200/1210' },
  { id: 'c08', code: '1220', name: 'Intangible Assets', accountType: 'ASSET', normalBalance: debit, depth: 2, parentId: 'c06', postable: true, frozen: false, fullPath: '1000/1200/1220' },
  // Liabilities
  { id: 'c09', code: '2000', name: 'Liabilities', accountType: 'LIABILITY', normalBalance: credit, depth: 0, parentId: null, postable: false, frozen: false, fullPath: '2000' },
  { id: 'c10', code: '2100', name: 'Current Liabilities', accountType: 'LIABILITY', normalBalance: credit, depth: 1, parentId: 'c09', postable: false, frozen: false, fullPath: '2000/2100' },
  { id: 'c11', code: '2110', name: 'Accounts Payable', accountType: 'LIABILITY', normalBalance: credit, depth: 2, parentId: 'c10', postable: true, frozen: false, fullPath: '2000/2100/2110' },
  { id: 'c12', code: '2120', name: 'Accrued Liabilities', accountType: 'LIABILITY', normalBalance: credit, depth: 2, parentId: 'c10', postable: true, frozen: false, fullPath: '2000/2100/2120' },
  // Equity
  { id: 'c13', code: '3000', name: 'Equity', accountType: 'EQUITY', normalBalance: credit, depth: 0, parentId: null, postable: false, frozen: false, fullPath: '3000' },
  { id: 'c14', code: '3100', name: 'Share Capital', accountType: 'EQUITY', normalBalance: credit, depth: 1, parentId: 'c13', postable: true, frozen: false, fullPath: '3000/3100' },
  { id: 'c15', code: '3200', name: 'Retained Earnings', accountType: 'EQUITY', normalBalance: credit, depth: 1, parentId: 'c13', postable: true, frozen: false, fullPath: '3000/3200' },
  // Revenue
  { id: 'c16', code: '4000', name: 'Revenue', accountType: 'REVENUE', normalBalance: credit, depth: 0, parentId: null, postable: false, frozen: false, fullPath: '4000' },
  { id: 'c17', code: '4100', name: 'Sales Revenue', accountType: 'REVENUE', normalBalance: credit, depth: 1, parentId: 'c16', postable: true, frozen: false, fullPath: '4000/4100' },
  { id: 'c18', code: '4200', name: 'Service Revenue', accountType: 'REVENUE', normalBalance: credit, depth: 1, parentId: 'c16', postable: true, frozen: false, fullPath: '4000/4200' },
  // Expenses
  { id: 'c19', code: '5000', name: 'Expenses', accountType: 'EXPENSE', normalBalance: debit, depth: 0, parentId: null, postable: false, frozen: false, fullPath: '5000' },
  { id: 'c20', code: '5100', name: 'Operating Expenses', accountType: 'EXPENSE', normalBalance: debit, depth: 1, parentId: 'c19', postable: false, frozen: false, fullPath: '5000/5100' },
  { id: 'c21', code: '5110', name: 'Salaries & Wages', accountType: 'EXPENSE', normalBalance: debit, depth: 2, parentId: 'c20', postable: true, frozen: false, fullPath: '5000/5100/5110' },
  { id: 'c22', code: '5120', name: 'Rent Expense', accountType: 'EXPENSE', normalBalance: debit, depth: 2, parentId: 'c20', postable: true, frozen: false, fullPath: '5000/5100/5120' },
  { id: 'c23', code: '5130', name: 'Utilities Expense', accountType: 'EXPENSE', normalBalance: debit, depth: 2, parentId: 'c20', postable: true, frozen: false, fullPath: '5000/5100/5130' },
]
