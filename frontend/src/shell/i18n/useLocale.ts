import { useTranslation } from 'react-i18next'
import { useEffect } from 'react'
import { SUPPORTED_LANGUAGES } from './index'

export function useLocale() {
  const { i18n } = useTranslation()
  const lang = i18n.language?.startsWith('ar') ? 'ar' : 'en'
  const dir = lang === 'ar' ? 'rtl' : 'ltr'

  useEffect(() => {
    document.documentElement.lang = lang
    document.documentElement.dir = dir
  }, [lang, dir])

  const changeLanguage = (code: string) => {
    i18n.changeLanguage(code)
  }

  return { lang, dir, changeLanguage, languages: SUPPORTED_LANGUAGES }
}
