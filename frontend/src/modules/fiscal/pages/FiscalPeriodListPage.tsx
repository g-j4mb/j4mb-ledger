import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { PageHeader } from '@/shell/components/layout/PageHeader'
import { DataTable, type Column } from '@/shell/components/data/DataTable'
import { Badge } from '@/shell/components/ui/Badge'
import { Select } from '@/shell/components/ui/Select'
import { useFiscalYears, useFiscalPeriods } from '../hooks/useFiscal'
import type { FiscalPeriod, FiscalStatus } from '../types'

const statusVariant: Record<FiscalStatus, 'success' | 'default' | 'error'> = {
  OPEN: 'success', CLOSED: 'default', LOCKED: 'error',
}

export function FiscalPeriodListPage() {
  const [searchParams] = useSearchParams()
  const initialYearId = searchParams.get('yearId') ?? ''
  const [selectedYearId, setSelectedYearId] = useState(initialYearId)

  const { data: yearsData } = useFiscalYears()
  const { data, isLoading } = useFiscalPeriods(selectedYearId || undefined)

  const yearOptions = [
    { value: '', label: 'All Years' },
    ...(yearsData ?? []).map((y) => ({ value: y.id, label: y.yearName })),
  ]

  const columns: Column<FiscalPeriod>[] = [
    { key: 'periodNumber', header: '#', render: (r) => <span className="font-mono text-sm">{r.periodNumber}</span> },
    { key: 'periodName', header: 'Period' },
    { key: 'startDate', header: 'Start Date' },
    { key: 'endDate', header: 'End Date' },
    { key: 'status', header: 'Status', render: (r) => <Badge variant={statusVariant[r.status]}>{r.status}</Badge> },
  ]

  return (
    <div>
      <PageHeader title="Fiscal Periods" description="Monthly fiscal periods" />
      <div className="mb-4">
        <Select
          options={yearOptions}
          value={selectedYearId}
          onChange={(e) => setSelectedYearId(e.target.value)}
          className="w-48"
        />
      </div>
      <DataTable
        columns={columns}
        rows={data ?? []}
        isLoading={isLoading}
        getRowKey={(r) => r.id}
        emptyTitle="No periods found"
      />
    </div>
  )
}
