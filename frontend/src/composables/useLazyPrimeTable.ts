import { watch, type Ref } from 'vue'
import type { DataTableSortEvent } from 'primevue/datatable'
import { useDebounceFn, DEFAULT_DEBOUNCE_MS } from '@/composables/useDebounceFn'

/** Map PrimeVue DataTable @sort event to lazy table state (sortField may be typed as string | function). */
export function lazySortStateFromDataTable(
  event: DataTableSortEvent,
): { sortField: string | null; sortOrder: number } {
  const raw = event.sortField
  const sortField = typeof raw === 'string' ? raw : null
  return {
    sortField,
    sortOrder: event.sortOrder ?? 0,
  }
}

export type LazyTableState = {
  first: number
  rows: number
  sortField: string | null
  sortOrder: number
}

export function pageFromLazyFirst(first: number, rows: number): number {
  return Math.floor(first / rows)
}

/** Build Spring Data `sort` query param from PrimeVue DataTable lazy sort event state. */
export function springSortFromPrime(sortField: string | null, sortOrder: number): string | undefined {
  if (sortField == null || sortOrder === 0) return undefined
  return `${sortField},${sortOrder === 1 ? 'asc' : 'desc'}`
}

/**
 * When search text changes: reset to first page and debounce-call `load`.
 */
export function useDebouncedSearchReload<T extends LazyTableState>(
  search: Ref<string>,
  lazyParams: Ref<T>,
  load: () => void,
  delayMs: number = DEFAULT_DEBOUNCE_MS,
): void {
  const debouncedLoad = useDebounceFn(load, delayMs)
  watch(search, () => {
    lazyParams.value = { ...lazyParams.value, first: 0 }
    debouncedLoad()
  })
}
