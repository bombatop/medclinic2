import http from './http'

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

export interface Appointment {
  id: number
  employeeId: number
  employeeName: string
  clientId: number
  clientName: string
  startTime: string
  endTime: string
  status: string
  notes: string | null
  createdAt: string
}

export function getPatients(params?: PageParams): Promise<PageResponse<Patient>> {
  const query: Record<string, unknown> = {
    page: params?.page ?? 0,
    size: params?.size ?? 20,
  }
  if (params?.sort != null) query.sort = params.sort
  return http.get<PageResponse<Patient>>('/main/clients', { params: query }).then((res) => res.data)
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
