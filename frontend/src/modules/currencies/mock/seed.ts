import type { Currency, ExchangeRate } from '../types'

export const currencySeed: Currency[] = [
  { id: 'cur-01', currencyCode: 'USD', currencyName: 'US Dollar', baseCurrency: true, active: true, decimalPlaces: 2 },
  { id: 'cur-02', currencyCode: 'EUR', currencyName: 'Euro', baseCurrency: false, active: true, decimalPlaces: 2 },
  { id: 'cur-03', currencyCode: 'GBP', currencyName: 'British Pound', baseCurrency: false, active: true, decimalPlaces: 2 },
  { id: 'cur-04', currencyCode: 'SAR', currencyName: 'Saudi Riyal', baseCurrency: false, active: true, decimalPlaces: 2 },
  { id: 'cur-05', currencyCode: 'AED', currencyName: 'UAE Dirham', baseCurrency: false, active: true, decimalPlaces: 2 },
]

export const exchangeRateSeed: ExchangeRate[] = [
  { id: 'er-01', fromCurrency: 'USD', toCurrency: 'EUR', rate: 0.9234, effectiveDate: '2026-06-01' },
  { id: 'er-02', fromCurrency: 'USD', toCurrency: 'GBP', rate: 0.7891, effectiveDate: '2026-06-01' },
  { id: 'er-03', fromCurrency: 'USD', toCurrency: 'SAR', rate: 3.7500, effectiveDate: '2026-06-01' },
  { id: 'er-04', fromCurrency: 'USD', toCurrency: 'AED', rate: 3.6725, effectiveDate: '2026-06-01' },
  { id: 'er-05', fromCurrency: 'EUR', toCurrency: 'USD', rate: 1.0828, effectiveDate: '2026-06-01' },
  { id: 'er-06', fromCurrency: 'EUR', toCurrency: 'GBP', rate: 0.8546, effectiveDate: '2026-06-01' },
  { id: 'er-07', fromCurrency: 'GBP', toCurrency: 'USD', rate: 1.2673, effectiveDate: '2026-06-01' },
  { id: 'er-08', fromCurrency: 'SAR', toCurrency: 'USD', rate: 0.2667, effectiveDate: '2026-06-01' },
]
