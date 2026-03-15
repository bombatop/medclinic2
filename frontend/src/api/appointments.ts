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

export interface AppointmentFilters {
  employeeId?: number | null
  clientId?: number | null
  status?: Appointment['status'] | null
  from?: string | null
  to?: string | null
}

export function createAppointment(data: CreateAppointmentRequest): Promise<Appointment> {
  return http.post<Appointment>('/main/appointments', data).then((res) => res.data)
}

export function getAppointments(
  params?: PageParams,
  filters?: AppointmentFilters,
): Promise<PageResponse<Appointment>> {
  const query: Record<string, unknown> = {
    page: params?.page ?? 0,
    size: params?.size ?? 20,
  }
  if (params?.sort != null) query.sort = params.sort
  if (filters?.employeeId != null) query.employeeId = filters.employeeId
  if (filters?.clientId != null) query.clientId = filters.clientId
  if (filters?.status != null) query.status = filters.status
  if (filters?.from != null) query.from = filters.from
  if (filters?.to != null) query.to = filters.to
  return http.get<PageResponse<Appointment>>('/main/appointments', { params: query }).then((res) => res.data)
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

export function updateAppointment(
  id: number,
  data: CreateAppointmentRequest,
): Promise<Appointment> {
  return http.put<Appointment>(`/main/appointments/${id}`, data).then((res) => res.data)
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
