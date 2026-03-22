import { ref } from 'vue'
import type {
  AppointmentTimetableScope,
  AppointmentViewMode,
} from '@/views/appointments/appointmentTypes'

export function useAppointmentViewMode() {
  const viewMode = ref<AppointmentViewMode>('timetable')
  const timetableScope = ref<AppointmentTimetableScope>('day')
  return { viewMode, timetableScope }
}
