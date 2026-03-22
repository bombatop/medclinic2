import { computed, onBeforeUnmount, ref, type ComputedRef, type Ref } from 'vue'
import { getAppointments, type Appointment, type AppointmentFilters } from '@/api/appointments'
import { getApiErrorMessage } from '@/utils/apiError'
import { isCanceledError } from '@/utils/isCanceledError'
import { TIME_SLOTS } from '@/views/appointments/appointmentConstants'
import { filterByParticipantSearch } from '@/views/appointments/appointmentHelpers'
import type { AppointmentViewMode } from '@/views/appointments/appointmentTypes'
import type { ToastServiceMethods } from 'primevue/toastservice'

function dayKey(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(
    d.getDate(),
  ).padStart(2, '0')}`
}

function cellKey(day: Date, slotStart: string): string {
  return `${dayKey(day)}|${slotStart}`
}

function slotFromDate(d: Date): string {
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = d.getMinutes() >= 30 ? '30' : '00'
  return `${hour}:${minute}`
}

export function useAppointmentTimetable(
  activeFilters: ComputedRef<AppointmentFilters>,
  viewMode: Ref<AppointmentViewMode>,
  filterDateFrom: Ref<Date | null>,
  filterDateTo: Ref<Date | null>,
  participantQueryDebounced: Ref<string>,
  toast: ToastServiceMethods,
) {
  const timetableAppointments = ref<Appointment[]>([])
  const timetableLoading = ref(false)
  let latestRequestId = 0
  let abortController: AbortController | null = null

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

  const filteredTimetableAppointments = computed(() =>
    filterByParticipantSearch(timetableAppointments.value, participantQueryDebounced.value.trim()),
  )

  const appointmentsByCell = computed(() => {
    const map = new Map<string, Appointment[]>()
    for (const appointment of filteredTimetableAppointments.value) {
      const start = new Date(appointment.startTime)
      const key = cellKey(start, slotFromDate(start))
      const list = map.get(key)
      if (list) list.push(appointment)
      else map.set(key, [appointment])
    }
    return map
  })

  function getAppointmentsForCell(day: Date, slotStart: string): Appointment[] {
    return appointmentsByCell.value.get(cellKey(day, slotStart)) ?? []
  }

  async function loadTimetableAppointments() {
    if (viewMode.value !== 'timetable') return
    abortController?.abort()
    abortController = new AbortController()
    const signal = abortController.signal
    const requestId = ++latestRequestId
    timetableLoading.value = true
    try {
      const res = await getAppointments(
        { page: 0, size: 500 },
        activeFilters.value,
        { signal },
      )
      if (requestId !== latestRequestId) return
      timetableAppointments.value = res.content
    } catch (err: unknown) {
      if (isCanceledError(err)) return
      if (requestId !== latestRequestId) return
      timetableAppointments.value = []
      toast.add({
        severity: 'error',
        summary: 'Load failed',
        detail: getApiErrorMessage(err, 'Unable to load timetable appointments.'),
      })
    } finally {
      if (requestId === latestRequestId) {
        timetableLoading.value = false
      }
    }
  }

  onBeforeUnmount(() => {
    abortController?.abort()
  })

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
