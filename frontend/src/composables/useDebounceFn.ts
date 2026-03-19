import { onUnmounted } from 'vue'

/**
 * Returns a debounced version of the given function.
 * Cancels pending invocations when called again before delay elapses.
 * Cleans up on component unmount.
 *
 * @param fn - Function to debounce
 * @param delayMs - Delay in milliseconds
 * @returns Debounced function
 */
export function useDebounceFn<T extends (...args: unknown[]) => unknown>(
  fn: T,
  delayMs: number,
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
