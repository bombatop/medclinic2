<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  activateDoctor,
  createDoctorWithAccount,
  deactivateDoctor,
  getAuthUser,
  getDoctorAppointments,
  getDoctors,
  updateDoctor,
  type Doctor,
} from '@/api/doctors'
import type { Appointment } from '@/api/patients'
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
import Password from 'primevue/password'
import Tag from 'primevue/tag'

const toast = useToast()
const confirm = useConfirm()
const authStore = useAuthStore()
const canManageDoctors = computed(() => authStore.hasPermission('employee.manage'))

const doctors = ref<Doctor[]>([])
const totalRecords = ref(0)
const loading = ref(true)
const search = ref('')
const expandedRows = ref<Doctor[]>([])
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
const saving = ref(false)
const editingDoctorId = ref<number | null>(null)
const editLoginInfo = ref<{ username: string; email: string } | null>(null)
const loadingLoginInfo = ref(false)

const form = reactive({
  username: '',
  password: '',
  email: '',
  phone: '',
  firstName: '',
  lastName: '',
  specialization: '',
})

const filteredDoctors = computed(() => {
  const q = search.value.toLowerCase().trim()
  if (!q) return doctors.value
  return doctors.value.filter((d) => {
    const haystack =
      `${d.firstName} ${d.lastName} ${d.specialization ?? ''}`.toLowerCase()
    return haystack.includes(q)
  })
})

function getErrorMessage(err: unknown, fallback: string): string {
  const apiErr = err as { response?: { data?: { message?: string } }; message?: string }
  return apiErr.response?.data?.message ?? apiErr.message ?? fallback
}

async function loadDoctors() {
  loading.value = true
  try {
    const page = Math.floor(lazyParams.value.first / lazyParams.value.rows)
    const sortField = lazyParams.value.sortField
    const sortOrder = lazyParams.value.sortOrder
    const sort =
      sortField != null && sortOrder !== 0
        ? `${sortField},${sortOrder === 1 ? 'asc' : 'desc'}`
        : undefined
    const res = await getDoctors({ page, size: lazyParams.value.rows, sort })
    doctors.value = res.content
    totalRecords.value = res.totalElements
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Load failed',
      detail: getErrorMessage(err, 'Unable to load doctors.'),
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
  void loadDoctors()
}

function onSort(event: { sortField?: string; sortOrder?: number }) {
  lazyParams.value = {
    ...lazyParams.value,
    sortField: event.sortField ?? null,
    sortOrder: event.sortOrder ?? 0,
  }
  void loadDoctors()
}

function openCreateDialog() {
  dialogMode.value = 'create'
  editingDoctorId.value = null
  form.username = ''
  form.password = ''
  form.email = ''
  form.phone = ''
  form.firstName = ''
  form.lastName = ''
  form.specialization = ''
  dialogVisible.value = true
}

async function openEditDialog(doctor: Doctor) {
  dialogMode.value = 'edit'
  editingDoctorId.value = doctor.id
  editLoginInfo.value = null
  form.firstName = doctor.firstName
  form.lastName = doctor.lastName
  form.specialization = doctor.specialization ?? ''
  dialogVisible.value = true

  loadingLoginInfo.value = true
  try {
    const user = await getAuthUser(doctor.authUserId)
    editLoginInfo.value = { username: user.username, email: user.email }
  } catch {
    editLoginInfo.value = null
  } finally {
    loadingLoginInfo.value = false
  }
}

async function saveDoctor() {
  if (isBlankInput(form.firstName)) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'First name is required.' })
    return
  }
  if (isBlankInput(form.lastName)) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Last name is required.' })
    return
  }
  if (dialogMode.value === 'create') {
    if (isBlankInput(form.username)) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'Username is required.' })
      return
    }
    if (isBlankInput(form.password) || form.password.length < 6) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'Password must be at least 6 characters.' })
      return
    }
    if (isBlankInput(form.email)) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'Email is required.' })
      return
    }
  }

  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      const created = await createDoctorWithAccount({
        username: form.username.trim(),
        password: form.password,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || undefined,
        specialization: form.specialization.trim() || undefined,
      })
      totalRecords.value += 1
      doctors.value = [created, ...doctors.value].slice(0, lazyParams.value.rows)
      toast.add({
        severity: 'success',
        summary: 'Doctor created',
        detail: `Account and profile for ${created.firstName} ${created.lastName} created.`,
      })
    } else {
      const updated = await updateDoctor(editingDoctorId.value!, {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        specialization: form.specialization.trim(),
      })
      const idx = doctors.value.findIndex((d) => d.id === updated.id)
      if (idx >= 0) doctors.value[idx] = updated
      toast.add({
        severity: 'success',
        summary: 'Doctor updated',
        detail: `${updated.firstName} ${updated.lastName} saved.`,
      })
    }

    dialogVisible.value = false
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Save failed',
      detail: getErrorMessage(err, 'Unable to save doctor.'),
    })
  } finally {
    saving.value = false
  }
}

