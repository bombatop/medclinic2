import { computed, ref, watch, type Ref } from 'vue'
import type { AppointmentFilters } from '@/api/appointments'
import { toLocalDayEndISO, toLocalDayStartISO } from '@/utils/formatting'
import type {
  AppointmentStatus,
  AppointmentTimetableScope,
  AppointmentViewMode,
} from '@/views/appointments/appointmentTypes'

export function useAppointmentFilters(
  viewMode: Ref<AppointmentViewMode>,
  timetableScope: Ref<AppointmentTimetableScope>,
) {
  const filterDoctor = ref<number | null>(null)
  const filterPatient = ref<number | null>(null)
  const filterStatus = ref<AppointmentStatus | null>(null)
  const filterDateFrom = ref<Date | null>(null)
  const filterDateTo = ref<Date | null>(null)

  function applyTimetableScopeToFilters() {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    if (timetableScope.value === 'day') {
      filterDateFrom.value = new Date(today)
      filterDateTo.value = new Date(today)
    } else {
      filterDateFrom.value = new Date(today)
      const end = new Date(today)
      end.setDate(end.getDate() + 7)
      filterDateTo.value = end
    }
  }

  const activeFilters = computed((): AppointmentFilters => {
    const from = filterDateFrom.value ? toLocalDayStartISO(filterDateFrom.value) : undefined
    const to = filterDateTo.value ? toLocalDayEndISO(filterDateTo.value) : undefined
    return {
      employeeId: filterDoctor.value ?? undefined,
      clientId: filterPatient.value ?? undefined,
      status: filterStatus.value ?? undefined,
      from,
      to,
    }
  })

  watch(viewMode, (mode) => {
    if (mode === 'timetable') applyTimetableScopeToFilters()
  })

  watch(filterDateFrom, (from) => {
    if (viewMode.value !== 'timetable') return
    if (!from) {
      filterDateTo.value = null
      return
    }
    if (timetableScope.value === 'day') {
      filterDateTo.value = new Date(from)
    } else {
      const end = new Date(from)
      end.setDate(end.getDate() + 7)
      filterDateTo.value = end
    }
  })

  return {
    filterDoctor,
    filterPatient,
    filterStatus,
    filterDateFrom,
    filterDateTo,
    activeFilters,
    applyTimetableScopeToFilters,
  }
}
