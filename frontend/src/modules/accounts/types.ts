export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED'

// Matches AccountResponse. There's no account-level "type" (that lives on the
// linked COA node) and no balance field exposed by this endpoint yet.
export interface Account {
  id: string
  accountNumber: string
  name: string
  currencyCode: string
  status: AccountStatus
  coaNodeId: string
  overdraftLimit: number
}
