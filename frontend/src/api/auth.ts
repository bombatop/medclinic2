import axios from 'axios'
import http from './http'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export interface LoginRequest {
  username: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  role: string
  username: string
}

export interface CurrentUser {
  id: number
  username: string
  firstName: string | null
  lastName: string | null
  email: string | null
  phone: string | null
  role: string
  active: boolean
  createdAt: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

export interface ApiError {
  status?: number
  message?: string
}

export function login(data: LoginRequest): Promise<AuthResponse> {
  return http.post<AuthResponse>('/auth/auth/login', data).then((res) => res.data)
}

/** Uses raw axios to avoid interceptor recursion when called from 401 handler */
export function refresh(refreshToken: string): Promise<AuthResponse> {
  return axios
    .post<AuthResponse>(`${baseURL}/auth/auth/refresh`, { refreshToken })
    .then((res) => res.data)
}

export function getCurrentUser(): Promise<CurrentUser> {
  return http.get<CurrentUser>('/auth/auth/me').then((res) => res.data)
}

export function changePassword(data: ChangePasswordRequest): Promise<void> {
  return http.put('/auth/auth/me/password', data).then(() => undefined)
}
