import { onUnmounted } from 'vue'

/** Default debounce delay for search/filter and lazy loads (ms). */
export const DEFAULT_DEBOUNCE_MS = 350

/**
 * Returns a debounced version of the given function.
 * Cancels pending invocations when called again before delay elapses.
 * Cleans up on component unmount.
 *
 * @param fn - Function to debounce
 * @param delayMs - Delay in milliseconds (defaults to {@link DEFAULT_DEBOUNCE_MS})
 * @returns Debounced function
 */
export function useDebounceFn<T extends (...args: unknown[]) => unknown>(
  fn: T,
  delayMs: number = DEFAULT_DEBOUNCE_MS,
): T {
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  const debouncedFn = ((...args: unknown[]) => {
    if (timeoutId != null) {
      clearTimeout(timeoutId)
    }
    timeoutId = setTimeout(() => {
      timeoutId = null
      fn(...args)
    }, delayMs)
  }) as T

  onUnmounted(() => {
    if (timeoutId != null) {
      clearTimeout(timeoutId)
      timeoutId = null
    }
  })

  return debouncedFn
}
