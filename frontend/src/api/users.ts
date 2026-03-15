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

export function getUsers(): Promise<User[]> {
  return http.get<User[]>('/auth/auth/users').then((res) => res.data)
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
