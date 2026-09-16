import { PageHeader } from '@/shell/components/layout/PageHeader'
import { DataTable, type Column } from '@/shell/components/data/DataTable'
import { useExchangeRates } from '../hooks/useCurrencies'
import type { ExchangeRate } from '../types'

export function ExchangeRateListPage() {
  const { data, isLoading } = useExchangeRates()

  const columns: Column<ExchangeRate>[] = [
    { key: 'fromCurrency', header: 'From', render: (r) => <span className="font-mono font-semibold">{r.fromCurrency}</span> },
    { key: 'toCurrency', header: 'To', render: (r) => <span className="font-mono font-semibold">{r.toCurrency}</span> },
    { key: 'rate', header: 'Rate', render: (r) => <span className="font-mono">{r.rate.toFixed(4)}</span> },
    { key: 'effectiveDate', header: 'Effective Date' },
  ]

  return (
    <div>
      <PageHeader title="Exchange Rates" description="Current exchange rates" />
      <DataTable
        columns={columns}
        rows={data ?? []}
        isLoading={isLoading}
        getRowKey={(r) => r.id}
        emptyTitle="No exchange rates found"
      />
    </div>
  )
}
