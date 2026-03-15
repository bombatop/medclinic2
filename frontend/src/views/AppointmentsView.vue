<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createAppointment,
  getAppointments,
  updateAppointmentStatus,
  type Appointment,
} from '@/api/appointments'
import { getDoctors, type Doctor } from '@/api/doctors'
import { getPatients, type Patient } from '@/api/patients'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'

const toast = useToast()

const appointments = ref<Appointment[]>([])
const doctors = ref<Doctor[]>([])
const patients = ref<Patient[]>([])
const loading = ref(true)
const search = ref('')
const filterStatus = ref<string | null>(null)

const dialogVisible = ref(false)
const saving = ref(false)

const form = reactive({
  employeeId: null as number | null,
  clientId: null as number | null,
  startTime: null as Date | null,
  endTime: null as Date | null,
  notes: '',
})

const doctorOptions = computed(() =>
  doctors.value
    .filter((d) => d.active)
    .map((d) => ({
      label: `${d.firstName} ${d.lastName}${d.specialization ? ` (${d.specialization})` : ''}`,
      value: d.id,
    })),
)

const patientOptions = computed(() =>
  patients.value.map((p) => ({
    label: `${p.firstName} ${p.lastName}`,
    value: p.id,
  })),
)

const statusOptions = [
  { label: 'All', value: null },
  { label: 'Scheduled', value: 'SCHEDULED' },
  { label: 'In progress', value: 'IN_PROGRESS' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Cancelled', value: 'CANCELLED' },
]

const filteredAppointments = computed(() => {
  let list = appointments.value
  const q = search.value.toLowerCase().trim()
  if (q) {
    list = list.filter((a) => {
      const haystack = `${a.employeeName} ${a.clientName}`.toLowerCase()
      return haystack.includes(q)
    })
  }
  if (filterStatus.value) {
    list = list.filter((a) => a.status === filterStatus.value)
  }
  return list
})

function getErrorMessage(err: unknown, fallback: string): string {
  const apiErr = err as { response?: { data?: { message?: string } }; message?: string }
  return apiErr.response?.data?.message ?? apiErr.message ?? fallback
}

async function loadAppointments() {
  loading.value = true
  try {
    appointments.value = await getAppointments()
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Load failed',
      detail: getErrorMessage(err, 'Unable to load appointments.'),
    })
  } finally {
    loading.value = false
  }
}

async function loadDoctorsAndPatients() {
  try {
    const [docList, patList] = await Promise.all([getDoctors(), getPatients()])
    doctors.value = docList
    patients.value = patList
  } catch {
    doctors.value = []
    patients.value = []
  }
}

function openCreateDialog() {
  form.employeeId = null
  form.clientId = null
  form.startTime = null
  form.endTime = null
  form.notes = ''
  dialogVisible.value = true
}

function toLocalISOString(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

async function saveAppointment() {
  if (form.employeeId == null) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Doctor is required.' })
    return
  }
  if (form.clientId == null) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Patient is required.' })
    return
  }
  if (!form.startTime) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Start time is required.' })
    return
  }
  if (!form.endTime) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'End time is required.' })
    return
  }
  if (form.endTime <= form.startTime) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'End time must be after start time.' })
    return
  }

  saving.value = true
  try {
    const created = await createAppointment({
      employeeId: form.employeeId,
      clientId: form.clientId,
      startTime: toLocalISOString(form.startTime),
      endTime: toLocalISOString(form.endTime),
      notes: form.notes.trim() || undefined,
    })
    appointments.value.push(created)
    toast.add({
      severity: 'success',
      summary: 'Appointment created',
      detail: `${created.clientName} with ${created.employeeName} scheduled.`,
    })
    dialogVisible.value = false
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Create failed',
      detail: getErrorMessage(err, 'Unable to create appointment.'),
    })
  } finally {
    saving.value = false
  }
}

async function changeStatus(apt: Appointment, status: Appointment['status']) {
  try {
    const updated = await updateAppointmentStatus(apt.id, status)
    const idx = appointments.value.findIndex((a) => a.id === updated.id)
    if (idx >= 0) appointments.value[idx] = updated
    toast.add({
      severity: 'success',
      summary: 'Status updated',
      detail: `Appointment ${status.toLowerCase()}.`,
    })
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Update failed',
      detail: getErrorMessage(err, 'Unable to update status.'),
    })
  }
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value))
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat(undefined, { timeStyle: 'short' }).format(new Date(value))
}

function statusSeverity(status: string) {
  const map: Record<string, string> = {
    SCHEDULED: 'info',
    IN_PROGRESS: 'warn',
    COMPLETED: 'success',
    CANCELLED: 'danger',
  }
  return map[status] ?? 'secondary'
}

