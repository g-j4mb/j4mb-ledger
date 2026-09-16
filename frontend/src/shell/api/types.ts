export interface ApiResponse<T> {
  data: T
  message?: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
