import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const TOKEN_KEY = 'token'
const REFRESH_KEY = 'refreshToken'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_KEY))

  const isAuthenticated = computed(() => !!token.value)

  function setTokens(access: string, refresh: string) {
    token.value = access
    refreshToken.value = refresh
    localStorage.setItem(TOKEN_KEY, access)
    localStorage.setItem(REFRESH_KEY, refresh)
  }

  function clearTokens() {
    token.value = null
    refreshToken.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
  }

  return { token, refreshToken, isAuthenticated, setTokens, clearTokens }
})
