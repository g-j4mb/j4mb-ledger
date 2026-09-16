import { useParams, useNavigate } from 'react-router'
import { ArrowLeft } from 'lucide-react'
import { Button } from '@/shell/components/ui/Button'
import { Card, CardHeader, CardBody } from '@/shell/components/ui/Card'
import { Badge } from '@/shell/components/ui/Badge'
import { LoadingOverlay } from '@/shell/components/feedback/LoadingOverlay'
import { useAccount } from '../hooks/useAccounts'
import type { AccountStatus } from '../types'

const statusVariant: Record<AccountStatus, 'success' | 'default' | 'error'> = {
  ACTIVE: 'success', FROZEN: 'default', CLOSED: 'error',
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-xs font-medium uppercase tracking-wide text-[#94a3b8]">{label}</span>
      <span className="text-sm text-[#0f172a]">{children}</span>
    </div>
  )
}

export function AccountDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: account, isLoading } = useAccount(id ?? '')

  if (isLoading) return <LoadingOverlay />
  if (!account) return <p className="text-[#64748b] text-sm">Account not found.</p>

  return (
    <div>
      <div className="mb-4">
        <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
          <ArrowLeft className="me-1.5 h-4 w-4" /> Back
        </Button>
      </div>
      <Card className="max-w-xl">
        <CardHeader>
          <div className="flex items-center justify-between">
            <span className="font-mono text-sm font-semibold text-[#64748b]">{account.accountNumber}</span>
            <Badge variant={statusVariant[account.status]}>{account.status}</Badge>
          </div>
          <h2 className="mt-1 text-lg font-semibold text-[#0f172a]">{account.name}</h2>
        </CardHeader>
        <CardBody>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Currency">{account.currencyCode}</Field>
            <Field label="Overdraft Limit"><span className="font-mono">{account.overdraftLimit.toLocaleString()}</span></Field>
            <Field label="COA Reference">{account.coaNodeId}</Field>
          </div>
        </CardBody>
      </Card>
    </div>
  )
}
