import type { RouteLocationRaw } from 'vue-router'

const DEFAULT_FALLBACK: RouteLocationRaw = { path: '/' }

/**
 * Returns a safe in-app redirect target. Rejects external URLs and protocol-relative paths.
 */
export function safeRedirectAfterLogin(raw: unknown): RouteLocationRaw {
  if (typeof raw !== 'string' || raw.length === 0) return DEFAULT_FALLBACK
  const trimmed = raw.trim()
  if (!trimmed.startsWith('/') || trimmed.startsWith('//')) return DEFAULT_FALLBACK
  if (/[\r\n]/.test(trimmed)) return DEFAULT_FALLBACK
  if (/^[a-zA-Z][a-zA-Z\d+.-]*:/.test(trimmed)) return DEFAULT_FALLBACK
  return { path: trimmed }
}
