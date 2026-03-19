import http from './http'

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
  sort?: string
}

export interface ListParams extends PageParams {
  search?: string
}

export function getUsers(params?: ListParams): Promise<PageResponse<User>> {
  const query: Record<string, unknown> = {
    page: params?.page ?? 0,
    size: params?.size ?? 20,
  }
  if (params?.sort) query.sort = params.sort
  if (params?.search?.trim()) query.search = params.search.trim()
  return http.get<PageResponse<User>>('/auth/auth/users', { params: query }).then((res) => res.data)
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
