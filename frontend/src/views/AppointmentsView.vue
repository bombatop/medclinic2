<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  pageFromLazyFirst,
  springSortFromPrime,
} from '@/composables/useLazyPrimeTable'
import {
  createAppointment,
  getAppointments,
  updateAppointment,
  updateAppointmentStatus,
  type Appointment,
} from '@/api/appointments'
import { getDoctors, type Doctor } from '@/api/doctors'
import { getPatients, type Patient } from '@/api/patients'
import { DIALOG_WIDTH_DEFAULT } from '@/constants/ui'
import { useAuthStore } from '@/stores/auth'
import { getApiErrorMessage } from '@/utils/apiError'
import { appointmentStatusSeverity, formatDate, formatTime } from '@/utils/formatting'
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
const authStore = useAuthStore()

const canCreateAppointments = computed(
  () =>
    authStore.hasPermission('appointment.create_any') ||
    (authStore.hasPermission('appointment.create_self') &&
      authStore.hasPermission('appointment.participate')),
)

const canUpdateAppointments = computed(
  () =>
    authStore.hasPermission('appointment.update_any') ||
    (authStore.hasPermission('appointment.update_self') &&
      authStore.hasPermission('appointment.participate')),
)

const canUpdateAppointmentStatus = computed(
  () =>
    authStore.hasPermission('appointment.status_update_any') ||
    authStore.hasPermission('appointment.status_update_self') ||
    authStore.hasPermission('appointment.cancel_any') ||
    authStore.hasPermission('appointment.cancel_self'),
)

const appointments = ref<Appointment[]>([])
const totalRecords = ref(0)
const doctors = ref<Doctor[]>([])
const patients = ref<Patient[]>([])
const loading = ref(true)
const lazyParams = ref({
  first: 0,
  rows: 10,
  sortField: 'startTime' as string | null,
  sortOrder: -1 as number,
})
const search = ref('')
const filterDoctor = ref<number | null>(null)
const filterPatient = ref<number | null>(null)
const filterStatus = ref<string | null>(null)
const filterDateFrom = ref<Date | null>(null)
const filterDateTo = ref<Date | null>(null)

const viewMode = ref<'table' | 'timetable'>('timetable')
const timetableScope = ref<'day' | 'week'>('day')
const timetableAppointments = ref<Appointment[]>([])
const timetableLoading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const viewingAppointment = ref<Appointment | null>(null)
const editingAppointmentId = ref<number | null>(null)
const editForm = reactive({
  employeeId: null as number | null,
  clientId: null as number | null,
  startTime: null as Date | null,
  endTime: null as Date | null,
  notes: '',
})

const DURATION_PRESETS = [
  { label: '15 min', value: 15 },
  { label: '30 min', value: 30 },
  { label: '45 min', value: 45 },
  { label: '1 hour', value: 60 },
  { label: '1.5 hours', value: 90 },
  { label: '2 hours', value: 120 },
]

const form = reactive({
  employeeId: null as number | null,
  clientId: null as number | null,
  startTime: null as Date | null,
  endTime: null as Date | null,
  notes: '',
})

function formatDuration(minutes: number): string {
  if (minutes < 60) return `${minutes} min`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m === 0 ? `${h} h` : `${h} h ${m} min`
}

const formDurationMinutes = computed(() => {
  if (!form.startTime || !form.endTime) return null
  return Math.round((form.endTime.getTime() - form.startTime.getTime()) / 60000)
})

const editFormDurationMinutes = computed(() => {
  if (!editForm.startTime || !editForm.endTime) return null
  return Math.round((editForm.endTime.getTime() - editForm.startTime.getTime()) / 60000)
})

const formEndTimeModel = computed({
  get: () => form.endTime,
  set: (v: Date | null) => {
    if (!v) {
      form.endTime = null
      return
    }
    if (!form.startTime) {
      form.endTime = v
      return
    }
    const end = new Date(form.startTime)
    end.setHours(v.getHours(), v.getMinutes(), 0, 0)
    form.endTime = end
  },
})

