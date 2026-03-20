/** Coalesces concurrent identical async work (e.g. parallel GETs with the same key). */
const inflight = new Map<string, Promise<unknown>>()

export function dedupePromise<T>(key: string, factory: () => Promise<T>): Promise<T> {
  const existing = inflight.get(key)
  if (existing) return existing as Promise<T>
  const p = factory().finally(() => {
    inflight.delete(key)
  })
  inflight.set(key, p)
  return p as Promise<T>
}
