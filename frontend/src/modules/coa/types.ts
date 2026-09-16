export type AccountType = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE'
export type NormalBalance = 'DEBIT' | 'CREDIT'

// Matches CoaNodeResponse. There's no per-node "currency" or "status" — those
// live on the operational Account linked to a postable leaf node, not the
// COA hierarchy itself.
export interface CoaNode {
  id: string
  parentId: string | null
  code: string
  name: string
  fullPath: string
  depth: number
  accountType: AccountType
  normalBalance: NormalBalance
  postable: boolean
  frozen: boolean
}
