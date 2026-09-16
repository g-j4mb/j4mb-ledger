import { useLocation } from 'react-router'
import { PageHeader } from '@/shell/components/layout/PageHeader'
import { DataTable, type Column } from '@/shell/components/data/DataTable'
import { Badge } from '@/shell/components/ui/Badge'
import { Button } from '@/shell/components/ui/Button'
import { useClosingRecords } from '../hooks/useClosing'
import type { ClosingRecord, ClosingType, ClosingStatus } from '../types'

const statusVariant: Record<ClosingStatus, 'success' | 'warning' | 'error'> = {
  COMPLETED: 'success', PENDING: 'warning', FAILED: 'error',
}
const typeVariant: Record<ClosingType, 'info' | 'warning'> = {
  PERIOD: 'info', YEAR: 'warning',
}

export function ClosingPage() {
  const { pathname } = useLocation()
  const isYearEnd = pathname.includes('year')
  const { data, isLoading } = useClosingRecords()

  const records = (data?.content ?? []).filter((r) =>
    isYearEnd ? r.type === 'YEAR' : r.type === 'PERIOD'
  )

  const title = isYearEnd ? 'Year-End Closing' : 'Period Closing'

  const columns: Column<ClosingRecord>[] = [
    { key: 'type', header: 'Type', render: (r) => <Badge variant={typeVariant[r.type]}>{r.type}</Badge> },
    { key: 'period', header: 'Period', render: (r) => <span className="font-medium">{r.period}</span> },
    { key: 'closedAt', header: 'Closed At', render: (r) => <span>{new Date(r.closedAt).toLocaleDateString()}</span> },
    { key: 'closedBy', header: 'Closed By' },
    { key: 'status', header: 'Status', render: (r) => <Badge variant={statusVariant[r.status]}>{r.status}</Badge> },
    { key: 'notes', header: 'Notes', render: (r) => <span className="text-[#64748b] text-xs">{r.notes}</span> },
  ]

  return (
    <div>
      <PageHeader
        title={title}
        description="Closing history and operations"
        action={
          <Button
            size="sm"
            disabled
            title="Connect to backend to enable closing operations"
            className="cursor-not-allowed opacity-60"
          >
            {isYearEnd ? 'Close Year' : 'Close Period'}
          </Button>
        }
      />
      <div className="mb-4 rounded-md border border-amber-200 bg-amber-50 px-4 py-2.5 text-sm text-amber-700">
        Closing operations are read-only in mock mode. Set <code className="font-mono text-xs">VITE_USE_MOCK=false</code> to connect to the backend.
      </div>
      <DataTable
        columns={columns}
        rows={records}
        isLoading={isLoading}
        getRowKey={(r) => r.id}
        emptyTitle={`No ${isYearEnd ? 'year-end' : 'period'} closings found`}
      />
    </div>
  )
}
