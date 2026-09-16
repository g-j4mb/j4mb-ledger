export interface Currency {
  id: string
  currencyCode: string
  currencyName: string
  baseCurrency: boolean
  active: boolean
  decimalPlaces: number
}

export interface ExchangeRate {
  id: string
  fromCurrency: string
  toCurrency: string
  rate: number
  effectiveDate: string
}
