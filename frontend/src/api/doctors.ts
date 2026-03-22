import http from './http'
import type { Appointment } from './appointments'
import { buildListQuery, type ListParams, type PageResponse } from './types/pagination'

export type { ListParams, PageResponse } from './types/pagination'

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

export function getDoctors(params?: ListParams): Promise<PageResponse<Doctor>> {
  return http
    .get<PageResponse<Doctor>>('/main/employees', { params: buildListQuery(params) })
    .then((res) => res.data)
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
