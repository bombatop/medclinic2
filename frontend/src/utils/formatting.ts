import type { TagSeverity } from '@/utils/tagSeverity'

export function formatDate(value: string | Date): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value))
}

export function formatTime(value: string | Date): string {
  return new Intl.DateTimeFormat(undefined, { timeStyle: 'short' }).format(new Date(value))
}

/** Date + short time (e.g. audit log timestamps). */
export function formatDateTime(value: string | Date): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

/**
 * Maps appointment status strings to PrimeVue Tag severities.
 */
export function appointmentStatusSeverity(status: string): TagSeverity {
  const map: Record<string, TagSeverity> = {
    SCHEDULED: 'info',
    IN_PROGRESS: 'warn',
    COMPLETED: 'success',
    CANCELLED: 'danger',
  }
  return map[status] ?? 'secondary'
}

/** Local datetime string for API filters (same convention as appointment forms). */
export function toLocalDateTimeISO(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function toLocalDayStartISO(d: Date): string {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return toLocalDateTimeISO(x)
}

export function toLocalDayEndISO(d: Date): string {
  const x = new Date(d)
  x.setHours(23, 59, 59, 999)
  return toLocalDateTimeISO(x)
}
