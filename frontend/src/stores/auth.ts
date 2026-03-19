import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { logout } from '@/api/auth'

const TOKEN_KEY = 'token'
const ROLES_KEY = 'roles'
const PERMISSIONS_KEY = 'permissions'

function readStringArray(key: string): string[] {
  const raw = localStorage.getItem(key)
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.filter((v) => typeof v === 'string') : []
  } catch {
    return []
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const roles = ref<string[]>(readStringArray(ROLES_KEY))
  const permissions = ref<string[]>(readStringArray(PERMISSIONS_KEY))

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
    localStorage.setItem(TOKEN_KEY, access)
    localStorage.setItem(ROLES_KEY, JSON.stringify(userRoles))
    localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(userPermissions))
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
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(ROLES_KEY)
    localStorage.removeItem(PERMISSIONS_KEY)
  }

  function hasPermission(permission: string): boolean {
    return permissions.value.includes(permission)
  }

  function hasAnyPermission(requiredPermissions: string[]): boolean {
    return requiredPermissions.some((permission) => permissions.value.includes(permission))
  }

  return {
    token,
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
