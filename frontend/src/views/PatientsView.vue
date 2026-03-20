<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  lazySortStateFromDataTable,
  pageFromLazyFirst,
  springSortFromPrime,
  useDebouncedSearchReload,
} from '@/composables/useLazyPrimeTable'
import {
  createPatient,
  deletePatient,
  getPatientAppointments,
  getPatients,
  updatePatient,
  type Appointment,
  type Patient,
} from '@/api/patients'
import { DIALOG_WIDTH_DEFAULT } from '@/constants/ui'
import { getApiErrorMessage } from '@/utils/apiError'
import { appointmentStatusSeverity, formatDate, formatTime } from '@/utils/formatting'
import { useReferenceDataStore } from '@/stores/referenceData'
import { isBlankInput } from '@/utils/validation'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import Button from 'primevue/button'
import Column from 'primevue/column'
import ConfirmDialog from 'primevue/confirmdialog'
import DataTable from 'primevue/datatable'
import type { DataTableSortEvent } from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'

const toast = useToast()
const confirm = useConfirm()
const referenceDataStore = useReferenceDataStore()

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

async function loadPatients() {
  loading.value = true
  try {
    const page = pageFromLazyFirst(lazyParams.value.first, lazyParams.value.rows)
    const sort = springSortFromPrime(lazyParams.value.sortField, lazyParams.value.sortOrder)
    const res = await getPatients({
      page,
      size: lazyParams.value.rows,
      sort,
      search: search.value.trim() || undefined,
    })
    patients.value = res.content
    totalRecords.value = res.totalElements
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Load failed',
      detail: getApiErrorMessage(err, 'Unable to load patients.'),
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

function onSort(event: DataTableSortEvent) {
  const { sortField, sortOrder } = lazySortStateFromDataTable(event)
  lazyParams.value = {
    first: 0,
    rows: lazyParams.value.rows,
    sortField,
    sortOrder,
  }
  void loadPatients()
}

useDebouncedSearchReload(search, lazyParams, loadPatients)

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
      referenceDataStore.invalidateDoctorsPatients()
    } else {
      const updated = await updatePatient(editingPatientId.value!, trimmed)
      const idx = patients.value.findIndex((p) => p.id === updated.id)
      if (idx >= 0) patients.value[idx] = updated
      toast.add({
        severity: 'success',
        summary: 'Patient updated',
        detail: `${updated.firstName} ${updated.lastName} saved.`,
      })
      referenceDataStore.invalidateDoctorsPatients()
    }

    dialogVisible.value = false
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Save failed',
      detail: getApiErrorMessage(err, 'Unable to save patient.'),
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
    referenceDataStore.invalidateDoctorsPatients()
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Delete failed',
      detail: getApiErrorMessage(err, 'Unable to delete patient.'),
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

onMounted(() => {
  void loadPatients()
})
</script>

<template>
  <div class="mc-page patients-page">
    <ConfirmDialog />

    <div class="mc-page-header">
      <div>
        <h1>Patients</h1>
        <p class="mc-page-subtitle">Manage patient records and history.</p>
      </div>
      <Button label="Add Patient" icon="pi pi-plus" @click="openCreateDialog" />
    </div>

    <IconField class="mc-search-field">
      <InputIcon class="pi pi-search" />
      <InputText v-model="search" placeholder="Search by name, phone, or email..." />
    </IconField>

    <DataTable
      v-model:expandedRows="expandedRows"
      :value="patients"
      :loading="loading"
      :lazy="true"
      :totalRecords="totalRecords"
      dataKey="id"
      paginator
      :rows="lazyParams.rows"
      :rowsPerPageOptions="[10, 25, 50]"
      stripedRows
      removableSort
      :sortField="lazyParams.sortField ?? undefined"
      :sortOrder="lazyParams.sortOrder"
      @row-expand="onRowExpand"
      @page="onPage"
      @sort="onSort"
    >
      <template #empty>
        <div class="mc-table-empty">No patients found.</div>
      </template>

      <Column expander style="width: 3rem" />

      <Column header="Name" sortable sortField="lastName">
        <template #body="{ data }">
          {{ data.firstName }} {{ data.lastName }}
        </template>
      </Column>

      <Column field="phone" header="Phone" sortable sortField="phone" />

      <Column field="email" header="Email" sortable sortField="email">
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
          <div class="mc-row-actions">
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
        <div class="mc-expansion-content">
          <div v-if="patient.notes" class="patient-notes">
            <strong>Notes:</strong> {{ patient.notes }}
          </div>

          <h3>Appointment History</h3>

          <div v-if="loadingAppointments[patient.id]" class="mc-loading-state">
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
                <Tag :value="apt.status" :severity="appointmentStatusSeverity(apt.status)" />
              </template>
            </Column>
            <Column header="Notes">
              <template #body="{ data: apt }">
                {{ apt.notes || '—' }}
              </template>
            </Column>
          </DataTable>

          <p v-else class="mc-no-data">No appointments found for this patient.</p>
        </div>
      </template>
    </DataTable>

    <Dialog
      v-model:visible="dialogVisible"
      :header="dialogMode === 'create' ? 'New Patient' : 'Edit Patient'"
      modal
      :style="{ width: DIALOG_WIDTH_DEFAULT }"
    >
      <div class="mc-dialog-form dialog-form">
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
.patient-notes {
  color: var(--p-text-muted-color);
}

.mc-expansion-content h3 {
  font-size: 1rem;
  font-weight: 600;
}

.dialog-form .field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.dialog-form .field label {
  font-weight: 500;
}
</style>
