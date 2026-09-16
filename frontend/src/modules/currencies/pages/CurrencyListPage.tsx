import { PageHeader } from '@/shell/components/layout/PageHeader'
import { DataTable, type Column } from '@/shell/components/data/DataTable'
import { Badge } from '@/shell/components/ui/Badge'
import { useCurrencies } from '../hooks/useCurrencies'
import type { Currency } from '../types'

export function CurrencyListPage() {
  const { data, isLoading } = useCurrencies()

  const columns: Column<Currency>[] = [
    { key: 'currencyCode', header: 'Code', render: (r) => <span className="font-mono font-semibold">{r.currencyCode}</span> },
    { key: 'currencyName', header: 'Name' },
    { key: 'decimalPlaces', header: 'Decimals' },
    {
      key: 'baseCurrency',
      header: 'Base',
      render: (r) => r.baseCurrency ? <Badge variant="info">Base</Badge> : null,
    },
  ]

  return (
    <div>
      <PageHeader title="Currencies" description="Supported currencies" />
      <DataTable
        columns={columns}
        rows={data ?? []}
        isLoading={isLoading}
        getRowKey={(r) => r.id}
        emptyTitle="No currencies configured"
      />
    </div>
  )
}
