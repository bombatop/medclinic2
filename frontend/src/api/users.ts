import http from './http'
import { buildListQuery, type ListParams, type PageResponse } from './types/pagination'

export type { ListParams, PageResponse } from './types/pagination'

export interface User {
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

export interface CreateUserRequest {
  username: string
  password: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  roles: string[]
}

export interface UpdateUserRequest {
  firstName?: string
  lastName?: string
  email?: string
  phone?: string
}

export interface UserRolesResponse {
  userId: number
  username: string
  roles: string[]
}

export function getUsers(params?: ListParams): Promise<PageResponse<User>> {
  return http
    .get<PageResponse<User>>('/auth/auth/users', { params: buildListQuery(params) })
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

export function getUserRoles(id: number): Promise<UserRolesResponse> {
  return http.get<UserRolesResponse>(`/auth/auth/users/${id}/roles`).then((res) => res.data)
}

export function updateUserRoles(id: number, roles: string[]): Promise<UserRolesResponse> {
  return http.put<UserRolesResponse>(`/auth/auth/users/${id}/roles`, { roles }).then((res) => res.data)
}