const editFormEndTimeModel = computed({
  get: () => editForm.endTime,
  set: (v: Date | null) => {
    if (!v) {
      editForm.endTime = null
      return
    }
    if (!editForm.startTime) {
      editForm.endTime = v
      return
    }
    const end = new Date(editForm.startTime)
    end.setHours(v.getHours(), v.getMinutes(), 0, 0)
    editForm.endTime = end
  },
})

function applyDurationPreset(
  startTime: Date | null,
  setEndTime: (d: Date | null) => void,
  minutes: number,
) {
  if (!startTime) return
  const end = new Date(startTime)
  end.setMinutes(end.getMinutes() + minutes)
  setEndTime(end)
}

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
  { label: 'Scheduled', value: 'SCHEDULED' },
  { label: 'In progress', value: 'IN_PROGRESS' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Cancelled', value: 'CANCELLED' },
]

function filterBySearch<T extends { employeeName: string; clientName: string }>(
  list: T[],
  q: string,
): T[] {
  if (!q) return list
  const lower = q.toLowerCase()
  return list.filter((a) => {
    const haystack = `${a.employeeName} ${a.clientName}`.toLowerCase()
    return haystack.includes(lower)
  })
}

const filteredAppointments = computed(() =>
  filterBySearch(appointments.value, search.value.trim()),
)

const filteredTimetableAppointments = computed(() =>
  filterBySearch(timetableAppointments.value, search.value.trim()),
)

