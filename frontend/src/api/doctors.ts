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

export interface CreateDoctorForm {
  username: string
  password: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  specialization?: string
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

export async function createDoctorWithAccount(form: CreateDoctorForm): Promise<Doctor> {
  const user = await http
    .post<AuthUser>('/auth/auth/users', {
      username: form.username,
      password: form.password,
      firstName: form.firstName,
      lastName: form.lastName,
      email: form.email,
      phone: form.phone || null,
      roles: ['DOCTOR'],
    })
    .then((res) => res.data)

  return http
    .post<Doctor>('/main/employees', {
      authUserId: user.id,
      firstName: form.firstName,
      lastName: form.lastName,
      specialization: form.specialization || null,
    })
    .then((res) => res.data)
}

export function getAuthUser(userId: number): Promise<AuthUser> {
  return http.get<AuthUser>(`/auth/auth/users/${userId}`).then((res) => res.data)
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
