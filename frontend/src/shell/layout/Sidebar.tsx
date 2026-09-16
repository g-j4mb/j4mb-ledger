import { NavLink } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/shell/auth/AuthContext'
import { useUiStore } from '@/shell/store/uiStore'
import { menuConfig, filterMenuByRoles } from '@/shell/navigation/menuConfig'
import { cn } from '@/shell/utils/cn'

function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation('menu')
  const { user } = useAuth()
  const groups = filterMenuByRoles(menuConfig, user?.roles ?? [])

  return (
    <div className="flex h-full flex-col bg-[#0f172a] text-[#cbd5e1]">
      {/* Logo */}
      <div className="flex h-14 items-center px-5 border-b border-slate-700/50">
        <span className="text-lg font-bold text-white tracking-tight">J4MB</span>
        <span className="ms-2 text-xs text-[#64748b] font-medium uppercase tracking-widest">ERP</span>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto py-3 px-2">
        {groups.map((group, gi) => (
          <div key={group.group} className={cn(gi > 0 && 'mt-4')}>
            <p className="px-3 pb-1 pt-2 text-[10px] font-semibold uppercase tracking-widest text-slate-500 select-none">
              {t(`groups.${group.groupKey}`, group.group)}
            </p>
            {group.items.map((item) => {
              const Icon = item.icon
              return (
                <NavLink
                  key={item.id}
                  to={item.path}
                  onClick={onNavigate}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-[#1e40af] text-white'
                        : 'text-[#cbd5e1] hover:bg-[#1e293b] hover:text-white'
                    )
                  }
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  {t(`items.${item.labelKey}`, item.labelKey)}
                </NavLink>
              )
            })}
          </div>
        ))}
      </nav>

      {/* User footer */}
      {user && (
        <div className="border-t border-slate-700/50 px-4 py-3">
          <p className="text-xs font-medium text-white truncate">{user.name}</p>
          <p className="text-[11px] text-slate-500 truncate">{user.email}</p>
        </div>
      )}
    </div>
  )
}

export function Sidebar() {
  const { sidebarOpen, closeSidebar } = useUiStore()

  return (
    <>
      {/* Desktop sidebar */}
      <aside className="hidden md:flex md:w-[260px] md:shrink-0 md:flex-col h-screen sticky top-0">
        <SidebarContent />
      </aside>

      {/* Mobile drawer overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
          onClick={closeSidebar}
          aria-hidden="true"
        />
      )}

      {/* Mobile drawer */}
      <aside
        className={cn(
          'fixed inset-y-0 start-0 z-50 w-[260px] transition-transform duration-200 ease-in-out md:hidden',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        )}
        style={{ direction: 'ltr' }}
      >
        <SidebarContent onNavigate={closeSidebar} />
      </aside>
    </>
  )
}
