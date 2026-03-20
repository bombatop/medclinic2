import { ref } from 'vue'

export function useAppointmentViewMode() {
  const viewMode = ref<'table' | 'timetable'>('timetable')
  const timetableScope = ref<'day' | 'week'>('day')
  return { viewMode, timetableScope }
}
