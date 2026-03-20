import { ref, type ComputedRef } from 'vue'
import type { DataTableSortEvent } from 'primevue/datatable'
import {
  lazySortStateFromDataTable,
  pageFromLazyFirst,
  springSortFromPrime,
} from '@/composables/useLazyPrimeTable'
import { getAppointments, type Appointment, type AppointmentFilters } from '@/api/appointments'
import { getApiErrorMessage } from '@/utils/apiError'
import type { ToastServiceMethods } from 'primevue/toastservice'

export function useAppointmentTableLoad(
  activeFilters: ComputedRef<AppointmentFilters>,
  toast: ToastServiceMethods,
) {
  const appointments = ref<Appointment[]>([])
  const totalRecords = ref(0)
  const loading = ref(true)
  const lazyParams = ref({
    first: 0,
    rows: 10,
    sortField: 'startTime' as string | null,
    sortOrder: -1 as number,
  })

  async function loadAppointments() {
    loading.value = true
    try {
      const page = pageFromLazyFirst(lazyParams.value.first, lazyParams.value.rows)
      const sortField = lazyParams.value.sortField
      const sortOrder = lazyParams.value.sortOrder
      const backendSortField =
        sortField === 'clientName'
          ? 'client.lastName'
          : sortField === 'employeeName'
            ? 'employee.lastName'
            : sortField
      const sort = springSortFromPrime(backendSortField, sortOrder)
      const res = await getAppointments(
        { page, size: lazyParams.value.rows, sort },
        activeFilters.value,
      )
      appointments.value = res.content
      totalRecords.value = res.totalElements
    } catch (err: unknown) {
      toast.add({
        severity: 'error',
        summary: 'Load failed',
        detail: getApiErrorMessage(err, 'Unable to load appointments.'),
      })
    } finally {
      loading.value = false
    }
  }

  function onPage(event: { first: number; rows: number }) {
    lazyParams.value = {
      first: event.first,
      rows: event.rows,
      sortField: lazyParams.value.sortField,
      sortOrder: lazyParams.value.sortOrder,
    }
    void loadAppointments()
  }

  function onSort(event: DataTableSortEvent) {
    const { sortField, sortOrder } = lazySortStateFromDataTable(event)
    lazyParams.value = {
      first: 0,
      rows: lazyParams.value.rows,
      sortField,
      sortOrder,
    }
    void loadAppointments()
  }

  return {
    appointments,
    totalRecords,
    loading,
    lazyParams,
    loadAppointments,
    onPage,
    onSort,
  }
}
