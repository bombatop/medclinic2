import type { ComputedRef, Ref } from 'vue'
import {
  createAppointment,
  updateAppointment,
  updateAppointmentStatus,
  type Appointment,
} from '@/api/appointments'
import { getApiErrorMessage } from '@/utils/apiError'
import { toLocalDateTimeISO } from '@/utils/formatting'
import type { ToastServiceMethods } from 'primevue/toastservice'
import type {
  AppointmentFormState,
  AppointmentViewMode,
} from '@/views/appointments/appointmentTypes'
import { resetAppointmentForm } from '@/views/appointments/appointmentHelpers'

type LazyParams = { first: number; rows: number; sortField: string | null; sortOrder: number }

type AppointmentPayload = {
  employeeId: number
  clientId: number
  startTime: string
  endTime: string
  notes?: string
}

function validateAppointmentForm(
  form: AppointmentFormState,
  toast: ToastServiceMethods,
): AppointmentPayload | null {
  if (form.employeeId == null) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Doctor is required.' })
    return null
  }
  if (form.clientId == null) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Patient is required.' })
    return null
  }
  if (!form.startTime) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Start time is required.' })
    return null
  }
  if (!form.endTime) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'End time is required.' })
    return null
  }
  if (form.endTime.getTime() <= form.startTime.getTime()) {
    toast.add({
      severity: 'warn',
      summary: 'Validation',
      detail: 'End time must be after start time.',
    })
    return null
  }
  return {
    employeeId: form.employeeId,
    clientId: form.clientId,
    startTime: toLocalDateTimeISO(form.startTime),
    endTime: toLocalDateTimeISO(form.endTime),
    notes: form.notes.trim() || undefined,
  }
}

function upsertById(list: Appointment[], updated: Appointment): Appointment[] {
  const index = list.findIndex((item) => item.id === updated.id)
  if (index < 0) return list
  const next = [...list]
  next[index] = updated
  return next
}

export function useAppointmentMutations(deps: {
  toast: ToastServiceMethods
  canCreateAppointments: ComputedRef<boolean>
  canUpdateAppointments: ComputedRef<boolean>
  canUpdateAppointmentStatus: ComputedRef<boolean>
  form: AppointmentFormState
  editForm: AppointmentFormState
  appointments: Ref<Appointment[]>
  totalRecords: Ref<number>
  lazyParams: Ref<LazyParams>
  timetableAppointments: Ref<Appointment[]>
  viewMode: Ref<AppointmentViewMode>
  dialogVisible: Ref<boolean>
  viewingAppointment: Ref<Appointment | null>
  editingAppointmentId: Ref<number | null>
  saving: Ref<boolean>
}) {
  async function saveAppointment() {
    if (!deps.canCreateAppointments.value) {
      deps.toast.add({
        severity: 'warn',
        summary: 'Access denied',
        detail: 'You cannot create appointments.',
      })
      return
    }
    const payload = validateAppointmentForm(deps.form, deps.toast)
    if (!payload) return

    deps.saving.value = true
    try {
      const created = await createAppointment(payload)
      deps.totalRecords.value += 1
      deps.appointments.value = [created, ...deps.appointments.value].slice(0, deps.lazyParams.value.rows)
      if (deps.viewMode.value === 'timetable') {
        deps.timetableAppointments.value = [created, ...deps.timetableAppointments.value]
      }
      deps.toast.add({
        severity: 'success',
        summary: 'Appointment created',
        detail: `${created.clientName} with ${created.employeeName} scheduled.`,
      })
      deps.dialogVisible.value = false
      resetAppointmentForm(deps.form)
    } catch (err: unknown) {
      deps.toast.add({
        severity: 'error',
        summary: 'Create failed',
        detail: getApiErrorMessage(err, 'Unable to create appointment.'),
      })
    } finally {
      deps.saving.value = false
    }
  }

  async function saveEditAppointment() {
    if (!deps.canUpdateAppointments.value) {
      deps.toast.add({
        severity: 'warn',
        summary: 'Access denied',
        detail: 'You cannot update appointments.',
      })
      return
    }
    if (deps.editingAppointmentId.value == null) return
    const payload = validateAppointmentForm(deps.editForm, deps.toast)
    if (!payload) return

    deps.saving.value = true
    try {
      const updated = await updateAppointment(deps.editingAppointmentId.value, payload)
      deps.appointments.value = upsertById(deps.appointments.value, updated)
      deps.timetableAppointments.value = upsertById(deps.timetableAppointments.value, updated)
      deps.viewingAppointment.value = null
      deps.editingAppointmentId.value = null
      deps.toast.add({
        severity: 'success',
        summary: 'Appointment updated',
        detail: `${updated.clientName} with ${updated.employeeName} rescheduled.`,
      })
    } catch (err: unknown) {
      deps.toast.add({
        severity: 'error',
        summary: 'Update failed',
        detail: getApiErrorMessage(err, 'Unable to update appointment.'),
      })
    } finally {
      deps.saving.value = false
    }
  }

  async function changeStatus(apt: Appointment, status: Appointment['status']) {
    if (!deps.canUpdateAppointmentStatus.value) {
      deps.toast.add({
        severity: 'warn',
        summary: 'Access denied',
        detail: 'You cannot update appointment status.',
      })
      return
    }
    try {
      const updated = await updateAppointmentStatus(apt.id, status)
      deps.appointments.value = upsertById(deps.appointments.value, updated)
      deps.timetableAppointments.value = upsertById(deps.timetableAppointments.value, updated)
      if (deps.viewingAppointment.value?.id === updated.id) deps.viewingAppointment.value = updated
      deps.toast.add({
        severity: 'success',
        summary: 'Status updated',
        detail: `Appointment ${status.toLowerCase()}.`,
      })
    } catch (err: unknown) {
      deps.toast.add({
        severity: 'error',
        summary: 'Update failed',
        detail: getApiErrorMessage(err, 'Unable to update status.'),
      })
    }
  }

  return {
    saveAppointment,
    saveEditAppointment,
    changeStatus,
  }
}
