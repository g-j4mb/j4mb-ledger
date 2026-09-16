import { useState } from 'react'
import { useNavigate } from 'react-router'
import { Plus } from 'lucide-react'
import { PageHeader } from '@/shell/components/layout/PageHeader'
import { DataTable, type Column } from '@/shell/components/data/DataTable'
import { Badge } from '@/shell/components/ui/Badge'
import { Button } from '@/shell/components/ui/Button'
import { Select } from '@/shell/components/ui/Select'
import { useJournals } from '../hooks/useJournals'
import type { JournalEntry, JournalStatus } from '../types'

const statusVariant: Record<JournalStatus, 'success' | 'warning' | 'info'> = {
  POSTED: 'success', DRAFT: 'warning', CANCELLED: 'info',
}

const statusOptions = [
  { value: '', label: 'All Statuses' },
  { value: 'POSTED', label: 'Posted' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

export function JournalListPage() {
  const navigate = useNavigate()
  const [status, setStatus] = useState('')
  const { data, isLoading } = useJournals(status || undefined)

  const columns: Column<JournalEntry>[] = [
    { key: 'journalNumber', header: 'Journal #', render: (r) => <span className="font-mono text-sm font-medium">{r.journalNumber}</span> },
    { key: 'transactionType', header: 'Type' },
    { key: 'description', header: 'Description' },
    { key: 'currencyCode', header: 'CCY' },
    {
      key: 'totalDebit',
      header: 'Debit',
      render: (r) => <span className="font-mono text-sm">{r.totalDebit.toLocaleString()}</span>,
    },
    {
      key: 'totalCredit',
      header: 'Credit',
      render: (r) => <span className="font-mono text-sm">{r.totalCredit.toLocaleString()}</span>,
    },
    { key: 'status', header: 'Status', render: (r) => <Badge variant={statusVariant[r.status]}>{r.status}</Badge> },
  ]

  return (
    <div>
      <PageHeader
        title="Journal Entries"
        action={
          <Button size="sm" onClick={() => navigate('/ledger/journals/new')}>
            <Plus className="me-1.5 h-4 w-4" /> New Entry
          </Button>
        }
      />
      <div className="mb-4">
        <Select options={statusOptions} value={status} onChange={(e) => setStatus(e.target.value)} className="w-44" />
      </div>
      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        isLoading={isLoading}
        getRowKey={(r) => r.id}
        onRowClick={(r) => navigate(`/ledger/journals/${r.id}`)}
        emptyTitle="No journal entries found"
      />
    </div>
  )
}
