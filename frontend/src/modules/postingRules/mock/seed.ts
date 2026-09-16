import type { PostingRule } from '../types'

export const postingRuleSeed: PostingRule[] = [
  { id: 'pr-01', name: 'Sales Invoice', description: 'Automatic posting for sales invoices', triggerEvent: 'SALES_INVOICE', debitAccount: 'Accounts Receivable (1120)', creditAccount: 'Sales Revenue (4100)', isActive: true },
  { id: 'pr-02', name: 'Payroll Processing', description: 'Monthly payroll auto-posting', triggerEvent: 'PAYROLL_RUN', debitAccount: 'Salaries & Wages (5110)', creditAccount: 'Cash (1110)', isActive: true },
  { id: 'pr-03', name: 'Supplier Payment', description: 'AP settlement posting', triggerEvent: 'SUPPLIER_PAYMENT', debitAccount: 'Accounts Payable (2110)', creditAccount: 'Cash (1110)', isActive: true },
  { id: 'pr-04', name: 'Depreciation', description: 'Monthly depreciation charge', triggerEvent: 'PERIOD_DEPRECIATION', debitAccount: 'Depreciation Expense', creditAccount: 'PP&E (1210)', isActive: false },
  { id: 'pr-05', name: 'Interest Income', description: 'Bank interest credit', triggerEvent: 'BANK_INTEREST', debitAccount: 'Cash (1110)', creditAccount: 'Sales Revenue (4100)', isActive: true },
]
