import { useParams, useNavigate } from 'react-router'
import { ArrowLeft, CheckCircle, AlertCircle } from 'lucide-react'
import { Button } from '@/shell/components/ui/Button'
import { Card, CardHeader, CardBody } from '@/shell/components/ui/Card'
import { Badge } from '@/shell/components/ui/Badge'
import { LoadingOverlay } from '@/shell/components/feedback/LoadingOverlay'
import { useJournal } from '../hooks/useJournals'
import { useAccounts } from '@/modules/accounts/hooks/useAccounts'
import type { JournalStatus } from '../types'

const statusVariant: Record<JournalStatus, 'success' | 'warning' | 'info'> = {
  POSTED: 'success', DRAFT: 'warning', CANCELLED: 'info',
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-[#94a3b8]">{label}</p>
      <p className="mt-0.5 text-sm text-[#0f172a]">{children}</p>
    </div>
  )
}

export function JournalDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: entry, isLoading } = useJournal(id ?? '')
  const { data: accountsPage } = useAccounts()
  const accountName = (accountId: string) =>
    accountsPage?.content.find((a) => a.id === accountId)?.name ?? accountId

  if (isLoading) return <LoadingOverlay />
  if (!entry) return <p className="text-sm text-[#64748b]">Entry not found.</p>

  const isBalanced = Math.abs(entry.totalDebit - entry.totalCredit) < 0.01

  return (
    <div>
      <div className="mb-4">
        <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
          <ArrowLeft className="me-1.5 h-4 w-4" /> Back
        </Button>
      </div>

      <Card className="mb-4">
        <CardHeader>
          <div className="flex items-center justify-between">
            <span className="font-mono text-sm font-semibold text-[#64748b]">{entry.journalNumber}</span>
            <Badge variant={statusVariant[entry.status]}>{entry.status}</Badge>
          </div>
          <h2 className="mt-1 text-lg font-semibold text-[#0f172a]">{entry.description}</h2>
        </CardHeader>
        <CardBody>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Field label="Type">{entry.transactionType}</Field>
            <Field label="Reference">{entry.reference || '—'}</Field>
            <Field label="Currency">{entry.currencyCode}</Field>
            <Field label="Posted By">{entry.postedBy || '—'}</Field>
            <Field label="Total Debit"><span className="font-mono">{entry.totalDebit.toLocaleString()}</span></Field>
            <Field label="Total Credit"><span className="font-mono">{entry.totalCredit.toLocaleString()}</span></Field>
          </div>
        </CardBody>
      </Card>

      {/* Balance indicator */}
      <div className={`mb-4 flex items-center gap-2 rounded-md border px-4 py-2.5 text-sm ${isBalanced ? 'border-green-200 bg-green-50 text-green-700' : 'border-red-200 bg-red-50 text-red-700'}`}>
        {isBalanced ? <CheckCircle className="h-4 w-4" /> : <AlertCircle className="h-4 w-4" />}
        {isBalanced ? 'Entry is balanced' : `Out of balance by ${Math.abs(entry.totalDebit - entry.totalCredit).toLocaleString()}`}
      </div>

      {/* Lines */}
      <Card>
        <CardHeader><h3 className="text-sm font-semibold text-[#0f172a]">Journal Lines</h3></CardHeader>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-[#e2e8f0] text-sm">
            <thead>
              <tr className="bg-[#f8fafc] text-xs font-semibold uppercase tracking-wide text-[#64748b]">
                <th className="px-4 py-3 text-start">Account</th>
                <th className="px-4 py-3 text-start">Description</th>
                <th className="px-4 py-3 text-end">Debit</th>
                <th className="px-4 py-3 text-end">Credit</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e2e8f0]">
              {entry.lines.map((line) => (
                <tr key={line.id}>
                  <td className="px-4 py-2.5 text-[#0f172a]">{accountName(line.accountId)}</td>
                  <td className="px-4 py-2.5 text-[#64748b]">{line.description}</td>
                  <td className="px-4 py-2.5 text-end font-mono">{line.entryType === 'DEBIT' ? line.amount.toLocaleString() : ''}</td>
                  <td className="px-4 py-2.5 text-end font-mono">{line.entryType === 'CREDIT' ? line.amount.toLocaleString() : ''}</td>
                </tr>
              ))}
              <tr className="bg-[#f8fafc] font-semibold">
                <td className="px-4 py-2.5 text-[#64748b]" colSpan={2}>Total</td>
                <td className="px-4 py-2.5 text-end font-mono text-[#0f172a]">{entry.totalDebit.toLocaleString()}</td>
                <td className="px-4 py-2.5 text-end font-mono text-[#0f172a]">{entry.totalCredit.toLocaleString()}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}
