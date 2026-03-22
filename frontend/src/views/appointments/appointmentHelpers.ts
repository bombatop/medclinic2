import type { AppointmentFormState } from '@/views/appointments/appointmentTypes'

export function filterByParticipantSearch<T extends { employeeName: string; clientName: string }>(
  list: T[],
  query: string,
): T[] {
  if (!query) return list
  const lower = query.toLowerCase()
  return list.filter((item) => {
    const haystack = `${item.employeeName} ${item.clientName}`.toLowerCase()
    return haystack.includes(lower)
  })
}

export function formatDuration(minutes: number): string {
  if (minutes < 60) return `${minutes} min`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m === 0 ? `${h} h` : `${h} h ${m} min`
}

export function applyDurationPreset(
  startTime: Date | null,
  setEndTime: (d: Date | null) => void,
  minutes: number,
) {
  if (!startTime) return
  const end = new Date(startTime)
  end.setMinutes(end.getMinutes() + minutes)
  setEndTime(end)
}

export function resetAppointmentForm(form: AppointmentFormState): void {
  form.employeeId = null
  form.clientId = null
  form.startTime = null
  form.endTime = null
  form.notes = ''
}
