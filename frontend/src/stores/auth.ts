import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const TOKEN_KEY = 'token'
const REFRESH_KEY = 'refreshToken'
const ROLE_KEY = 'role'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_KEY))
  const role = ref<string | null>(localStorage.getItem(ROLE_KEY))

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setTokens(access: string, refresh: string, userRole: string) {
    token.value = access
    refreshToken.value = refresh
    role.value = userRole
    localStorage.setItem(TOKEN_KEY, access)
    localStorage.setItem(REFRESH_KEY, refresh)
    localStorage.setItem(ROLE_KEY, userRole)
  }

  function clearTokens() {
    token.value = null
    refreshToken.value = null
    role.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(ROLE_KEY)
  }

  return { token, refreshToken, role, isAuthenticated, isAdmin, setTokens, clearTokens }
})
