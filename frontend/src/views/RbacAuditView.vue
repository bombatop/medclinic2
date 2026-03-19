<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getRbacAuditLogs, type RbacAuditLog } from '@/api/rbac'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'

const toast = useToast()

const rows = ref<RbacAuditLog[]>([])
const loading = ref(true)
const totalRecords = ref(0)
const lazyParams = ref({ first: 0, rows: 20 })

const filters = reactive({
  actorUsername: '',
  action: '',
})

function getErrorMessage(err: unknown, fallback: string): string {
  const apiErr = err as { response?: { data?: { message?: string } }; message?: string }
  return apiErr.response?.data?.message ?? apiErr.message ?? fallback
}

async function loadAudit() {
  loading.value = true
  try {
    const page = Math.floor(lazyParams.value.first / lazyParams.value.rows)
    const res = await getRbacAuditLogs(
      { page, size: lazyParams.value.rows },
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
      detail: getErrorMessage(err, 'Unable to load RBAC audit logs.'),
    })
  } finally {
    loading.value = false
  }
}

function onPage(event: { first: number; rows: number }) {
  lazyParams.value = { first: event.first, rows: event.rows }
  void loadAudit()
}

function applyFilters() {
  lazyParams.value.first = 0
  void loadAudit()
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

onMounted(() => {
  void loadAudit()
})
</script>

<template>
  <div class="rbac-audit-page">
    <div class="page-header">
      <div>
        <h1>RBAC Audit</h1>
        <p class="page-subtitle">Track role and permission changes.</p>
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
      :value="rows"
      :loading="loading"
      :lazy="true"
      :totalRecords="totalRecords"
      dataKey="id"
      paginator
      :rows="lazyParams.rows"
      :rowsPerPageOptions="[20, 50, 100]"
      stripedRows
      @page="onPage"
    >
      <template #empty>
        <div class="table-empty">No audit events found.</div>
      </template>

      <Column field="createdAt" header="When">
        <template #body="{ data }">
          {{ formatDate(data.createdAt) }}
        </template>
      </Column>
      <Column field="actorUsername" header="Actor" />
      <Column field="action" header="Action" />
      <Column field="targetType" header="Target Type" />
      <Column field="targetRef" header="Target Ref" />
      <Column field="details" header="Details" />
    </DataTable>
  </div>
</template>

<style scoped>
.rbac-audit-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-subtitle {
  color: var(--p-text-muted-color);
}

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

.table-empty {
  text-align: center;
  padding: 2rem;
  color: var(--p-text-muted-color);
}
</style>
