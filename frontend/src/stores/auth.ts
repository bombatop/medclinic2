import { ref, computed, readonly } from 'vue'
import { defineStore } from 'pinia'
import { logout } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  const isAuthenticated = computed(() => !!token.value)
  const role = computed(() => roles.value[0] ?? null)
  const isAdmin = computed(() => roles.value.includes('ADMIN'))
  const isDoctor = computed(() => roles.value.includes('DOCTOR'))
  const isReceptionist = computed(() => roles.value.includes('RECEPTIONIST'))
  const canAccessAdmin = computed(
    () => hasAnyPermission(['users.read_all', 'users.manage', 'users.manage_roles']),
  )
  const canManageRbac = computed(() => hasPermission('users.manage_roles'))

  function setTokens(access: string, userRoles: string[], userPermissions: string[] = []) {
    token.value = access
    roles.value = userRoles
    permissions.value = userPermissions
  }

  async function clearTokens() {
    try {
      await logout()
    } catch {
      // Ignore logout errors (e.g. already logged out)
    }
    token.value = null
    roles.value = []
    permissions.value = []
  }

  function hasPermission(permission: string): boolean {
    return permissions.value.includes(permission)
  }

  function hasAnyPermission(requiredPermissions: string[]): boolean {
    return requiredPermissions.some((permission) => permissions.value.includes(permission))
  }

  return {
    token: readonly(token),
    role,
    roles,
    permissions,
    isAuthenticated,
    isAdmin,
    isDoctor,
    isReceptionist,
    canAccessAdmin,
    canManageRbac,
    hasPermission,
    hasAnyPermission,
    setTokens,
    clearTokens,
  }
})
