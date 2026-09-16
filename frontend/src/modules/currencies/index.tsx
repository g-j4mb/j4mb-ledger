import { useLocation } from 'react-router'
import { CurrencyListPage } from './pages/CurrencyListPage'
import { ExchangeRateListPage } from './pages/ExchangeRateListPage'

export default function Currencies() {
  const { pathname } = useLocation()
  if (pathname.includes('exchange-rates')) return <ExchangeRateListPage />
  return <CurrencyListPage />
}
