import { PageHeader } from '@/shell/components/layout/PageHeader'
import { DataTable, type Column } from '@/shell/components/data/DataTable'
import { Badge } from '@/shell/components/ui/Badge'
import { usePostingRules } from '../hooks/usePostingRules'
import type { PostingRule } from '../types'

export function PostingRuleListPage() {
  const { data, isLoading } = usePostingRules()

  const columns: Column<PostingRule>[] = [
    { key: 'name', header: 'Name', render: (r) => <span className="font-medium">{r.name}</span> },
    { key: 'triggerEvent', header: 'Trigger Event', render: (r) => <span className="font-mono text-xs bg-slate-100 px-2 py-0.5 rounded">{r.triggerEvent}</span> },
    { key: 'debitAccount', header: 'Debit Account' },
    { key: 'creditAccount', header: 'Credit Account' },
    {
      key: 'isActive',
      header: 'Status',
      render: (r) => <Badge variant={r.isActive ? 'success' : 'default'}>{r.isActive ? 'Active' : 'Inactive'}</Badge>,
    },
  ]

  return (
    <div>
      <PageHeader title="Posting Rules" description="Automated journal posting rules" />
      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        isLoading={isLoading}
        getRowKey={(r) => r.id}
        emptyTitle="No posting rules defined"
      />
    </div>
  )
}
