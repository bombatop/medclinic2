import http from './http'

export interface Appointment {
  id: number
  employeeId: number
  employeeName: string
  clientId: number
  clientName: string
  startTime: string
  endTime: string
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
  notes: string | null
  createdAt: string
}

export interface CreateAppointmentRequest {
  employeeId: number
  clientId: number
  startTime: string
  endTime: string
  notes?: string
}

export function createAppointment(data: CreateAppointmentRequest): Promise<Appointment> {
  return http.post<Appointment>('/main/appointments', data).then((res) => res.data)
}

export function getAppointments(): Promise<Appointment[]> {
  return http.get<Appointment[]>('/main/appointments').then((res) => res.data)
}

export function getAppointment(id: number): Promise<Appointment> {
  return http.get<Appointment>(`/main/appointments/${id}`).then((res) => res.data)
}

export function getAppointmentsByEmployee(employeeId: number): Promise<Appointment[]> {
  return http
    .get<Appointment[]>(`/main/appointments/employee/${employeeId}`)
    .then((res) => res.data)
}

export function getAppointmentsByClient(clientId: number): Promise<Appointment[]> {
  return http
    .get<Appointment[]>(`/main/appointments/client/${clientId}`)
    .then((res) => res.data)
}

export function updateAppointmentStatus(
  id: number,
  status: Appointment['status'],
): Promise<Appointment> {
  return http
    .patch<Appointment>(`/main/appointments/${id}/status`, null, {
      params: { status },
    })
    .then((res) => res.data)
}
