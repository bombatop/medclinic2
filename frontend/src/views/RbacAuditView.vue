<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  lazySortStateFromDataTable,
  pageFromLazyFirst,
  springSortFromPrime,
} from '@/composables/useLazyPrimeTable'
import { getRbacAuditLogs, type RbacAuditLog } from '@/api/rbac'
import { useToast } from 'primevue/usetoast'
import { getApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/formatting'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable, { type DataTableSortEvent } from 'primevue/datatable'
import InputText from 'primevue/inputtext'

const toast = useToast()

const rows = ref<RbacAuditLog[]>([])
const loading = ref(true)
const totalRecords = ref(0)
const lazyParams = ref({
  first: 0,
  rows: 20,
  sortField: 'createdAt' as string | null,
  sortOrder: -1 as number,
})

const filters = reactive({
  actorUsername: '',
  action: '',
})

async function loadAudit() {
  loading.value = true
  try {
    const page = pageFromLazyFirst(lazyParams.value.first, lazyParams.value.rows)
    const sort = springSortFromPrime(lazyParams.value.sortField, lazyParams.value.sortOrder)
    const res = await getRbacAuditLogs(
      { page, size: lazyParams.value.rows, sort },
      {
        actorUsername: filters.actorUsername.trim() || undefined,
        action: filters.action.trim() || undefined,
      },
    )
    rows.value = res.content
    totalRecords.value = res.totalElements
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Load failed',
      detail: getApiErrorMessage(err, 'Unable to load audit logs.'),
    })
  } finally {
    loading.value = false
  }
}

function onPage(event: { first: number; rows: number }) {
  lazyParams.value = {
    ...lazyParams.value,
    first: event.first,
    rows: event.rows,
  }
  void loadAudit()
}

function onSort(event: DataTableSortEvent) {
  const { sortField, sortOrder } = lazySortStateFromDataTable(event)
  lazyParams.value = {
    first: 0,
    rows: lazyParams.value.rows,
    sortField,
    sortOrder,
  }
  void loadAudit()
}

function applyFilters() {
  lazyParams.value.first = 0
  void loadAudit()
}

onMounted(() => {
  void loadAudit()
})
</script>

<template>
  <div class="mc-page rbac-audit-page">
    <div class="mc-page-header">
      <div>
        <h1>Audit</h1>
        <p class="mc-page-subtitle">Track role and permission changes.</p>
      </div>
    </div>

    <div class="filters">
      <div class="field">
        <label for="audit-actor">Actor username</label>
        <InputText id="audit-actor" v-model="filters.actorUsername" placeholder="e.g. admin" />
      </div>
      <div class="field">
        <label for="audit-action">Action</label>
        <InputText id="audit-action" v-model="filters.action" placeholder="e.g. ROLE_CREATED" />
      </div>
      <Button label="Apply" icon="pi pi-filter" @click="applyFilters" />
    </div>

    <DataTable
      class="audit-table"
      :pt="{ tableContainer: { class: 'audit-datatable-scroll' } }"
      :value="rows"
      :loading="loading"
      :lazy="true"
      :totalRecords="totalRecords"
      dataKey="id"
      paginator
      :rows="lazyParams.rows"
      :rowsPerPageOptions="[20, 50, 100]"
      :sortField="lazyParams.sortField ?? undefined"
      :sortOrder="lazyParams.sortOrder"
      stripedRows
      removableSort
      @page="onPage"
      @sort="onSort"
    >
      <template #empty>
        <div class="mc-table-empty">No audit events found.</div>
      </template>

      <Column field="createdAt" header="When" sortable sortField="createdAt">
        <template #body="{ data }">
          {{ formatDateTime(data.createdAt) }}
        </template>
      </Column>
      <Column field="actorUsername" header="Actor" sortable sortField="actorUsername" />
      <Column field="action" header="Action" sortable sortField="action" />
      <Column field="targetType" header="Target Type" />
      <Column field="targetRef" header="Target Ref" />
      <Column field="details" header="Details" />
    </DataTable>
  </div>
</template>

<style scoped>
.filters {
  display: flex;
  gap: 0.75rem;
  align-items: end;
  flex-wrap: wrap;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  min-width: 240px;
}

/* Long details: scroll the grid only (not the paginator); keep each cell on one line. */
.audit-datatable-scroll {
  max-width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.audit-table :deep(table) {
  width: max-content;
  min-width: 100%;
  table-layout: auto;
}

.audit-table :deep(thead th),
.audit-table :deep(tbody td) {
  white-space: nowrap;
  vertical-align: top;
}

</style>
