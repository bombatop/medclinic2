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
import type { AppointmentFormState } from '@/views/appointments/composables/useAppointmentForms'

type LazyParams = {
  first: number
  rows: number
  sortField: string | null
  sortOrder: number
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
  viewMode: Ref<'table' | 'timetable'>
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
    if (deps.form.employeeId == null) {
      deps.toast.add({ severity: 'warn', summary: 'Validation', detail: 'Doctor is required.' })
      return
    }
    if (deps.form.clientId == null) {
      deps.toast.add({ severity: 'warn', summary: 'Validation', detail: 'Patient is required.' })
      return
    }
    if (!deps.form.startTime) {
      deps.toast.add({ severity: 'warn', summary: 'Validation', detail: 'Start time is required.' })
      return
    }
    if (!deps.form.endTime) {
      deps.toast.add({ severity: 'warn', summary: 'Validation', detail: 'End time is required.' })
      return
    }
    if (deps.form.endTime.getTime() <= deps.form.startTime.getTime()) {
      deps.toast.add({
        severity: 'warn',
        summary: 'Validation',
        detail: 'End time must be after start time.',
      })
      return
    }

    deps.saving.value = true
    try {
      const created = await createAppointment({
        employeeId: deps.form.employeeId,
        clientId: deps.form.clientId,
        startTime: toLocalDateTimeISO(deps.form.startTime),
        endTime: toLocalDateTimeISO(deps.form.endTime),
        notes: deps.form.notes.trim() || undefined,
      })
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
    if (deps.editForm.employeeId == null) {
      deps.toast.add({ severity: 'warn', summary: 'Validation', detail: 'Doctor is required.' })
      return
    }
    if (deps.editForm.clientId == null) {
      deps.toast.add({ severity: 'warn', summary: 'Validation', detail: 'Patient is required.' })
      return
    }
    if (!deps.editForm.startTime) {
      deps.toast.add({ severity: 'warn', summary: 'Validation', detail: 'Start time is required.' })
      return
    }
    if (!deps.editForm.endTime) {
      deps.toast.add({ severity: 'warn', summary: 'Validation', detail: 'End time is required.' })
      return
    }
    if (deps.editForm.endTime.getTime() <= deps.editForm.startTime.getTime()) {
      deps.toast.add({
        severity: 'warn',
        summary: 'Validation',
        detail: 'End time must be after start time.',
      })
      return
    }

    deps.saving.value = true
    try {
      const updated = await updateAppointment(deps.editingAppointmentId.value, {
        employeeId: deps.editForm.employeeId,
        clientId: deps.editForm.clientId,
        startTime: toLocalDateTimeISO(deps.editForm.startTime),
        endTime: toLocalDateTimeISO(deps.editForm.endTime),
        notes: deps.editForm.notes.trim() || undefined,
      })
      const idx = deps.appointments.value.findIndex((a) => a.id === updated.id)
      if (idx >= 0) deps.appointments.value[idx] = updated
      const tidx = deps.timetableAppointments.value.findIndex((a) => a.id === updated.id)
      if (tidx >= 0) deps.timetableAppointments.value[tidx] = updated
      deps.viewingAppointment.value = updated
      deps.toast.add({
        severity: 'success',
        summary: 'Appointment updated',
        detail: `${updated.clientName} with ${updated.employeeName} rescheduled.`,
      })
      deps.viewingAppointment.value = null
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
      const idx = deps.appointments.value.findIndex((a) => a.id === updated.id)
      if (idx >= 0) deps.appointments.value[idx] = updated
      const tidx = deps.timetableAppointments.value.findIndex((a) => a.id === updated.id)
      if (tidx >= 0) deps.timetableAppointments.value[tidx] = updated
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
