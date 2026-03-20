import http from './http'
import type { Appointment } from './patients'

export interface Doctor {
  id: number
  authUserId: number
  firstName: string
  lastName: string
  specialization: string | null
  active: boolean
  createdAt: string
}

export interface CreateEmployeeProfileRequest {
  authUserId: number
  firstName: string
  lastName: string
  specialization?: string | null
}

export interface UpdateDoctorRequest {
  firstName?: string
  lastName?: string
  specialization?: string
}

export interface AuthUser {
  id: number
  username: string
  email: string
  phone: string | null
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

export function getAuthUser(userId: number): Promise<AuthUser> {
  return http.get<AuthUser>(`/auth/auth/users/${userId}`).then((res) => res.data)
}

export function getLinkedAuthUserIds(): Promise<number[]> {
  return http.get<number[]>('/main/employees/linked-auth-user-ids').then((res) => res.data)
}

export function createEmployeeProfile(data: CreateEmployeeProfileRequest): Promise<Doctor> {
  return http
    .post<Doctor>('/main/employees', {
      authUserId: data.authUserId,
      firstName: data.firstName,
      lastName: data.lastName,
      specialization: data.specialization ?? null,
    })
    .then((res) => res.data)
}

export interface ListParams extends PageParams {
  search?: string
}

export function getDoctors(params?: ListParams): Promise<PageResponse<Doctor>> {
  const query: Record<string, unknown> = {
    page: params?.page ?? 0,
    size: params?.size ?? 20,
  }
  if (params?.sort != null) query.sort = params.sort
  if (params?.search?.trim()) query.search = params.search.trim()
  return http.get<PageResponse<Doctor>>('/main/employees', { params: query }).then((res) => res.data)
}

export function updateDoctor(id: number, data: UpdateDoctorRequest): Promise<Doctor> {
  return http.put<Doctor>(`/main/employees/${id}`, data).then((res) => res.data)
}

export function activateDoctor(id: number): Promise<void> {
  return http.patch(`/main/employees/${id}/activate`).then(() => undefined)
}

export function deactivateDoctor(id: number): Promise<void> {
  return http.patch(`/main/employees/${id}/deactivate`).then(() => undefined)
}

export function getDoctorAppointments(employeeId: number): Promise<Appointment[]> {
  return http
    .get<Appointment[]>(`/main/appointments/employee/${employeeId}`)
    .then((res) => res.data)
}
