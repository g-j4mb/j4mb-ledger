import { useState } from 'react'
import { useNavigate } from 'react-router'
import { Plus, Trash2, AlertCircle, CheckCircle, ArrowLeft } from 'lucide-react'
import { PageHeader } from '@/shell/components/layout/PageHeader'
import { Button } from '@/shell/components/ui/Button'
import { Input } from '@/shell/components/ui/Input'
import { Select } from '@/shell/components/ui/Select'
import { Card, CardHeader, CardBody } from '@/shell/components/ui/Card'
import { useCreateJournal } from '../hooks/useJournals'
import { useAccounts } from '@/modules/accounts/hooks/useAccounts'
import { useCurrencies } from '@/modules/currencies/hooks/useCurrencies'
import { useFiscalPeriods } from '@/modules/fiscal/hooks/useFiscal'
import type { TransactionType, EntryType } from '../types'

const transactionTypeOptions: { value: TransactionType; label: string }[] = [
  { value: 'SALE', label: 'Sale' },
  { value: 'PURCHASE', label: 'Purchase' },
  { value: 'PAYMENT', label: 'Payment' },
  { value: 'RECEIPT', label: 'Receipt' },
  { value: 'ADJUSTMENT', label: 'Adjustment' },
  { value: 'TRANSFER', label: 'Transfer' },
]

interface LineItem {
  accountId: string
  entryType: EntryType
  amount: string
  description: string
}

const emptyLine = (): LineItem => ({ accountId: '', entryType: 'DEBIT', amount: '', description: '' })

