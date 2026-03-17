import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const TOKEN_KEY = 'token'
const REFRESH_KEY = 'refreshToken'
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
  const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_KEY))
  const roles = ref<string[]>(readStringArray(ROLES_KEY))
  const permissions = ref<string[]>(readStringArray(PERMISSIONS_KEY))

  const isAuthenticated = computed(() => !!token.value)
  const role = computed(() => roles.value[0] ?? null)
  const isAdmin = computed(() => roles.value.includes('ADMIN'))
  const isDoctor = computed(() => roles.value.includes('DOCTOR'))
  const isReceptionist = computed(() => roles.value.includes('RECEPTIONIST'))

  function setTokens(access: string, refresh: string, userRoles: string[], userPermissions: string[] = []) {
    token.value = access
    refreshToken.value = refresh
    roles.value = userRoles
    permissions.value = userPermissions
    localStorage.setItem(TOKEN_KEY, access)
    localStorage.setItem(REFRESH_KEY, refresh)
    localStorage.setItem(ROLES_KEY, JSON.stringify(userRoles))
    localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(userPermissions))
  }

  function clearTokens() {
    token.value = null
    refreshToken.value = null
    roles.value = []
    permissions.value = []
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(ROLES_KEY)
    localStorage.removeItem(PERMISSIONS_KEY)
  }

  function hasPermission(permission: string): boolean {
    return permissions.value.includes(permission)
  }

  return {
    token,
    refreshToken,
    role,
    roles,
    permissions,
    isAuthenticated,
    isAdmin,
    isDoctor,
    isReceptionist,
    hasPermission,
    setTokens,
    clearTokens,
  }
})
