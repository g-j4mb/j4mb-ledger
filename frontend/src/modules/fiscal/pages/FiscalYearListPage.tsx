import { useNavigate } from 'react-router'
import { PageHeader } from '@/shell/components/layout/PageHeader'
import { DataTable, type Column } from '@/shell/components/data/DataTable'
import { Badge } from '@/shell/components/ui/Badge'
import { Button } from '@/shell/components/ui/Button'
import { useFiscalYears } from '../hooks/useFiscal'
import type { FiscalYear, FiscalStatus } from '../types'

const statusVariant: Record<FiscalStatus, 'success' | 'default' | 'error'> = {
  OPEN: 'success', CLOSED: 'default', LOCKED: 'error',
}

export function FiscalYearListPage() {
  const navigate = useNavigate()
  const { data, isLoading } = useFiscalYears()

  const columns: Column<FiscalYear>[] = [
    { key: 'yearName', header: 'Fiscal Year' },
    { key: 'startDate', header: 'Start Date' },
    { key: 'endDate', header: 'End Date' },
    { key: 'status', header: 'Status', render: (r) => <Badge variant={statusVariant[r.status]}>{r.status}</Badge> },
    {
      key: 'actions',
      header: '',
      render: (r) => (
        <Button size="sm" variant="ghost" onClick={(e) => { e.stopPropagation(); navigate(`/ledger/fiscal/periods?yearId=${r.id}`) }}>
          View Periods
        </Button>
      ),
    },
  ]

  return (
    <div>
      <PageHeader title="Fiscal Years" description="Manage fiscal years and periods" />
      <DataTable
        columns={columns}
        rows={data ?? []}
        isLoading={isLoading}
        getRowKey={(r) => r.id}
        emptyTitle="No fiscal years defined"
      />
    </div>
  )
}
