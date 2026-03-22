import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function useAppointmentPermissions() {
  const authStore = useAuthStore()

  const canCreateAppointments = computed(
    () =>
      authStore.hasPermission('appointment.create_any') ||
      (authStore.hasPermission('appointment.create_self') &&
        authStore.hasPermission('appointment.participate')),
  )

  const canUpdateAppointments = computed(
    () =>
      authStore.hasPermission('appointment.update_any') ||
      (authStore.hasPermission('appointment.update_self') &&
        authStore.hasPermission('appointment.participate')),
  )

  const canUpdateAppointmentStatus = computed(
    () =>
      authStore.hasPermission('appointment.status_update_any') ||
      authStore.hasPermission('appointment.status_update_self') ||
      authStore.hasPermission('appointment.cancel_any') ||
      authStore.hasPermission('appointment.cancel_self'),
  )

  return {
    canCreateAppointments,
    canUpdateAppointments,
    canUpdateAppointmentStatus,
  }
}