function toLocalISO(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const activeFilters = computed(() => {
  let from: string | undefined
  let to: string | undefined
  if (filterDateFrom.value) {
    const d = new Date(filterDateFrom.value)
    d.setHours(0, 0, 0, 0)
    from = toLocalISO(d)
  }
  if (filterDateTo.value) {
    const d = new Date(filterDateTo.value)
    d.setHours(23, 59, 59, 999)
    to = toLocalISO(d)
  }
  return {
    employeeId: filterDoctor.value ?? undefined,
    clientId: filterPatient.value ?? undefined,
    status: filterStatus.value ?? undefined,
    from,
    to,
  }
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

function onSort(event: { sortField?: string; sortOrder?: number }) {
  lazyParams.value = {
    first: 0,
    rows: lazyParams.value.rows,
    sortField: event.sortField ?? null,
    sortOrder: event.sortOrder ?? 0,
  }
  void loadAppointments()
}

function reloadCurrentView() {
  lazyParams.value = {
    first: 0,
    rows: lazyParams.value.rows,
    sortField: lazyParams.value.sortField,
    sortOrder: lazyParams.value.sortOrder,
  }
  void loadForCurrentView()
}

function applyTimetableScopeToFilters() {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  if (timetableScope.value === 'day') {
    filterDateFrom.value = new Date(today)
    filterDateTo.value = new Date(today)
  } else {
    filterDateFrom.value = new Date(today)
    const end = new Date(today)
    end.setDate(end.getDate() + 7)
    filterDateTo.value = end
  }
}

watch(viewMode, (mode) => {
  if (mode === 'timetable') applyTimetableScopeToFilters()
})

watch(filterDateFrom, (from) => {
  if (viewMode.value === 'timetable' && timetableScope.value === 'day') {
    filterDateTo.value = from ? new Date(from) : null
  }
})

watch(
  [viewMode, timetableScope, filterDoctor, filterPatient, filterStatus, filterDateFrom, filterDateTo],
  reloadCurrentView,
)

watch(
  () => form.startTime,
  (start, prevStart) => {
    if (!start) return
    if (!form.endTime) {
      const end = new Date(start)
      end.setMinutes(end.getMinutes() + 30)
      form.endTime = end
      return
    }
    if (prevStart && form.endTime.getTime() <= start.getTime()) {
      const durationMin = Math.round((form.endTime.getTime() - prevStart.getTime()) / 60000)
      const end = new Date(start)
      end.setMinutes(end.getMinutes() + Math.max(5, durationMin))
      form.endTime = end
    }
  },
)

watch(
  () => editForm.startTime,
  (start, prevStart) => {
    if (!start) return
    if (!editForm.endTime) {
      const end = new Date(start)
      end.setMinutes(end.getMinutes() + 30)
      editForm.endTime = end
      return
    }
    if (prevStart && editForm.endTime.getTime() <= start.getTime()) {
      const durationMin = Math.max(5, Math.round((editForm.endTime.getTime() - prevStart.getTime()) / 60000))
      const end = new Date(start)
      end.setMinutes(end.getMinutes() + durationMin)
      editForm.endTime = end
    }
  },
)

const timetableFrom = computed(() => {
  if (filterDateFrom.value) {
    const d = new Date(filterDateFrom.value)
    d.setHours(0, 0, 0, 0)
    return d
  }
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d
})

const timetableTo = computed(() => {
  if (filterDateTo.value) {
    const d = new Date(filterDateTo.value)
    d.setHours(23, 59, 59, 999)
    return d
  }
  const d = new Date(timetableFrom.value)
  d.setDate(d.getDate() + 1)
  return d
})

const timetableDays = computed(() => {
  const days: Date[] = []
  const start = new Date(timetableFrom.value)
  start.setHours(0, 0, 0, 0)
  const end = new Date(timetableTo.value)
  end.setHours(0, 0, 0, 0)
  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    days.push(new Date(d))
  }
  return days
})

const TIME_SLOTS = (() => {
  const slots: string[] = []
  for (let h = 7; h <= 19; h++) {
    slots.push(`${h.toString().padStart(2, '0')}:00`)
    slots.push(`${h.toString().padStart(2, '0')}:30`)
  }
  return slots
})()

const detailsModalOpen = computed({
  get: () => viewingAppointment.value != null,
  set: (v) => {
    if (!v) viewingAppointment.value = null
  },
})

function openDetailsModal(apt: Appointment) {
  viewingAppointment.value = apt
  editingAppointmentId.value = apt.id
  editForm.employeeId = apt.employeeId
  editForm.clientId = apt.clientId
  editForm.startTime = new Date(apt.startTime)
  editForm.endTime = new Date(apt.endTime)
  editForm.notes = apt.notes ?? ''
}

function closeDetailsModal() {
  viewingAppointment.value = null
  editingAppointmentId.value = null
}

function handleTimetableBlockClick(apt: Appointment) {
  openDetailsModal(apt)
}

function getAppointmentsForCell(day: Date, slotStart: string): Appointment[] {
  const [h, m] = slotStart.split(':').map(Number)
  const cellStart = new Date(day)
  cellStart.setHours(h, m, 0, 0)
  const cellEnd = new Date(cellStart)
  cellEnd.setMinutes(cellEnd.getMinutes() + 30)
  return filteredTimetableAppointments.value.filter((a) => {
    const start = new Date(a.startTime)
    return start >= cellStart && start < cellEnd
  })
}

async function loadTimetableAppointments() {
  if (viewMode.value !== 'timetable') return
  if (timetableLoading.value) return
  timetableLoading.value = true
  try {
    const res = await getAppointments(
      { page: 0, size: 500 },
      activeFilters.value,
    )
    timetableAppointments.value = res.content
  } catch {
    timetableAppointments.value = []
  } finally {
    timetableLoading.value = false
  }
}

function loadForCurrentView() {
  if (viewMode.value === 'timetable') void loadTimetableAppointments()
  else void loadAppointments()
}

async function loadDoctorsAndPatients() {
  try {
    const [docRes, patRes] = await Promise.all([
      getDoctors({ page: 0, size: 500 }),
      getPatients({ page: 0, size: 500 }),
    ])
    doctors.value = docRes.content
    patients.value = patRes.content
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
  if (!canCreateAppointments.value) {
    toast.add({ severity: 'warn', summary: 'Access denied', detail: 'You cannot create appointments.' })
    return
  }
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
  if (form.endTime.getTime() <= form.startTime.getTime()) {
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
    totalRecords.value += 1
    appointments.value = [created, ...appointments.value].slice(0, lazyParams.value.rows)
    if (viewMode.value === 'timetable') {
      timetableAppointments.value = [created, ...timetableAppointments.value]
    }
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
      detail: getApiErrorMessage(err, 'Unable to create appointment.'),
    })
  } finally {
    saving.value = false
  }
}

async function saveEditAppointment() {
  if (!canUpdateAppointments.value) {
    toast.add({ severity: 'warn', summary: 'Access denied', detail: 'You cannot update appointments.' })
    return
  }
  if (editingAppointmentId.value == null) return
  if (editForm.employeeId == null) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Doctor is required.' })
    return
  }
  if (editForm.clientId == null) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Patient is required.' })
    return
  }
  if (!editForm.startTime) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Start time is required.' })
    return
  }
  if (!editForm.endTime) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'End time is required.' })
    return
  }
  if (editForm.endTime.getTime() <= editForm.startTime.getTime()) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'End time must be after start time.' })
    return
  }

  saving.value = true
  try {
    const updated = await updateAppointment(editingAppointmentId.value, {
      employeeId: editForm.employeeId,
      clientId: editForm.clientId,
      startTime: toLocalISOString(editForm.startTime),
      endTime: toLocalISOString(editForm.endTime),
      notes: editForm.notes.trim() || undefined,
    })
    const idx = appointments.value.findIndex((a) => a.id === updated.id)
    if (idx >= 0) appointments.value[idx] = updated
    const tidx = timetableAppointments.value.findIndex((a) => a.id === updated.id)
    if (tidx >= 0) timetableAppointments.value[tidx] = updated
    viewingAppointment.value = updated
    toast.add({
      severity: 'success',
      summary: 'Appointment updated',
      detail: `${updated.clientName} with ${updated.employeeName} rescheduled.`,
    })
    viewingAppointment.value = null
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Update failed',
      detail: getApiErrorMessage(err, 'Unable to update appointment.'),
    })
  } finally {
    saving.value = false
  }
}

