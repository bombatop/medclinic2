<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createPatient,
  deletePatient,
  getPatientAppointments,
  getPatients,
  updatePatient,
  type Appointment,
  type Patient,
} from '@/api/patients'
import { isBlankInput } from '@/utils/validation'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import Button from 'primevue/button'
import Column from 'primevue/column'
import ConfirmDialog from 'primevue/confirmdialog'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'

const toast = useToast()
const confirm = useConfirm()

const patients = ref<Patient[]>([])
const totalRecords = ref(0)
const loading = ref(true)
const search = ref('')
const expandedRows = ref<Patient[]>([])
const lazyParams = ref({
  first: 0,
  rows: 10,
  sortField: 'lastName' as string | null,
  sortOrder: 1 as number,
})
const appointmentsCache = ref<Record<number, Appointment[]>>({})
const loadingAppointments = ref<Record<number, boolean>>({})

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const savingPatient = ref(false)
const editingPatientId = ref<number | null>(null)

const form = reactive({
  firstName: '',
  lastName: '',
  phone: '',
  email: '',
  notes: '',
})

const filteredPatients = computed(() => {
  const q = search.value.toLowerCase().trim()
  if (!q) return patients.value
  return patients.value.filter((p) => {
    const haystack = `${p.firstName} ${p.lastName} ${p.phone} ${p.email ?? ''}`.toLowerCase()
    return haystack.includes(q)
  })
})

function getErrorMessage(err: unknown, fallback: string): string {
  const apiErr = err as { response?: { data?: { message?: string } }; message?: string }
  return apiErr.response?.data?.message ?? apiErr.message ?? fallback
}

async function loadPatients() {
  loading.value = true
  try {
    const page = Math.floor(lazyParams.value.first / lazyParams.value.rows)
    const sortField = lazyParams.value.sortField
    const sortOrder = lazyParams.value.sortOrder
    const sort =
      sortField != null && sortOrder !== 0
        ? `${sortField},${sortOrder === 1 ? 'asc' : 'desc'}`
        : undefined
    const res = await getPatients({ page, size: lazyParams.value.rows, sort })
    patients.value = res.content
    totalRecords.value = res.totalElements
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Load failed',
      detail: getErrorMessage(err, 'Unable to load patients.'),
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
  void loadPatients()
}

function onSort(event: { sortField?: string; sortOrder?: number }) {
  lazyParams.value = {
    ...lazyParams.value,
    sortField: event.sortField ?? null,
    sortOrder: event.sortOrder ?? 0,
  }
  void loadPatients()
}

function openCreateDialog() {
  dialogMode.value = 'create'
  editingPatientId.value = null
  form.firstName = ''
  form.lastName = ''
  form.phone = ''
  form.email = ''
  form.notes = ''
  dialogVisible.value = true
}

function openEditDialog(patient: Patient) {
  dialogMode.value = 'edit'
  editingPatientId.value = patient.id
  form.firstName = patient.firstName
  form.lastName = patient.lastName
  form.phone = patient.phone
  form.email = patient.email ?? ''
  form.notes = patient.notes ?? ''
  dialogVisible.value = true
}

async function savePatient() {
  if (isBlankInput(form.firstName)) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'First name is required.' })
    return
  }
  if (isBlankInput(form.lastName)) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Last name is required.' })
    return
  }
  if (isBlankInput(form.phone)) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Phone is required.' })
    return
  }

  savingPatient.value = true
  try {
    const trimmed = {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      phone: form.phone.trim(),
      email: form.email.trim(),
      notes: form.notes.trim(),
    }

    if (dialogMode.value === 'create') {
      const data = {
        ...trimmed,
        email: trimmed.email || undefined,
        notes: trimmed.notes || undefined,
      }
      const created = await createPatient(data)
      totalRecords.value += 1
      patients.value = [created, ...patients.value].slice(0, lazyParams.value.rows)
      toast.add({
        severity: 'success',
        summary: 'Patient created',
        detail: `${created.firstName} ${created.lastName} added.`,
      })
    } else {
      const updated = await updatePatient(editingPatientId.value!, trimmed)
      const idx = patients.value.findIndex((p) => p.id === updated.id)
      if (idx >= 0) patients.value[idx] = updated
      toast.add({
        severity: 'success',
        summary: 'Patient updated',
        detail: `${updated.firstName} ${updated.lastName} saved.`,
      })
    }

    dialogVisible.value = false
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Save failed',
      detail: getErrorMessage(err, 'Unable to save patient.'),
    })
  } finally {
    savingPatient.value = false
  }
}

function confirmDelete(patient: Patient) {
  confirm.require({
    message: `Permanently delete ${patient.firstName} ${patient.lastName}? This cannot be undone.`,
    header: 'Delete Patient',
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Cancel',
    acceptLabel: 'Delete',
    acceptClass: 'p-button-danger',
    accept: () => void performDelete(patient),
  })
}

async function performDelete(patient: Patient) {
  try {
    await deletePatient(patient.id)
    patients.value = patients.value.filter((p) => p.id !== patient.id)
    totalRecords.value = Math.max(0, totalRecords.value - 1)
    delete appointmentsCache.value[patient.id]
    toast.add({
      severity: 'success',
      summary: 'Patient deleted',
      detail: `${patient.firstName} ${patient.lastName} removed.`,
    })
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Delete failed',
      detail: getErrorMessage(err, 'Unable to delete patient.'),
    })
  }
}

