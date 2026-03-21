import { useAuthStore } from '@/stores/auth'
import { refresh } from '@/api/auth'

/**
 * Rehydrate session from HttpOnly refresh cookie (no access token in storage).
 * Must run after Pinia is installed on the app.
 */
export async function bootstrapAuth(): Promise<void> {
  const auth = useAuthStore()
  if (auth.isAuthenticated) {
    return
  }

  try {
    const res = await refresh()
    auth.setTokens(res.accessToken, res.roles, res.permissions)
  } catch {
    // No valid refresh cookie — stay logged out
  }
}
