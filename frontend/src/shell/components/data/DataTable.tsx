import { LoadingOverlay } from '@/shell/components/feedback/LoadingOverlay'
import { EmptyState } from '@/shell/components/data/EmptyState'
import { cn } from '@/shell/utils/cn'

export interface Column<T> {
  key: string
  header: string
  render?: (row: T) => React.ReactNode
  className?: string
}

interface DataTableProps<T> {
  columns: Column<T>[]
  rows: T[]
  isLoading?: boolean
  getRowKey: (row: T) => string
  onRowClick?: (row: T) => void
  emptyTitle?: string
  emptyDescription?: string
}

export function DataTable<T>({
  columns,
  rows,
  isLoading,
  getRowKey,
  onRowClick,
  emptyTitle,
  emptyDescription,
}: DataTableProps<T>) {
  if (isLoading) return <LoadingOverlay />

  if (!rows.length) return <EmptyState title={emptyTitle} description={emptyDescription} />

  return (
    <div className="overflow-x-auto rounded-lg border border-[#e2e8f0]">
      <table className="min-w-full divide-y divide-[#e2e8f0] bg-white text-sm">
        <thead>
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                className={cn('px-4 py-3 text-start text-xs font-semibold uppercase tracking-wide text-[#64748b] bg-[#f8fafc]', col.className)}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-[#e2e8f0]">
          {rows.map((row) => (
            <tr
              key={getRowKey(row)}
              onClick={() => onRowClick?.(row)}
              className={cn('transition-colors', onRowClick && 'cursor-pointer hover:bg-[#f8fafc]')}
            >
              {columns.map((col) => (
                <td key={col.key} className={cn('px-4 py-3 text-[#0f172a]', col.className)}>
                  {col.render ? col.render(row) : (row as Record<string, unknown>)[col.key] as React.ReactNode}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