onMounted(() => {
  void loadAppointments()
  void loadDoctorsAndPatients()
})
</script>

<template>
  <div class="appointments-page">
    <div class="page-header">
      <div>
        <h1>Appointments</h1>
        <p class="page-subtitle">Schedule and manage appointments.</p>
      </div>
      <Button label="New Appointment" icon="pi pi-plus" @click="openCreateDialog" />
    </div>

    <div class="filters">
      <IconField class="search-field">
        <InputIcon class="pi pi-search" />
        <InputText v-model="search" placeholder="Search by patient or doctor..." />
      </IconField>
      <Select
        v-model="filterStatus"
        :options="statusOptions"
        optionLabel="label"
        optionValue="value"
        placeholder="Status"
        class="status-filter"
      />
    </div>

    <DataTable
      :value="filteredAppointments"
      :loading="loading"
      dataKey="id"
      paginator
      :rows="10"
      :rowsPerPageOptions="[10, 25, 50]"
      stripedRows
      removableSort
    >
      <template #empty>
        <div class="table-empty">No appointments found.</div>
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

      <Column field="clientName" header="Patient" sortable />

      <Column field="employeeName" header="Doctor" sortable />

      <Column header="Status" sortable sortField="status" style="width: 8rem">
        <template #body="{ data }">
          <Tag :value="data.status" :severity="statusSeverity(data.status)" />
        </template>
      </Column>

      <Column header="Notes">
        <template #body="{ data }">
          {{ data.notes || '—' }}
        </template>
      </Column>

      <Column v-if="filteredAppointments.some((a) => a.status !== 'CANCELLED' && a.status !== 'COMPLETED')" style="width: 10rem">
        <template #body="{ data }">
          <div v-if="data.status === 'SCHEDULED'" class="row-actions">
            <Button
              label="Start"
              size="small"
              severity="success"
              @click="changeStatus(data, 'IN_PROGRESS')"
            />
            <Button
              label="Cancel"
              size="small"
              severity="danger"
              text
              @click="changeStatus(data, 'CANCELLED')"
            />
          </div>
          <div v-else-if="data.status === 'IN_PROGRESS'" class="row-actions">
            <Button
              label="Complete"
              size="small"
              severity="success"
              @click="changeStatus(data, 'COMPLETED')"
            />
            <Button
              label="Cancel"
              size="small"
              severity="danger"
              text
              @click="changeStatus(data, 'CANCELLED')"
            />
          </div>
          <span v-else class="text-muted">—</span>
        </template>
      </Column>
    </DataTable>

    <Dialog
      v-model:visible="dialogVisible"
      header="New Appointment"
      modal
      :style="{ width: '480px' }"
    >
      <div class="dialog-form">
        <div class="field">
          <label for="dlg-doctor">Doctor *</label>
          <Select
            id="dlg-doctor"
            v-model="form.employeeId"
            :options="doctorOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Select doctor"
            :disabled="saving"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="dlg-patient">Patient *</label>
          <Select
            id="dlg-patient"
            v-model="form.clientId"
            :options="patientOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Select patient"
            :disabled="saving"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="dlg-start">Start time *</label>
          <DatePicker
            id="dlg-start"
            v-model="form.startTime"
            show-time
            hour-format="24"
            :disabled="saving"
            fluid
          />
        </div>
        <div class="field">
          <label for="dlg-end">End time *</label>
          <DatePicker
            id="dlg-end"
            v-model="form.endTime"
            show-time
            hour-format="24"
            :disabled="saving"
            fluid
          />
        </div>
        <div class="field">
          <label for="dlg-notes">Notes</label>
          <Textarea id="dlg-notes" v-model="form.notes" :disabled="saving" rows="3" autoResize />
        </div>
      </div>

      <template #footer>
        <Button
          label="Cancel"
          severity="secondary"
          text
          :disabled="saving"
          @click="dialogVisible = false"
        />
        <Button
          label="Create"
          icon="pi pi-check"
          :loading="saving"
          @click="saveAppointment"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.appointments-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.page-subtitle {
  color: var(--p-text-muted-color);
}

.filters {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.search-field :deep(.p-inputtext) {
  width: 100%;
  max-width: 300px;
}

.status-filter {
  min-width: 140px;
}

.row-actions {
  display: flex;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.text-muted {
  color: var(--p-text-muted-color);
}

.table-empty {
  text-align: center;
  padding: 2rem;
  color: var(--p-text-muted-color);
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.dialog-form .field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.dialog-form .field label {
  font-weight: 500;
}

.dialog-form .field :deep(.p-inputtext),
.dialog-form .field :deep(.p-textarea) {
  width: 100%;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .search-field :deep(.p-inputtext) {
    max-width: none;
  }
}
</style>
