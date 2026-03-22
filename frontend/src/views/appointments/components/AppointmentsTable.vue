<script setup lang="ts">
import { computed } from 'vue'
import type { Appointment } from '@/api/appointments'
import { appointmentStatusSeverity, formatDate, formatTime } from '@/utils/formatting'
import type { LazyTableState } from '@/composables/useLazyPrimeTable'
import type { DataTablePageEvent, DataTableSortEvent } from 'primevue/datatable'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Tag from 'primevue/tag'

const props = defineProps<{
  rows: Appointment[]
  totalRecords: number
  loading: boolean
  lazyParams: LazyTableState
  canUpdateAppointments: boolean
  canUpdateAppointmentStatus: boolean
}>()

const emit = defineEmits<{
  page: [event: DataTablePageEvent]
  sort: [event: DataTableSortEvent]
  edit: [appointment: Appointment]
  'change-status': [appointment: Appointment, status: Appointment['status']]
}>()

const canShowStatusActions = computed(
  () =>
    props.canUpdateAppointmentStatus &&
    props.rows.some((a) => a.status !== 'CANCELLED' && a.status !== 'COMPLETED'),
)
</script>

<template>
  <div class="table-view">
    <DataTable
      :value="rows"
      :loading="loading"
      :lazy="true"
      :totalRecords="totalRecords"
      dataKey="id"
      paginator
      :rows="lazyParams.rows"
      :rowsPerPageOptions="[10, 25, 50]"
      :sortField="lazyParams.sortField ?? undefined"
      :sortOrder="lazyParams.sortOrder"
      stripedRows
      removableSort
      @page="emit('page', $event)"
      @sort="emit('sort', $event)"
    >
      <template #empty>
        <div class="mc-table-empty">No appointments found.</div>
      </template>

      <Column header="Date" sortable sortField="startTime">
        <template #body="{ data }">
          {{ formatDate(data.startTime) }}
        </template>
      </Column>

      <Column header="Time" sortable sortField="startTime">
        <template #body="{ data }">
          {{ formatTime(data.startTime) }}–{{ formatTime(data.endTime) }}
        </template>
      </Column>

      <Column field="clientName" header="Patient" sortable sortField="clientName" />

      <Column field="employeeName" header="Doctor" sortable sortField="employeeName" />

      <Column header="Status" sortable sortField="status" style="width: 8rem">
        <template #body="{ data }">
          <Tag :value="data.status" :severity="appointmentStatusSeverity(data.status)" />
        </template>
      </Column>

      <Column header="Notes">
        <template #body="{ data }">
          {{ data.notes || '—' }}
        </template>
      </Column>

      <Column v-if="canUpdateAppointments" header="" style="width: 4rem">
        <template #body="{ data }">
          <Button
            icon="pi pi-pencil"
            severity="secondary"
            text
            size="small"
            aria-label="Edit"
            @click="emit('edit', data)"
          />
        </template>
      </Column>

      <Column
        v-if="canShowStatusActions"
        style="width: 10rem"
      >
        <template #body="{ data }">
          <div v-if="data.status === 'SCHEDULED'" class="mc-row-actions">
            <Button
              label="Start"
              size="small"
              severity="success"
              @click="emit('change-status', data, 'IN_PROGRESS')"
            />
            <Button
              label="Cancel"
              size="small"
              severity="danger"
              text
              @click="emit('change-status', data, 'CANCELLED')"
            />
          </div>
          <div v-else-if="data.status === 'IN_PROGRESS'" class="mc-row-actions">
            <Button
              label="Complete"
              size="small"
              severity="success"
              @click="emit('change-status', data, 'COMPLETED')"
            />
            <Button
              label="Cancel"
              size="small"
              severity="danger"
              text
              @click="emit('change-status', data, 'CANCELLED')"
            />
          </div>
          <span v-else class="mc-text-muted">—</span>
        </template>
      </Column>
    </DataTable>
  </div>
</template>