function confirmToggleActive(doctor: Doctor) {
  const action = doctor.active ? 'deactivate' : 'activate'
  confirm.require({
    message: `${doctor.active ? 'Deactivate' : 'Activate'} ${doctor.firstName} ${doctor.lastName}?`,
    header: `${doctor.active ? 'Deactivate' : 'Activate'} Doctor`,
    icon: doctor.active ? 'pi pi-ban' : 'pi pi-check-circle',
    rejectLabel: 'Cancel',
    acceptLabel: doctor.active ? 'Deactivate' : 'Activate',
    acceptClass: doctor.active ? 'p-button-danger' : 'p-button-success',
    accept: () => void performToggleActive(doctor, action),
  })
}

async function performToggleActive(doctor: Doctor, action: 'activate' | 'deactivate') {
  try {
    if (action === 'deactivate') {
      await deactivateDoctor(doctor.id)
    } else {
      await activateDoctor(doctor.id)
    }
    const idx = doctors.value.findIndex((d) => d.id === doctor.id)
    if (idx >= 0) doctors.value[idx] = { ...doctors.value[idx]!, active: action === 'activate' }
    toast.add({
      severity: 'success',
      summary: `Doctor ${action}d`,
      detail: `${doctor.firstName} ${doctor.lastName} ${action}d.`,
    })
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: `${action} failed`,
      detail: getErrorMessage(err, `Unable to ${action} doctor.`),
    })
  }
}

async function onRowExpand(event: { data: Doctor }) {
  const doctor = event.data
  if (appointmentsCache.value[doctor.id]) return

  loadingAppointments.value[doctor.id] = true
  try {
    appointmentsCache.value[doctor.id] = await getDoctorAppointments(doctor.id)
  } catch {
    appointmentsCache.value[doctor.id] = []
  } finally {
    loadingAppointments.value[doctor.id] = false
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
  void loadDoctors()
})
</script>

