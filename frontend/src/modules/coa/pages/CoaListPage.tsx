import { useState } from 'react'
import { ChevronRight, ChevronDown } from 'lucide-react'
import { PageHeader } from '@/shell/components/layout/PageHeader'
import { Badge } from '@/shell/components/ui/Badge'
import { LoadingOverlay } from '@/shell/components/feedback/LoadingOverlay'
import { useCoa, buildTree, type TreeNode } from '../hooks/useCoa'
import { cn } from '@/shell/utils/cn'

const typeVariant: Record<string, 'info' | 'success' | 'warning' | 'error' | 'default'> = {
  ASSET: 'info',
  LIABILITY: 'error',
  EQUITY: 'warning',
  REVENUE: 'success',
  EXPENSE: 'default',
}

function CoaRow({ node, depth = 0 }: { node: TreeNode; depth?: number }) {
  const [expanded, setExpanded] = useState(depth === 0)
  const hasChildren = node.children.length > 0

  return (
    <>
      <tr className="border-b border-[#e2e8f0] hover:bg-[#f8fafc]">
        <td className="px-4 py-2.5">
          <div className="flex items-center" style={{ paddingInlineStart: `${depth * 20}px` }}>
            {hasChildren ? (
              <button onClick={() => setExpanded((e) => !e)} className="me-1.5 text-[#94a3b8] hover:text-[#0f172a]">
                {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
              </button>
            ) : (
              <span className="me-1.5 w-4" />
            )}
            <span className="font-mono text-sm text-[#64748b]">{node.code}</span>
          </div>
        </td>
        <td className="px-4 py-2.5 text-sm text-[#0f172a]">{node.name}</td>
        <td className="px-4 py-2.5">
          <Badge variant={typeVariant[node.accountType] ?? 'default'}>{node.accountType}</Badge>
        </td>
        <td className="px-4 py-2.5 text-sm text-[#64748b]">{node.normalBalance}</td>
        <td className="px-4 py-2.5">
          <Badge variant={node.postable ? 'success' : 'default'}>{node.postable ? 'Postable' : 'Group'}</Badge>
        </td>
      </tr>
      {expanded && node.children.map((child) => (
        <CoaRow key={child.id} node={child} depth={depth + 1} />
      ))}
    </>
  )
}

export function CoaListPage() {
  const { data, isLoading } = useCoa()
  const tree = buildTree(data ?? [])

  if (isLoading) return <LoadingOverlay />

  return (
    <div>
      <PageHeader title="Chart of Accounts" description="Hierarchical account structure" />
      <div className={cn('overflow-x-auto rounded-lg border border-[#e2e8f0] bg-white')}>
        <table className="min-w-full text-sm">
          <thead>
            <tr className="bg-[#f8fafc] text-xs font-semibold uppercase tracking-wide text-[#64748b]">
              <th className="px-4 py-3 text-start">Code</th>
              <th className="px-4 py-3 text-start">Name</th>
              <th className="px-4 py-3 text-start">Type</th>
              <th className="px-4 py-3 text-start">Normal Balance</th>
              <th className="px-4 py-3 text-start">Kind</th>
            </tr>
          </thead>
          <tbody>
            {tree.map((node) => (
              <CoaRow key={node.id} node={node} depth={0} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
