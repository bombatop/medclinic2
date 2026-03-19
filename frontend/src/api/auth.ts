import axios from 'axios'
import http from './http'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export interface LoginRequest {
  username: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string | null
  roles: string[]
  permissions: string[]
  username: string
}

export interface CurrentUser {
  id: number
  username: string
  firstName: string
  lastName: string
  email: string
  phone: string | null
  roles: string[]
  active: boolean
  createdAt: string
}

export interface UpdateProfileRequest {
  firstName?: string
  lastName?: string
  email?: string
  phone?: string | null
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
  return http.post<AuthResponse>('/auth/auth/login', data, { withCredentials: true }).then((res) => res.data)
}

/** Uses raw axios to avoid interceptor recursion when called from 401 handler. Refresh token from httpOnly cookie. */
export function refresh(): Promise<AuthResponse> {
  return axios
    .post<AuthResponse>(`${baseURL}/auth/auth/refresh`, {}, { withCredentials: true })
    .then((res) => res.data)
}

export function logout(): Promise<void> {
  return axios
    .post(`${baseURL}/auth/auth/logout`, {}, { withCredentials: true })
    .then(() => undefined)
}

export function getCurrentUser(): Promise<CurrentUser> {
  return http.get<CurrentUser>('/auth/auth/me').then((res) => res.data)
}

export function updateProfile(data: UpdateProfileRequest): Promise<CurrentUser> {
  return http.put<CurrentUser>('/auth/auth/me', data).then((res) => res.data)
}

export function changePassword(data: ChangePasswordRequest): Promise<void> {
  return http.put('/auth/auth/me/password', data).then(() => undefined)
}