<template>
  <div class="doctors-page">
    <ConfirmDialog />

    <div class="page-header">
      <div>
        <h1>Doctors</h1>
        <p class="page-subtitle">Manage doctor profiles and schedules.</p>
      </div>
      <Button v-if="canManageDoctors" label="Add Doctor" icon="pi pi-plus" @click="openCreateDialog" />
    </div>

    <IconField class="search-field">
      <InputIcon class="pi pi-search" />
      <InputText v-model="search" placeholder="Search by name or specialization..." />
    </IconField>

    <DataTable
      v-model:expandedRows="expandedRows"
      :value="filteredDoctors"
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
        <div class="table-empty">No doctors found.</div>
      </template>

      <Column expander style="width: 3rem" />

      <Column header="Name" sortable sortField="lastName">
        <template #body="{ data }">
          {{ data.firstName }} {{ data.lastName }}
        </template>
      </Column>

      <Column field="specialization" header="Specialization" sortable sortField="specialization">
        <template #body="{ data }">
          {{ data.specialization || '—' }}
        </template>
      </Column>

      <Column header="Status" sortable sortField="active" style="width: 8rem">
        <template #body="{ data }">
          <Tag
            :value="data.active ? 'Active' : 'Inactive'"
            :severity="data.active ? 'success' : 'danger'"
          />
        </template>
      </Column>

      <Column header="Created" sortable sortField="createdAt">
        <template #body="{ data }">
          {{ formatDate(data.createdAt) }}
        </template>
      </Column>

      <Column v-if="canManageDoctors" style="width: 9rem">
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
              :icon="data.active ? 'pi pi-ban' : 'pi pi-check-circle'"
              :severity="data.active ? 'danger' : 'success'"
              text
              rounded
              size="small"
              v-tooltip.top="data.active ? 'Deactivate' : 'Activate'"
              @click="confirmToggleActive(data)"
            />
          </div>
        </template>
      </Column>

      <template #expansion="{ data: doctor }">
        <div class="expansion-content">
          <h3>Appointment Schedule</h3>

          <div v-if="loadingAppointments[doctor.id]" class="loading-state">
            <i class="pi pi-spin pi-spinner" />
            Loading appointments...
          </div>

          <DataTable
            v-else-if="appointmentsCache[doctor.id]?.length"
            :value="appointmentsCache[doctor.id]"
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
            <Column field="clientName" header="Patient" />
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

          <p v-else class="no-data">No appointments found for this doctor.</p>
        </div>
      </template>
    </DataTable>

    <Dialog
      v-model:visible="dialogVisible"
      :header="dialogMode === 'create' ? 'New Doctor' : 'Edit Doctor'"
      modal
      :style="{ width: '480px' }"
    >
      <div class="dialog-form">
        <template v-if="dialogMode === 'edit'">
          <div class="login-info">
            <div v-if="loadingLoginInfo" class="login-info-loading">
              <i class="pi pi-spin pi-spinner" /> Loading account info...
            </div>
            <template v-else-if="editLoginInfo">
              <div class="login-info-row">
                <span class="login-info-label">Login:</span>
                <span>{{ editLoginInfo.username }}</span>
              </div>
              <div class="login-info-row">
                <span class="login-info-label">Email:</span>
                <span>{{ editLoginInfo.email }}</span>
              </div>
            </template>
          </div>
        </template>
        <template v-if="dialogMode === 'create'">
          <p class="dialog-section">Account</p>
          <div class="field">
            <label for="dlg-username">Username *</label>
            <InputText id="dlg-username" v-model="form.username" :disabled="saving" />
          </div>
          <div class="field">
            <label for="dlg-password">Password *</label>
            <Password
              id="dlg-password"
              v-model="form.password"
              :disabled="saving"
              toggle-mask
              :feedback="false"
              input-class="w-full"
              class="w-full"
            />
          </div>
          <div class="field">
            <label for="dlg-email">Email *</label>
            <InputText id="dlg-email" v-model="form.email" :disabled="saving" />
          </div>
          <div class="field">
            <label for="dlg-phone">Phone</label>
            <InputText id="dlg-phone" v-model="form.phone" :disabled="saving" />
          </div>
          <p class="dialog-section">Profile</p>
        </template>
        <div class="field">
          <label for="dlg-firstName">First name *</label>
          <InputText id="dlg-firstName" v-model="form.firstName" :disabled="saving" />
        </div>
        <div class="field">
          <label for="dlg-lastName">Last name *</label>
          <InputText id="dlg-lastName" v-model="form.lastName" :disabled="saving" />
        </div>
        <div class="field">
          <label for="dlg-specialization">Specialization</label>
          <InputText
            id="dlg-specialization"
            v-model="form.specialization"
            :disabled="saving"
            placeholder="e.g. Cardiology, Pediatrics"
          />
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
          :label="dialogMode === 'create' ? 'Create' : 'Save'"
          icon="pi pi-check"
          :loading="saving"
          @click="saveDoctor"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.doctors-page {
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
.dialog-form .field :deep(.p-password) {
  width: 100%;
}

.login-info {
  background: var(--p-surface-100);
  border-radius: 8px;
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.login-info-loading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

.login-info-row {
  display: flex;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.login-info-label {
  color: var(--p-text-muted-color);
  min-width: 3rem;
}

.dialog-section {
  font-weight: 600;
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--p-text-muted-color);
  border-bottom: 1px solid var(--p-surface-border);
  padding-bottom: 0.375rem;
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
