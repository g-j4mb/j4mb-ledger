import axios from 'axios'

let tokenGetter: (() => string | null) | null = null
let tenantGetter: (() => string | null) | null = null

export function setTokenGetter(fn: () => string | null) {
  tokenGetter = fn
}

export function setTenantGetter(fn: () => string | null) {
  tenantGetter = fn
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = tokenGetter?.()
  if (token) config.headers['Authorization'] = `Bearer ${token}`

  const tenant = tenantGetter?.()
  if (tenant) config.headers['X-Tenant-Code'] = tenant

  return config
})

// The backend wraps every response as { success, data, error, timestamp, traceId }.
// Unwrap it here so callers can keep doing `apiClient.get<T>(...).then((r) => r.data)`
// and get the real payload T, not the envelope.
apiClient.interceptors.response.use(
  (res) => {
    if (res.data && typeof res.data === 'object' && 'success' in res.data && 'data' in res.data) {
      res.data = res.data.data
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