async function onRowExpand(event: { data: Patient }) {
  const patient = event.data
  if (appointmentsCache.value[patient.id]) return

  loadingAppointments.value[patient.id] = true
  try {
    appointmentsCache.value[patient.id] = await getPatientAppointments(patient.id)
  } catch {
    appointmentsCache.value[patient.id] = []
  } finally {
    loadingAppointments.value[patient.id] = false
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
  void loadPatients()
})
</script>

<template>
  <div class="patients-page">
    <ConfirmDialog />

    <div class="page-header">
      <div>
        <h1>Patients</h1>
        <p class="page-subtitle">Manage patient records and history.</p>
      </div>
      <Button label="Add Patient" icon="pi pi-plus" @click="openCreateDialog" />
    </div>

    <IconField class="search-field">
      <InputIcon class="pi pi-search" />
      <InputText v-model="search" placeholder="Search by name, phone, or email..." />
    </IconField>

    <DataTable
      v-model:expandedRows="expandedRows"
      :value="filteredPatients"
      :loading="loading"
      :lazy="true"
      :totalRecords="totalRecords"
      dataKey="id"
      paginator
      :rows="lazyParams.rows"
      :rowsPerPageOptions="[10, 25, 50]"
      stripedRows
      removableSort
      :sortField="lazyParams.sortField"
      :sortOrder="lazyParams.sortOrder"
      @row-expand="onRowExpand"
      @page="onPage"
      @sort="onSort"
    >
      <template #empty>
        <div class="table-empty">No patients found.</div>
      </template>

      <Column expander style="width: 3rem" />

      <Column header="Name" sortable sortField="lastName">
        <template #body="{ data }">
          {{ data.firstName }} {{ data.lastName }}
        </template>
      </Column>

      <Column field="phone" header="Phone" />

      <Column field="email" header="Email">
        <template #body="{ data }">
          {{ data.email || '—' }}
        </template>
      </Column>

      <Column header="Created" sortable sortField="createdAt">
        <template #body="{ data }">
          {{ formatDate(data.createdAt) }}
        </template>
      </Column>

      <Column style="width: 7rem">
        <template #body="{ data }">
          <div class="row-actions">
            <Button
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              size="small"
              v-tooltip.top="'Edit'"
              @click="openEditDialog(data)"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              size="small"
              v-tooltip.top="'Delete'"
              @click="confirmDelete(data)"
            />
          </div>
        </template>
      </Column>

      <template #expansion="{ data: patient }">
        <div class="expansion-content">
          <div v-if="patient.notes" class="patient-notes">
            <strong>Notes:</strong> {{ patient.notes }}
          </div>

          <h3>Appointment History</h3>

          <div v-if="loadingAppointments[patient.id]" class="loading-state">
            <i class="pi pi-spin pi-spinner" />
            Loading appointments...
          </div>

          <DataTable
            v-else-if="appointmentsCache[patient.id]?.length"
            :value="appointmentsCache[patient.id]"
            size="small"
          >
            <Column header="Date">
              <template #body="{ data: apt }">
                {{ formatDate(apt.startTime) }}
              </template>
            </Column>
            <Column header="Time">
              <template #body="{ data: apt }">
                {{ formatTime(apt.startTime) }}–{{ formatTime(apt.endTime) }}
              </template>
            </Column>
            <Column field="employeeName" header="Doctor" />
            <Column header="Status">
              <template #body="{ data: apt }">
                <Tag :value="apt.status" :severity="statusSeverity(apt.status)" />
              </template>
            </Column>
            <Column header="Notes">
              <template #body="{ data: apt }">
                {{ apt.notes || '—' }}
              </template>
            </Column>
          </DataTable>

          <p v-else class="no-data">No appointments found for this patient.</p>
        </div>
      </template>
    </DataTable>

    <Dialog
      v-model:visible="dialogVisible"
      :header="dialogMode === 'create' ? 'New Patient' : 'Edit Patient'"
      modal
      :style="{ width: '480px' }"
    >
      <div class="dialog-form">
        <div class="field">
          <label for="dlg-firstName">First name *</label>
          <InputText id="dlg-firstName" v-model="form.firstName" :disabled="savingPatient" />
        </div>
        <div class="field">
          <label for="dlg-lastName">Last name *</label>
          <InputText id="dlg-lastName" v-model="form.lastName" :disabled="savingPatient" />
        </div>
        <div class="field">
          <label for="dlg-phone">Phone *</label>
          <InputText id="dlg-phone" v-model="form.phone" :disabled="savingPatient" />
        </div>
        <div class="field">
          <label for="dlg-email">Email</label>
          <InputText id="dlg-email" v-model="form.email" :disabled="savingPatient" />
        </div>
        <div class="field">
          <label for="dlg-notes">Notes</label>
          <Textarea id="dlg-notes" v-model="form.notes" :disabled="savingPatient" rows="3" autoResize />
        </div>
      </div>

      <template #footer>
        <Button
          label="Cancel"
          severity="secondary"
          text
          :disabled="savingPatient"
          @click="dialogVisible = false"
        />
        <Button
          :label="dialogMode === 'create' ? 'Create' : 'Save'"
          icon="pi pi-check"
          :loading="savingPatient"
          @click="savePatient"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.patients-page {
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

.search-field :deep(.p-inputtext) {
  width: 100%;
  max-width: 400px;
}

.row-actions {
  display: flex;
  gap: 0.25rem;
}

.table-empty,
.no-data {
  text-align: center;
  padding: 2rem;
  color: var(--p-text-muted-color);
}

.expansion-content {
  padding: 1rem 2rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.expansion-content h3 {
  font-size: 1rem;
  font-weight: 600;
}

.patient-notes {
  color: var(--p-text-muted-color);
}

.loading-state {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--p-text-muted-color);
  padding: 1rem 0;
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
