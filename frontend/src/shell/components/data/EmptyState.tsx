import { Inbox } from 'lucide-react'

interface EmptyStateProps {
  title?: string
  description?: string
  action?: React.ReactNode
}

export function EmptyState({ title = 'No data', description = 'Nothing to display yet.', action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <Inbox className="mb-4 h-12 w-12 text-[#94a3b8]" />
      <p className="text-sm font-medium text-[#64748b]">{title}</p>
      <p className="mt-1 text-xs text-[#94a3b8]">{description}</p>
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
