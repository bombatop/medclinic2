import axios from 'axios'
import { refresh } from './auth'
import { useAuthStore } from '@/stores/auth'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

let refreshPromise: Promise<boolean> | null = null

function isAuthRequest(url: string) {
  return url?.includes('/auth/auth/login') || url?.includes('/auth/auth/refresh') || url?.includes('/auth/auth/logout')
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status !== 401) {
      return Promise.reject(error)
    }

    if (isAuthRequest(originalRequest?.url)) {
      useAuthStore().clearTokens()
      if (!originalRequest?.url?.includes('/auth/auth/login')) {
        import('@/router').then(({ default: router }) => router.push({ name: 'login' }))
      }
      return Promise.reject(error)
    }

    if (!refreshPromise) {
      refreshPromise = refresh()
        .then((res) => {
          useAuthStore().setTokens(res.accessToken, res.roles, res.permissions)
          return true
        })
        .catch(async () => {
          await useAuthStore().clearTokens()
          const { default: router } = await import('@/router')
          router.push({ name: 'login' })
          return false
        })
        .finally(() => {
          refreshPromise = null
        })
    }

    const refreshed = await refreshPromise
    if (refreshed && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true
      return http(originalRequest)
    }

    return Promise.reject(error)
  },
)

export default http
