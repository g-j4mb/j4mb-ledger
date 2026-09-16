import { dashboardHandlers } from '@/modules/dashboard/mock/handlers'
import { coaHandlers } from '@/modules/coa/mock/handlers'
import { accountHandlers } from '@/modules/accounts/mock/handlers'
import { fiscalHandlers } from '@/modules/fiscal/mock/handlers'
import { journalHandlers } from '@/modules/journals/mock/handlers'
import { currencyHandlers } from '@/modules/currencies/mock/handlers'
import { postingRuleHandlers } from '@/modules/postingRules/mock/handlers'
import { closingHandlers } from '@/modules/closing/mock/handlers'

export const handlers = [
  ...dashboardHandlers,
  ...coaHandlers,
  ...accountHandlers,
  ...fiscalHandlers,
  ...journalHandlers,
  ...currencyHandlers,
  ...postingRuleHandlers,
  ...closingHandlers,
]
