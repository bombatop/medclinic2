import http from './http'
import type { PageResponse, PageParams } from './users'

export interface Role {
  id: number
  code: string
  name: string
  description: string | null
  active: boolean
  system: boolean
  createdAt: string
}

export interface Permission {
  id: number
  code: string
  name: string
  description: string | null
  active: boolean
  system: boolean
  createdAt: string
}

export interface CreateRoleRequest {
  code: string
  name: string
  description?: string
  active?: boolean
}

export interface UpdateRoleRequest {
  name: string
  description?: string
  active?: boolean
}

export interface RolePermissionsResponse {
  roleId: number
  roleCode: string
  permissions: string[]
}

export interface RbacAuditLog {
  id: number
  actorUserId: number
  actorUsername: string
  action: string
  targetType: string
  targetRef: string | null
  details: string
  createdAt: string
}

export interface AuditFilters {
  actorUsername?: string
  action?: string
  from?: string
  to?: string
}

export function getRoles(params?: PageParams): Promise<PageResponse<Role>> {
  return http
    .get<PageResponse<Role>>('/auth/auth/rbac/roles', {
      params: { page: params?.page ?? 0, size: params?.size ?? 20 },
    })
    .then((res) => res.data)
}

export function getRole(id: number): Promise<Role> {
  return http.get<Role>(`/auth/auth/rbac/roles/${id}`).then((res) => res.data)
}

export function createRole(data: CreateRoleRequest): Promise<Role> {
  return http.post<Role>('/auth/auth/rbac/roles', data).then((res) => res.data)
}

export function updateRole(id: number, data: UpdateRoleRequest): Promise<Role> {
  return http.put<Role>(`/auth/auth/rbac/roles/${id}`, data).then((res) => res.data)
}

export function deleteRole(id: number): Promise<void> {
  return http.delete(`/auth/auth/rbac/roles/${id}`).then(() => undefined)
}

export function getPermissions(): Promise<Permission[]> {
  return http.get<Permission[]>('/auth/auth/rbac/permissions').then((res) => res.data)
}

export function getRolePermissions(roleId: number): Promise<RolePermissionsResponse> {
  return http
    .get<RolePermissionsResponse>(`/auth/auth/rbac/roles/${roleId}/permissions`)
    .then((res) => res.data)
}

export function updateRolePermissions(roleId: number, permissions: string[]): Promise<RolePermissionsResponse> {
  return http
    .put<RolePermissionsResponse>(`/auth/auth/rbac/roles/${roleId}/permissions`, { permissions })
    .then((res) => res.data)
}

export function getRbacAuditLogs(
  params?: PageParams,
  filters?: AuditFilters,
): Promise<PageResponse<RbacAuditLog>> {
  const query: Record<string, unknown> = {
    page: params?.page ?? 0,
    size: params?.size ?? 20,
  }
  if (filters?.actorUsername) query.actorUsername = filters.actorUsername
  if (filters?.action) query.action = filters.action
  if (filters?.from) query.from = filters.from
  if (filters?.to) query.to = filters.to
  return http.get<PageResponse<RbacAuditLog>>('/auth/auth/rbac/audit', { params: query }).then((res) => res.data)
}