async function changeStatus(apt: Appointment, status: Appointment['status']) {
  if (!canUpdateAppointmentStatus.value) {
    toast.add({ severity: 'warn', summary: 'Access denied', detail: 'You cannot update appointment status.' })
    return
  }
  try {
    const updated = await updateAppointmentStatus(apt.id, status)
    const idx = appointments.value.findIndex((a) => a.id === updated.id)
    if (idx >= 0) appointments.value[idx] = updated
    const tidx = timetableAppointments.value.findIndex((a) => a.id === updated.id)
    if (tidx >= 0) timetableAppointments.value[tidx] = updated
    if (viewingAppointment.value?.id === updated.id) viewingAppointment.value = updated
    toast.add({
      severity: 'success',
      summary: 'Status updated',
      detail: `Appointment ${status.toLowerCase()}.`,
    })
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Update failed',
      detail: getApiErrorMessage(err, 'Unable to update status.'),
    })
  }
}

onMounted(() => {
  void loadDoctorsAndPatients()
  if (viewMode.value === 'timetable') {
    applyTimetableScopeToFilters()
  } else {
    reloadCurrentView()
  }
})
</script>

<template>
  <div class="mc-page appointments-page">
    <div class="mc-page-header">
      <div>
        <h1>Appointments</h1>
        <p class="mc-page-subtitle">Schedule and manage appointments.</p>
      </div>
      <Button
        v-if="canCreateAppointments"
        label="New Appointment"
        icon="pi pi-plus"
        @click="openCreateDialog"
      />
    </div>

    <div class="filters">
      <Select
        v-model="filterDoctor"
        :options="doctorOptions"
        optionLabel="label"
        optionValue="value"
        placeholder="Doctor"
        showClear
        class="filter-select"
      />
      <Select
        v-model="filterPatient"
        :options="patientOptions"
        optionLabel="label"
        optionValue="value"
        placeholder="Patient"
        showClear
        class="filter-select"
      />
      <DatePicker
        v-model="filterDateFrom"
        placeholder="From date"
        :show-time="false"
        showClear
        class="filter-date"
      />
      <DatePicker
        v-model="filterDateTo"
        placeholder="To date"
        :show-time="false"
        :show-clear="!(viewMode === 'timetable' && timetableScope === 'day')"
        :disabled="viewMode === 'timetable' && timetableScope === 'day'"
        class="filter-date"
      />
      <Select
        v-model="filterStatus"
        :options="statusOptions"
        optionLabel="label"
        optionValue="value"
        placeholder="Status"
        showClear
        class="filter-select"
      />
      <IconField class="mc-search-field">
        <InputIcon class="pi pi-search" />
        <InputText v-model="search" placeholder="Search in loaded results..." />
      </IconField>
      <div class="view-toggle">
        <Button
          label="Table"
          :severity="viewMode === 'table' ? 'primary' : 'secondary'"
          size="small"
          @click="viewMode = 'table'"
        />
        <Button
          label="Timetable"
          :severity="viewMode === 'timetable' ? 'primary' : 'secondary'"
          size="small"
          @click="viewMode = 'timetable'"
        />
      </div>
      <div v-if="viewMode === 'timetable'" class="scope-toggle">
        <Button
          label="Day"
          :severity="timetableScope === 'day' ? 'primary' : 'secondary'"
          size="small"
          @click="timetableScope = 'day'; applyTimetableScopeToFilters()"
        />
        <Button
          label="Week"
          :severity="timetableScope === 'week' ? 'primary' : 'secondary'"
          size="small"
          @click="timetableScope = 'week'; applyTimetableScopeToFilters()"
        />
      </div>
    </div>

    <div v-if="viewMode === 'table'" class="table-view">
    <DataTable
      :value="filteredAppointments"
      :loading="loading"
      :lazy="true"
      :totalRecords="totalRecords"
      dataKey="id"
      paginator
      :rows="lazyParams.rows"
      :rowsPerPageOptions="[10, 25, 50]"
      :sortField="lazyParams.sortField"
      :sortOrder="lazyParams.sortOrder"
      stripedRows
      removableSort
      @page="onPage"
      @sort="onSort"
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
            @click="openDetailsModal(data)"
          />
        </template>
      </Column>

      <Column
        v-if="canUpdateAppointmentStatus && filteredAppointments.some((a) => a.status !== 'CANCELLED' && a.status !== 'COMPLETED')"
        style="width: 10rem"
      >
        <template #body="{ data }">
          <div v-if="data.status === 'SCHEDULED'" class="mc-row-actions">
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
          <div v-else-if="data.status === 'IN_PROGRESS'" class="mc-row-actions">
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
    </div>

    <div v-else class="timetable-view">
      <div v-if="timetableLoading" class="timetable-loading">
        <i class="pi pi-spin pi-spinner" />
        Loading timetable...
      </div>
      <div v-else class="timetable-grid">
        <div class="timetable-header" :style="{ '--day-count': timetableDays.length }">
          <div class="timetable-corner">Time</div>
          <div
            v-for="day in timetableDays"
            :key="day.toISOString()"
            class="timetable-day-header"
          >
            {{ formatDate(day.toISOString()) }}
          </div>
        </div>
        <div
          v-for="slot in TIME_SLOTS"
          :key="slot"
          class="timetable-row"
          :style="{ '--day-count': timetableDays.length }"
        >
          <div class="timetable-slot-label">{{ slot }}</div>
          <div
            v-for="day in timetableDays"
            :key="`${slot}-${day.toISOString()}`"
            class="timetable-cell"
          >
            <div
              v-for="apt in getAppointmentsForCell(day, slot)"
              :key="apt.id"
              class="timetable-block"
              :class="{ 'timetable-block-completed': apt.status === 'COMPLETED' }"
              @click="handleTimetableBlockClick(apt)"
            >
              <span class="timetable-block-patient">{{ apt.clientName }}</span>
              <span class="timetable-block-doctor">{{ apt.employeeName }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Dialog
      v-model:visible="dialogVisible"
      header="New Appointment"
      modal
      :style="{ width: DIALOG_WIDTH_DEFAULT }"
    >
      <div class="mc-dialog-form dialog-form">
        <div class="field">
          <label for="dlg-doctor">Doctor *</label>
          <Select
            id="dlg-doctor"
            v-model="form.employeeId"
            :options="doctorOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Select doctor"
            showClear
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
            showClear
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
            :key="form.endTime?.getTime() ?? 0"
            v-model="formEndTimeModel"
            time-only
            hour-format="24"
            :disabled="saving || !form.startTime"
            fluid
          />
          <div v-if="form.startTime" class="presets-row">
            <Button
              v-for="p in DURATION_PRESETS"
              :key="p.value"
              size="small"
              severity="secondary"
              :label="p.label"
              :disabled="saving"
              @click="applyDurationPreset(form.startTime, (d) => (form.endTime = d), p.value)"
            />
          </div>
          <p v-if="formDurationMinutes != null" class="field-helper">{{ formatDuration(formDurationMinutes) }}</p>
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

    <Dialog
      v-model:visible="detailsModalOpen"
      header="Edit Appointment"
      modal
      :style="{ width: DIALOG_WIDTH_DEFAULT }"
    >
      <div v-if="viewingAppointment" class="mc-dialog-form dialog-form">
        <div class="field">
          <label>Status</label>
          <Tag :value="viewingAppointment.status" :severity="appointmentStatusSeverity(viewingAppointment.status)" />
        </div>
        <div class="field">
          <label for="edit-doctor">Doctor *</label>
          <Select
            id="edit-doctor"
            v-model="editForm.employeeId"
            :options="doctorOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Select doctor"
            showClear
            :disabled="saving"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="edit-patient">Patient *</label>
          <Select
            id="edit-patient"
            v-model="editForm.clientId"
            :options="patientOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Select patient"
            showClear
            :disabled="saving"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="edit-start">Start time *</label>
          <DatePicker
            id="edit-start"
            v-model="editForm.startTime"
            show-time
            hour-format="24"
            :disabled="saving"
            fluid
          />
        </div>
        <div class="field">
          <label for="edit-end">End time *</label>
          <DatePicker
            id="edit-end"
            :key="editForm.endTime?.getTime() ?? 0"
            v-model="editFormEndTimeModel"
            time-only
            hour-format="24"
            :disabled="saving || !editForm.startTime"
            fluid
          />
          <div v-if="editForm.startTime" class="presets-row">
            <Button
              v-for="p in DURATION_PRESETS"
              :key="p.value"
              size="small"
              severity="secondary"
              :label="p.label"
              :disabled="saving"
              @click="applyDurationPreset(editForm.startTime, (d) => (editForm.endTime = d), p.value)"
            />
          </div>
          <p v-if="editFormDurationMinutes != null" class="field-helper">{{ formatDuration(editFormDurationMinutes) }}</p>
        </div>
        <div class="field">
          <label for="edit-notes">Notes</label>
          <Textarea id="edit-notes" v-model="editForm.notes" :disabled="saving" rows="3" autoResize />
        </div>
        <div
          v-if="canUpdateAppointmentStatus && viewingAppointment.status !== 'CANCELLED' && viewingAppointment.status !== 'COMPLETED'"
          class="field detail-actions"
        >
          <div class="mc-row-actions">
            <Button
              v-if="viewingAppointment.status === 'SCHEDULED'"
              label="Start"
              size="small"
              severity="success"
              @click="changeStatus(viewingAppointment, 'IN_PROGRESS')"
            />
            <Button
              v-if="viewingAppointment.status === 'SCHEDULED'"
              label="Cancel"
              size="small"
              severity="danger"
              text
              @click="changeStatus(viewingAppointment, 'CANCELLED')"
            />
            <Button
              v-if="viewingAppointment.status === 'IN_PROGRESS'"
              label="Complete"
              size="small"
              severity="success"
              @click="changeStatus(viewingAppointment, 'COMPLETED')"
            />
            <Button
              v-if="viewingAppointment.status === 'IN_PROGRESS'"
              label="Cancel"
              size="small"
              severity="danger"
              text
              @click="changeStatus(viewingAppointment, 'CANCELLED')"
            />
          </div>
        </div>
      </div>
      <template #footer>
        <Button
          label="Close"
          severity="secondary"
          text
          :disabled="saving"
          @click="closeDetailsModal"
        />
        <Button
          v-if="canUpdateAppointments"
          label="Update"
          icon="pi pi-check"
          :loading="saving"
          @click="saveEditAppointment"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.filters {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.mc-search-field :deep(.p-inputtext) {
  width: 100%;
  max-width: 300px;
}

.filter-select {
  min-width: 140px;
}

.filter-date {
  min-width: 140px;
}

.text-muted {
  color: var(--p-text-muted-color);
}

.dialog-form .field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.dialog-form .field label {
  font-weight: 500;
}

.presets-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.field-helper {
  font-size: 0.875rem;
  color: var(--p-text-muted-color);
  margin: 0;
}

.dialog-form .field :deep(.p-inputtext),
.dialog-form .field :deep(.p-textarea) {
  width: 100%;
}

.detail-actions {
  margin-top: 0.5rem;
}

.view-toggle,
.scope-toggle {
  display: flex;
  gap: 0.25rem;
}

.timetable-view {
  min-height: 400px;
}

.timetable-loading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem;
  color: var(--p-text-muted-color);
}

.timetable-grid {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--p-surface-border);
  border-radius: 8px;
  overflow-x: auto;
}

