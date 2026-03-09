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

export interface CreateDoctorRequest {
  authUserId: number
  firstName: string
  lastName: string
  specialization?: string
}

export interface UpdateDoctorRequest {
  firstName?: string
  lastName?: string
  specialization?: string
}

export function getDoctors(): Promise<Doctor[]> {
  return http.get<Doctor[]>('/main/employees').then((res) => res.data)
}

export function createDoctor(data: CreateDoctorRequest): Promise<Doctor> {
  return http.post<Doctor>('/main/employees', data).then((res) => res.data)
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
