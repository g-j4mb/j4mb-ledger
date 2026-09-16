import type { Account } from '../types'

export const accountSeed: Account[] = [
  { id: 'a01', accountNumber: '1110', name: 'Cash and Cash Equivalents', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c03', overdraftLimit: 0 },
  { id: 'a02', accountNumber: '1120', name: 'Accounts Receivable', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c04', overdraftLimit: 0 },
  { id: 'a03', accountNumber: '1130', name: 'Inventory', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c05', overdraftLimit: 0 },
  { id: 'a04', accountNumber: '1210', name: 'Property, Plant & Equipment', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c07', overdraftLimit: 0 },
  { id: 'a05', accountNumber: '1220', name: 'Intangible Assets', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c08', overdraftLimit: 0 },
  { id: 'a06', accountNumber: '2110', name: 'Accounts Payable', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c11', overdraftLimit: 0 },
  { id: 'a07', accountNumber: '2120', name: 'Accrued Liabilities', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c12', overdraftLimit: 0 },
  { id: 'a08', accountNumber: '3100', name: 'Share Capital', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c14', overdraftLimit: 0 },
  { id: 'a09', accountNumber: '3200', name: 'Retained Earnings', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c15', overdraftLimit: 0 },
  { id: 'a10', accountNumber: '4100', name: 'Sales Revenue', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c17', overdraftLimit: 0 },
  { id: 'a11', accountNumber: '4200', name: 'Service Revenue', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c18', overdraftLimit: 0 },
  { id: 'a12', accountNumber: '5110', name: 'Salaries & Wages', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c21', overdraftLimit: 0 },
  { id: 'a13', accountNumber: '5120', name: 'Rent Expense', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c22', overdraftLimit: 0 },
  { id: 'a14', accountNumber: '5130', name: 'Utilities Expense', currencyCode: 'USD', status: 'ACTIVE', coaNodeId: 'c23', overdraftLimit: 0 },
  { id: 'a15', accountNumber: '5140', name: 'Office Supplies', currencyCode: 'USD', status: 'FROZEN', coaNodeId: 'c20', overdraftLimit: 0 },
]
