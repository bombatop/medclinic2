import http from './http'

export interface User {
  id: number
  username: string
  firstName: string
  lastName: string
  email: string
  phone: string | null
  role: 'ADMIN' | 'EMPLOYEE'
  active: boolean
  createdAt: string
}

export interface CreateUserRequest {
  username: string
  password: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  role: 'ADMIN' | 'EMPLOYEE'
}

export interface UpdateUserRequest {
  firstName?: string
  lastName?: string
  email?: string
  phone?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface PageParams {
  page?: number
  size?: number
}

export function getUsers(params?: PageParams): Promise<PageResponse<User>> {
  return http
    .get<PageResponse<User>>('/auth/auth/users', {
      params: { page: params?.page ?? 0, size: params?.size ?? 20 },
    })
    .then((res) => res.data)
}

export function createUser(data: CreateUserRequest): Promise<User> {
  return http.post<User>('/auth/auth/users', data).then((res) => res.data)
}

export function updateUser(id: number, data: UpdateUserRequest): Promise<User> {
  return http.put<User>(`/auth/auth/users/${id}`, data).then((res) => res.data)
}

export function activateUser(id: number): Promise<void> {
  return http.patch(`/auth/auth/users/${id}/activate`).then(() => undefined)
}

export function deactivateUser(id: number): Promise<void> {
  return http.patch(`/auth/auth/users/${id}/deactivate`).then(() => undefined)
}