export function JournalFormPage() {
  const navigate = useNavigate()
  const { mutateAsync, isPending } = useCreateJournal()
  const { data: accountsPage } = useAccounts()
  const { data: currencies } = useCurrencies()
  const { data: periods } = useFiscalPeriods()

  const accountOptions = (accountsPage?.content ?? []).map((a) => ({
    value: a.id,
    label: `${a.accountNumber} — ${a.name}`,
  }))
  const currencyOptions = (currencies ?? []).map((c) => ({ value: c.currencyCode, label: c.currencyCode }))
  const periodOptions = (periods ?? []).map((p) => ({ value: p.id, label: p.periodName }))

  const [description, setDescription] = useState('')
  const [reference, setReference] = useState('')
  const [transactionType, setTransactionType] = useState<TransactionType>('ADJUSTMENT')
  const [fiscalPeriodId, setFiscalPeriodId] = useState('')
  const [currencyCode, setCurrencyCode] = useState('')
  const [lines, setLines] = useState<LineItem[]>([emptyLine(), emptyLine()])
  const [error, setError] = useState('')

  const totalDebit = lines.filter((l) => l.entryType === 'DEBIT').reduce((s, l) => s + (parseFloat(l.amount) || 0), 0)
  const totalCredit = lines.filter((l) => l.entryType === 'CREDIT').reduce((s, l) => s + (parseFloat(l.amount) || 0), 0)
  const isBalanced = Math.abs(totalDebit - totalCredit) < 0.01 && totalDebit > 0

  const updateLine = (idx: number, field: keyof LineItem, value: string) => {
    setLines((prev) => prev.map((l, i) => i === idx ? { ...l, [field]: value } : l))
  }
  const addLine = () => setLines((prev) => [...prev, emptyLine()])
  const removeLine = (idx: number) => setLines((prev) => prev.filter((_, i) => i !== idx))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!fiscalPeriodId) { setError('Fiscal period is required'); return }
    if (!currencyCode) { setError('Currency is required'); return }
    if (!isBalanced) { setError('Entry must be balanced (Debit = Credit)'); return }
    const validLines = lines.filter((l) => l.accountId && parseFloat(l.amount) > 0)
    if (validLines.length < 2) { setError('At least 2 line items are required'); return }

    try {
      await mutateAsync({
        fiscalPeriodId,
        transactionType,
        currencyCode,
        description,
        reference: reference || undefined,
        lines: validLines.map((l) => ({
          accountId: l.accountId,
          entryType: l.entryType,
          amount: parseFloat(l.amount),
          currencyCode,
          description: l.description || undefined,
        })),
      })
      navigate('/ledger/journals')
    } catch {
      setError('Failed to save journal entry')
    }
  }

  return (
    <div>
      <div className="mb-4">
        <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
          <ArrowLeft className="me-1.5 h-4 w-4" /> Back
        </Button>
      </div>
      <PageHeader title="New Journal Entry" />

      <form onSubmit={handleSubmit}>
        {/* Header fields */}
        <Card className="mb-4">
          <CardHeader><h3 className="text-sm font-semibold text-[#0f172a]">Entry Details</h3></CardHeader>
          <CardBody>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="sm:col-span-1">
                <Select
                  id="fiscalPeriod"
                  label="Fiscal Period"
                  options={[{ value: '', label: 'Select period' }, ...periodOptions]}
                  value={fiscalPeriodId}
                  onChange={(e) => setFiscalPeriodId(e.target.value)}
                />
              </div>
              <div className="sm:col-span-1">
                <Select
                  id="transactionType"
                  label="Transaction Type"
                  options={transactionTypeOptions}
                  value={transactionType}
                  onChange={(e) => setTransactionType(e.target.value as TransactionType)}
                />
              </div>
              <div className="sm:col-span-1">
                <Select
                  id="currency"
                  label="Currency"
                  options={[{ value: '', label: 'Select currency' }, ...currencyOptions]}
                  value={currencyCode}
                  onChange={(e) => setCurrencyCode(e.target.value)}
                />
              </div>
              <div className="sm:col-span-2">
                <Input
                  id="description"
                  label="Description"
                  placeholder="Journal entry description"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>
              <div className="sm:col-span-1">
                <Input
                  id="reference"
                  label="Reference"
                  placeholder="External reference (optional)"
                  value={reference}
                  onChange={(e) => setReference(e.target.value)}
                />
              </div>
            </div>
          </CardBody>
        </Card>

        {/* Lines */}
        <Card className="mb-4">
          <CardHeader className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-[#0f172a]">Journal Lines</h3>
            <Button type="button" variant="ghost" size="sm" onClick={addLine}>
              <Plus className="me-1 h-4 w-4" /> Add Line
            </Button>
          </CardHeader>
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="bg-[#f8fafc] text-xs font-semibold uppercase tracking-wide text-[#64748b]">
                  <th className="px-4 py-3 text-start">Account</th>
                  <th className="px-4 py-3 text-start">Description</th>
                  <th className="px-4 py-3 w-28 text-start">Entry</th>
                  <th className="px-4 py-3 text-end w-32">Amount</th>
                  <th className="px-4 py-3 w-10" />
                </tr>
              </thead>
              <tbody className="divide-y divide-[#e2e8f0]">
                {lines.map((line, idx) => (
                  <tr key={idx}>
                    <td className="px-4 py-2">
                      <Select
                        options={accountOptions}
                        value={line.accountId}
                        onChange={(e) => updateLine(idx, 'accountId', e.target.value)}
                        placeholder="Select account"
                        className="min-w-[200px]"
                      />
                    </td>
                    <td className="px-4 py-2">
                      <Input
                        placeholder="Description"
                        value={line.description}
                        onChange={(e) => updateLine(idx, 'description', e.target.value)}
                      />
                    </td>
                    <td className="px-4 py-2">
                      <Select
                        options={[{ value: 'DEBIT', label: 'Debit' }, { value: 'CREDIT', label: 'Credit' }]}
                        value={line.entryType}
                        onChange={(e) => updateLine(idx, 'entryType', e.target.value)}
                      />
                    </td>
                    <td className="px-4 py-2">
                      <Input
                        type="number"
                        placeholder="0.00"
                        value={line.amount}
                        onChange={(e) => updateLine(idx, 'amount', e.target.value)}
                        className="text-end"
                      />
                    </td>
                    <td className="px-4 py-2">
                      {lines.length > 2 && (
                        <button type="button" onClick={() => removeLine(idx)} className="text-[#94a3b8] hover:text-[#dc2626]">
                          <Trash2 className="h-4 w-4" />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
                {/* Totals row */}
                <tr className="bg-[#f8fafc] font-semibold border-t-2 border-[#e2e8f0]">
                  <td className="px-4 py-2.5 text-[#64748b]" colSpan={3}>Total (Debit / Credit)</td>
                  <td className="px-4 py-2.5 text-end font-mono text-[#0f172a]">{totalDebit.toLocaleString()} / {totalCredit.toLocaleString()}</td>
                  <td />
                </tr>
              </tbody>
            </table>
          </div>

          {/* Balance indicator */}
          <div className={`mx-4 mb-4 flex items-center gap-2 rounded-md border px-3 py-2 text-sm ${isBalanced ? 'border-green-200 bg-green-50 text-green-700' : 'border-amber-200 bg-amber-50 text-amber-700'}`}>
            {isBalanced ? <CheckCircle className="h-4 w-4" /> : <AlertCircle className="h-4 w-4" />}
            {isBalanced ? 'Entry is balanced' : totalDebit > 0 || totalCredit > 0 ? `Difference: ${Math.abs(totalDebit - totalCredit).toLocaleString()}` : 'Add amounts to check balance'}
          </div>
        </Card>

        {error && (
          <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-600">{error}</div>
        )}

        <div className="flex gap-3">
          <Button type="submit" loading={isPending} disabled={!isBalanced}>
            Save Draft
          </Button>
          <Button type="button" variant="secondary" onClick={() => navigate(-1)}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  )
}
