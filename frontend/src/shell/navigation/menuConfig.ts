import {
  LayoutDashboard,
  GitBranch,
  BookOpen,
  Calendar,
  CalendarDays,
  FileText,
  Settings2,
  Archive,
  ArchiveRestore,
  Coins,
  ArrowLeftRight,
  Shield,
  type LucideIcon,
} from 'lucide-react'

export interface MenuItem {
  id: string
  labelKey: string
  icon: LucideIcon
  path: string
  roles?: string[]
}

export interface MenuGroup {
  group: string
  groupKey: string
  items: MenuItem[]
}

export const menuConfig: MenuGroup[] = [
  {
    group: 'Overview',
    groupKey: 'overview',
    items: [
      { id: 'dashboard', labelKey: 'dashboard', icon: LayoutDashboard, path: '/dashboard' },
    ],
  },
  {
    group: 'Accounts',
    groupKey: 'accounts',
    items: [
      { id: 'coa', labelKey: 'chartOfAccounts', icon: GitBranch, path: '/ledger/coa' },
      { id: 'accounts', labelKey: 'accounts', icon: BookOpen, path: '/ledger/accounts' },
    ],
  },
  {
    group: 'Fiscal',
    groupKey: 'fiscal',
    items: [
      { id: 'fiscal-years', labelKey: 'fiscalYears', icon: Calendar, path: '/ledger/fiscal/years' },
      { id: 'fiscal-periods', labelKey: 'fiscalPeriods', icon: CalendarDays, path: '/ledger/fiscal/periods' },
    ],
  },
  {
    group: 'Transactions',
    groupKey: 'transactions',
    items: [
      { id: 'journals', labelKey: 'journalEntries', icon: FileText, path: '/ledger/journals' },
      { id: 'posting-rules', labelKey: 'postingRules', icon: Settings2, path: '/ledger/posting-rules' },
    ],
  },
  {
    group: 'Closing',
    groupKey: 'closing',
    items: [
      { id: 'period-closing', labelKey: 'periodClosing', icon: Archive, path: '/ledger/closing/period' },
      { id: 'year-closing', labelKey: 'yearEndClosing', icon: ArchiveRestore, path: '/ledger/closing/year' },
    ],
  },
  {
    group: 'Settings',
    groupKey: 'settings',
    items: [
      { id: 'currencies', labelKey: 'currencies', icon: Coins, path: '/ledger/currencies' },
      { id: 'exchange-rates', labelKey: 'exchangeRates', icon: ArrowLeftRight, path: '/ledger/exchange-rates' },
      { id: 'tenant-settings', labelKey: 'tenantSettings', icon: Shield, path: '/admin/tenant', roles: ['ADMIN'] },
    ],
  },
]

export function filterMenuByRoles(config: MenuGroup[], userRoles: string[]): MenuGroup[] {
  return config
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => !item.roles || item.roles.some((r) => userRoles.includes(r))),
    }))
    .filter((group) => group.items.length > 0)
}