.timetable-header {
  display: grid;
  grid-template-columns: 80px repeat(var(--day-count, 8), minmax(120px, 1fr));
  background: var(--p-surface-100);
  font-weight: 600;
  font-size: 0.875rem;
}

.timetable-corner {
  padding: 0.75rem 1rem;
  border-right: 1px solid var(--p-surface-border);
  border-bottom: 1px solid var(--p-surface-border);
}

.timetable-day-header {
  padding: 0.75rem 1rem;
  border-right: 1px solid var(--p-surface-border);
  border-bottom: 1px solid var(--p-surface-border);
  text-align: center;
}

.timetable-row {
  display: grid;
  grid-template-columns: 80px repeat(var(--day-count, 8), minmax(120px, 1fr));
  min-height: 2.5rem;
}

.timetable-slot-label {
  padding: 0.5rem 1rem;
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
  border-right: 1px solid var(--p-surface-border);
  border-bottom: 1px solid var(--p-surface-border);
}

.timetable-cell {
  padding: 2px;
  border-right: 1px solid var(--p-surface-border);
  border-bottom: 1px solid var(--p-surface-border);
  min-height: 2rem;
}

.timetable-block {
  padding: 0.25rem 0.5rem;
  margin-bottom: 2px;
  background: var(--p-primary-color);
  color: var(--p-primary-contrast-color);
  border-radius: 4px;
  font-size: 0.75rem;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
}

.timetable-block:hover {
  opacity: 0.9;
}

.timetable-block-patient {
  display: block;
  font-weight: 500;
}

.timetable-block-doctor {
  display: block;
  font-size: 0.7rem;
  opacity: 0.9;
}

.timetable-block-completed {
  background: var(--p-surface-400);
}

@media (max-width: 768px) {
  .mc-search-field :deep(.p-inputtext) {
    max-width: none;
  }
}
</style>
