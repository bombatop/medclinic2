import { computed, ref, watch, type Ref } from 'vue'
import type { Appointment, AppointmentFilters } from '@/api/appointments'
import { toLocalDateTimeISO } from '@/utils/formatting'

export function useAppointmentFilters(
  viewMode: Ref<'table' | 'timetable'>,
  timetableScope: Ref<'day' | 'week'>,
) {
  const filterDoctor = ref<number | null>(null)
  const filterPatient = ref<number | null>(null)
  const filterStatus = ref<Appointment['status'] | null>(null)
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
    let from: string | undefined
    let to: string | undefined
    if (filterDateFrom.value) {
      const d = new Date(filterDateFrom.value)
      d.setHours(0, 0, 0, 0)
      from = toLocalDateTimeISO(d)
    }
    if (filterDateTo.value) {
      const d = new Date(filterDateTo.value)
      d.setHours(23, 59, 59, 999)
      to = toLocalDateTimeISO(d)
    }
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
    if (viewMode.value === 'timetable' && timetableScope.value === 'day') {
      filterDateTo.value = from ? new Date(from) : null
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
