import http from './http'
import type { Appointment } from './appointments'
import { buildListQuery, type ListParams, type PageResponse } from './types/pagination'

export type { Appointment } from './appointments'
export type { ListParams, PageResponse } from './types/pagination'

export interface Patient {
  id: number
  firstName: string
  lastName: string
  phone: string
  email: string | null
  notes: string | null
  createdAt: string
}

export interface CreatePatientRequest {
  firstName: string
  lastName: string
  phone: string
  email?: string
  notes?: string
}

export interface UpdatePatientRequest {
  firstName?: string
  lastName?: string
  phone?: string
  email?: string
  notes?: string
}

export function getPatients(params?: ListParams): Promise<PageResponse<Patient>> {
  return http
    .get<PageResponse<Patient>>('/main/clients', { params: buildListQuery(params) })
    .then((res) => res.data)
}

export function createPatient(data: CreatePatientRequest): Promise<Patient> {
  return http.post<Patient>('/main/clients', data).then((res) => res.data)
}

export function updatePatient(id: number, data: UpdatePatientRequest): Promise<Patient> {
  return http.put<Patient>(`/main/clients/${id}`, data).then((res) => res.data)
}

export function deletePatient(id: number): Promise<void> {
  return http.delete(`/main/clients/${id}`).then(() => undefined)
}

export function getPatientAppointments(clientId: number): Promise<Appointment[]> {
  return http.get<Appointment[]>(`/main/appointments/client/${clientId}`).then((res) => res.data)
}
