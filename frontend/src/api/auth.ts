import http from './http'

export interface LoginRequest {
  username: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  role: string
  username: string
}

export interface ApiError {
  status?: number
  message?: string
}

export function login(data: LoginRequest): Promise<AuthResponse> {
  return http.post<AuthResponse>('/auth/auth/login', data).then((res) => res.data)
}
