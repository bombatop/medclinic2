import type { Appointment } from '@/api/appointments'

export type AppointmentStatus = Appointment['status']

export type AppointmentViewMode = 'table' | 'timetable'

export type AppointmentTimetableScope = 'day' | 'week'

export type AppointmentFormState = {
  employeeId: number | null
  clientId: number | null
  startTime: Date | null
  endTime: Date | null
  notes: string
}

export type DoctorSelectOption = { label: string; value: number }

export type PatientSelectOption = { label: string; value: number }
