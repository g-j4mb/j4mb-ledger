import { Menu, LogOut, Globe } from 'lucide-react'
import { useAuth } from '@/shell/auth/AuthContext'
import { useUiStore } from '@/shell/store/uiStore'
import { useLocale } from '@/shell/i18n/useLocale'
import { useTranslation } from 'react-i18next'

export function TopBar() {
  const { user, logout } = useAuth()
  const { toggleSidebar } = useUiStore()
  const { lang, changeLanguage } = useLocale()
  const { t } = useTranslation()

  return (
    <header className="flex h-14 items-center border-b border-[#e2e8f0] bg-white px-4 shrink-0">
      {/* Hamburger — mobile only */}
      <button
        onClick={toggleSidebar}
        className="me-3 rounded-md p-2 text-[#64748b] hover:bg-[#f8fafc] hover:text-[#0f172a] md:hidden"
        aria-label="Toggle menu"
      >
        <Menu className="h-5 w-5" />
      </button>

      {/* Tenant badge */}
      <span className="rounded-full bg-blue-100 px-3 py-0.5 text-xs font-semibold text-blue-700">
        {user?.tenantCode ?? 'ACME'}
      </span>

      <div className="flex-1" />

      {/* Language toggle */}
      <button
        onClick={() => changeLanguage(lang === 'en' ? 'ar' : 'en')}
        className="me-2 flex items-center gap-1 rounded-md px-2 py-1.5 text-xs text-[#64748b] hover:bg-[#f8fafc] hover:text-[#0f172a]"
        title="Switch language"
      >
        <Globe className="h-4 w-4" />
        {lang === 'en' ? 'AR' : 'EN'}
      </button>

      {/* User avatar + logout */}
      <div className="flex items-center gap-2">
        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-[#1d4ed8] text-xs font-semibold text-white select-none">
          {user?.name.charAt(0) ?? 'A'}
        </div>
        <button
          onClick={logout}
          className="flex items-center gap-1 rounded-md px-2 py-1.5 text-xs text-[#64748b] hover:bg-[#f8fafc] hover:text-[#dc2626]"
          title={t('actions.logout')}
        >
          <LogOut className="h-4 w-4" />
        </button>
      </div>
    </header>
  )
}
