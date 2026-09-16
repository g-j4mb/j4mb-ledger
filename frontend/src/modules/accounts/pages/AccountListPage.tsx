import { useState } from 'react'
import { useNavigate } from 'react-router'
import { PageHeader } from '@/shell/components/layout/PageHeader'
import { DataTable, type Column } from '@/shell/components/data/DataTable'
import { SearchBar } from '@/shell/components/data/SearchBar'
import { Badge } from '@/shell/components/ui/Badge'
import { useAccounts } from '../hooks/useAccounts'
import type { Account, AccountStatus } from '../types'

const statusVariant: Record<AccountStatus, 'success' | 'default' | 'error'> = {
  ACTIVE: 'success', FROZEN: 'default', CLOSED: 'error',
}

export function AccountListPage() {
  const navigate = useNavigate()
  const [search, setSearch] = useState('')

  const { data, isLoading } = useAccounts()
  const rows = (data?.content ?? []).filter(
    (a) => !search || a.name.toLowerCase().includes(search.toLowerCase()) || a.accountNumber.includes(search)
  )

  const columns: Column<Account>[] = [
    { key: 'accountNumber', header: 'Account #', render: (r) => <span className="font-mono text-sm">{r.accountNumber}</span> },
    { key: 'name', header: 'Name' },
    { key: 'currencyCode', header: 'Currency' },
    { key: 'status', header: 'Status', render: (r) => <Badge variant={statusVariant[r.status]}>{r.status}</Badge> },
  ]

  return (
    <div>
      <PageHeader title="Accounts" description="All ledger accounts" />
      <div className="mb-4 flex flex-col gap-3 sm:flex-row">
        <SearchBar value={search} onChange={setSearch} placeholder="Search accounts…" className="sm:w-64" />
      </div>
      <DataTable
        columns={columns}
        rows={rows}
        isLoading={isLoading}
        getRowKey={(r) => r.id}
        onRowClick={(r) => navigate(`/ledger/accounts/${r.id}`)}
        emptyTitle="No accounts found"
      />
    </div>
  )
}
