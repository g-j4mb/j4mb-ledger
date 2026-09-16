import { useNavigate } from 'react-router'
import { LayoutDashboard, BookOpen, FileText, Calendar } from 'lucide-react'
import { PageHeader } from '@/shell/components/layout/PageHeader'
import { Card, CardBody } from '@/shell/components/ui/Card'
import { Badge } from '@/shell/components/ui/Badge'
import { Button } from '@/shell/components/ui/Button'
import { DataTable, type Column } from '@/shell/components/data/DataTable'
import { useDashboardSummary, useRecentJournals } from '../hooks/useDashboard'
import type { RecentJournal } from '../types'

function StatCard({ icon: Icon, label, value, color }: { icon: React.ElementType; label: string; value: React.ReactNode; color: string }) {
  return (
    <Card>
      <CardBody className="flex items-center gap-4">
        <div className={`flex h-12 w-12 items-center justify-center rounded-lg ${color}`}>
          <Icon className="h-6 w-6 text-white" />
        </div>
        <div>
          <p className="text-sm text-[#64748b]">{label}</p>
          <p className="text-xl font-semibold text-[#0f172a]">{value}</p>
        </div>
      </CardBody>
    </Card>
  )
}

const statusVariant: Record<string, 'success' | 'warning' | 'info'> = {
  POSTED: 'success',
  DRAFT: 'warning',
  REVERSED: 'info',
}

export function DashboardPage() {
  const navigate = useNavigate()
  const { data: summary } = useDashboardSummary()
  const { data: journals, isLoading } = useRecentJournals()

  const columns: Column<RecentJournal>[] = [
    { key: 'referenceNo', header: 'Reference' },
    { key: 'entryDate', header: 'Date' },
    { key: 'description', header: 'Description' },
    {
      key: 'totalDebit',
      header: 'Amount',
      render: (r) => <span className="font-mono">{r.currency} {r.totalDebit.toLocaleString()}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      render: (r) => <Badge variant={statusVariant[r.status] ?? 'default'}>{r.status}</Badge>,
    },
  ]

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description="Ledger overview for ACME Corp"
        action={
          <div className="flex gap-2">
            <Button variant="secondary" size="sm" onClick={() => navigate('/ledger/coa')}>
              View COA
            </Button>
            <Button size="sm" onClick={() => navigate('/ledger/journals/new')}>
              New Journal Entry
            </Button>
          </div>
        }
      />

      {/* Stat cards */}
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={BookOpen} label="Total Accounts" value={summary?.totalAccounts ?? '—'} color="bg-blue-500" />
        <StatCard icon={Calendar} label="Open Fiscal Period" value={summary?.openFiscalPeriod ?? '—'} color="bg-emerald-500" />
        <StatCard icon={FileText} label="Journal Entries (30d)" value={summary?.journalEntriesLast30d ?? '—'} color="bg-violet-500" />
        <StatCard icon={LayoutDashboard} label="Last Closing" value={summary?.lastClosingDate ?? '—'} color="bg-amber-500" />
      </div>

      {/* Recent journals */}
      <div>
        <h2 className="mb-3 text-base font-semibold text-[#0f172a]">Recent Journal Entries</h2>
        <DataTable
          columns={columns}
          rows={journals ?? []}
          isLoading={isLoading}
          getRowKey={(r) => r.id}
          onRowClick={(r) => navigate(`/ledger/journals/${r.id}`)}
          emptyTitle="No recent entries"
        />
      </div>
    </div>
  )
}
