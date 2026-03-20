import axios from 'axios'

/** True when the error is from an aborted Axios/fetch request. */
export function isCanceledError(err: unknown): boolean {
  if (axios.isCancel(err)) return true
  if (typeof err === 'object' && err !== null) {
    const o = err as { code?: string; name?: string }
    if (o.code === 'ERR_CANCELED' || o.name === 'CanceledError') return true
  }
  return false
}
