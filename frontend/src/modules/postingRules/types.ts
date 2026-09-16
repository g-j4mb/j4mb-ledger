export interface PostingRule {
  id: string
  name: string
  description: string
  triggerEvent: string
  debitAccount: string
  creditAccount: string
  isActive: boolean
}
