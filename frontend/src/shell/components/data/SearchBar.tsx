import { Search, X } from 'lucide-react'
import { cn } from '@/shell/utils/cn'

interface SearchBarProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  className?: string
}

export function SearchBar({ value, onChange, placeholder = 'Search…', className }: SearchBarProps) {
  return (
    <div className={cn('relative', className)}>
      <Search className="absolute start-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#94a3b8]" />
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="h-9 w-full rounded-md border border-[#e2e8f0] bg-white ps-9 pe-8 text-sm text-[#0f172a] placeholder:text-[#94a3b8] focus:outline-none focus:ring-2 focus:ring-[#1d4ed8]"
      />
      {value && (
        <button
          onClick={() => onChange('')}
          className="absolute end-2 top-1/2 -translate-y-1/2 text-[#94a3b8] hover:text-[#64748b]"
        >
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  )
}
