import { computed, ref, type ComputedRef, type Ref } from 'vue'
import { getAppointments, type Appointment, type AppointmentFilters } from '@/api/appointments'
import { TIME_SLOTS } from '@/views/appointments/appointmentConstants'

function filterBySearch<T extends { employeeName: string; clientName: string }>(
  list: T[],
  q: string,
): T[] {
  if (!q) return list
  const lower = q.toLowerCase()
  return list.filter((a) => {
    const haystack = `${a.employeeName} ${a.clientName}`.toLowerCase()
    return haystack.includes(lower)
  })
}

export function useAppointmentTimetable(
  activeFilters: ComputedRef<AppointmentFilters>,
  viewMode: Ref<'table' | 'timetable'>,
  filterDateFrom: Ref<Date | null>,
  filterDateTo: Ref<Date | null>,
  search: Ref<string>,
) {
  const timetableAppointments = ref<Appointment[]>([])
  const timetableLoading = ref(false)

  const filteredTimetableAppointments = computed(() =>
    filterBySearch(timetableAppointments.value, search.value.trim()),
  )

  const timetableFrom = computed(() => {
    if (filterDateFrom.value) {
      const d = new Date(filterDateFrom.value)
      d.setHours(0, 0, 0, 0)
      return d
    }
    const d = new Date()
    d.setHours(0, 0, 0, 0)
    return d
  })

  const timetableTo = computed(() => {
    if (filterDateTo.value) {
      const d = new Date(filterDateTo.value)
      d.setHours(23, 59, 59, 999)
      return d
    }
    const d = new Date(timetableFrom.value)
    d.setDate(d.getDate() + 1)
    return d
  })

  const timetableDays = computed(() => {
    const days: Date[] = []
    const start = new Date(timetableFrom.value)
    start.setHours(0, 0, 0, 0)
    const end = new Date(timetableTo.value)
    end.setHours(0, 0, 0, 0)
    for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
      days.push(new Date(d))
    }
    return days
  })

  function getAppointmentsForCell(day: Date, slotStart: string): Appointment[] {
    const parts = slotStart.split(':')
    const h = Number(parts[0])
    const m = Number(parts[1] ?? 0)
    if (!Number.isFinite(h) || !Number.isFinite(m)) return []
    const cellStart = new Date(day)
    cellStart.setHours(h, m, 0, 0)
    const cellEnd = new Date(cellStart)
    cellEnd.setMinutes(cellEnd.getMinutes() + 30)
    return filteredTimetableAppointments.value.filter((a) => {
      const start = new Date(a.startTime)
      return start >= cellStart && start < cellEnd
    })
  }

  async function loadTimetableAppointments() {
    if (viewMode.value !== 'timetable') return
    if (timetableLoading.value) return
    timetableLoading.value = true
    try {
      const res = await getAppointments(
        { page: 0, size: 500 },
        activeFilters.value,
      )
      timetableAppointments.value = res.content
    } catch {
      timetableAppointments.value = []
    } finally {
      timetableLoading.value = false
    }
  }

  return {
    timetableAppointments,
    timetableLoading,
    timetableFrom,
    timetableTo,
    timetableDays,
    timeSlots: TIME_SLOTS,
    getAppointmentsForCell,
    loadTimetableAppointments,
  }
}
