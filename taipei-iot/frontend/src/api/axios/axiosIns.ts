import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios'

const axiosIns: AxiosInstance = axios.create({
  baseURL: '/v1',
  timeout: 15000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

// --- Callback injection (avoid circular import with authStore) ---
let _refreshHandler: (() => Promise<string>) | null = null
let _logoutHandler: (() => void) | null = null

export function setAxiosToken(token: string | null) {
  if (token) {
    axiosIns.defaults.headers.common['Authorization'] = `Bearer ${token}`
  } else {
    delete axiosIns.defaults.headers.common['Authorization']
  }
}

export function setAxiosRefreshHandler(handler: () => Promise<string>) {
  _refreshHandler = handler
}

export function setAxiosLogoutHandler(handler: () => void) {
  _logoutHandler = handler
}

// Request interceptor
axiosIns.interceptors.request.use((config) => {
  return config
})

// Response interceptor — unwrap res.data
// Singleton refresh: prevent multiple 401s from triggering multiple refreshes
let refreshPromise: Promise<string> | null = null

axiosIns.interceptors.response.use(
  (res) => res.data,
  async (err) => {
    const originalRequest = err.config as InternalAxiosRequestConfig & { _retry?: boolean }
    if (
      err.response?.status === 401 &&
      !originalRequest._retry &&
      _refreshHandler &&
      !originalRequest.url?.includes('/noauth/token/refresh')
    ) {
      originalRequest._retry = true
      if (!refreshPromise) {
        refreshPromise = _refreshHandler()
          .catch(() => {
            if (_logoutHandler) _logoutHandler()
            return Promise.reject(err)
          })
          .finally(() => {
            refreshPromise = null
          })
      }
      try {
        const newToken = await refreshPromise
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`
        return axiosIns(originalRequest)
      } catch {
        return Promise.reject(err)
      }
    }
    return Promise.reject(err)
  },
)

export default axiosIns
