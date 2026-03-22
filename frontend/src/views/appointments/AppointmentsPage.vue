<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { Appointment } from '@/api/appointments'
import { DEFAULT_DEBOUNCE_MS, useDebounceFn } from '@/composables/useDebounceFn'
import Button from 'primevue/button'
import { useToast } from 'primevue/usetoast'
import { filterByParticipantSearch, resetAppointmentForm } from '@/views/appointments/appointmentHelpers'
import { useAppointmentFilters } from '@/views/appointments/composables/useAppointmentFilters'
import { useAppointmentForms } from '@/views/appointments/composables/useAppointmentForms'
import { useAppointmentMutations } from '@/views/appointments/composables/useAppointmentMutations'
import { useAppointmentPermissions } from '@/views/appointments/composables/useAppointmentPermissions'
import { useAppointmentTableLoad } from '@/views/appointments/composables/useAppointmentTableLoad'
import { useAppointmentTimetable } from '@/views/appointments/composables/useAppointmentTimetable'
import { useAppointmentViewMode } from '@/views/appointments/composables/useAppointmentViewMode'
import { useDoctorPatientOptions } from '@/views/appointments/composables/useDoctorPatientOptions'
import AppointmentCreateDialog from '@/views/appointments/components/AppointmentCreateDialog.vue'
import AppointmentEditDialog from '@/views/appointments/components/AppointmentEditDialog.vue'
import AppointmentsFiltersBar from '@/views/appointments/components/AppointmentsFiltersBar.vue'
import AppointmentsTable from '@/views/appointments/components/AppointmentsTable.vue'
import AppointmentsTimetable from '@/views/appointments/components/AppointmentsTimetable.vue'

const toast = useToast()

const { canCreateAppointments, canUpdateAppointments, canUpdateAppointmentStatus } =
  useAppointmentPermissions()

const { viewMode, timetableScope } = useAppointmentViewMode()
const {
  filterDoctor,
  filterPatient,
  filterStatus,
  filterDateFrom,
  filterDateTo,
  activeFilters,
  applyTimetableScopeToFilters,
} = useAppointmentFilters(viewMode, timetableScope)

const search = ref('')
const debouncedParticipantSearch = ref('')
const applyDebouncedParticipantSearch = useDebounceFn((q: string) => {
  debouncedParticipantSearch.value = q
}, DEFAULT_DEBOUNCE_MS)

watch(search, (v) => {
  applyDebouncedParticipantSearch(v)
})

const {
  appointments,
  totalRecords,
  loading,
  lazyParams,
  loadAppointments,
  onPage,
  onSort,
  resetToFirstPage,
} = useAppointmentTableLoad(activeFilters, toast)

const filteredAppointments = computed(() =>
  filterByParticipantSearch(appointments.value, debouncedParticipantSearch.value.trim()),
)

const {
  timetableAppointments,
  timetableLoading,
  timetableDays,
  timeSlots,
  getAppointmentsForCell,
  loadTimetableAppointments,
} = useAppointmentTimetable(
  activeFilters,
  viewMode,
  filterDateFrom,
  filterDateTo,
  debouncedParticipantSearch,
  toast,
)

function loadForCurrentView() {
  if (viewMode.value === 'timetable') void loadTimetableAppointments()
  else void loadAppointments()
}

function reloadCurrentView() {
  resetToFirstPage()
  void loadForCurrentView()
}

const debouncedReloadCurrentView = useDebounceFn(reloadCurrentView, 120)

watch(viewMode, (mode) => {
  if (mode === 'table') {
    reloadCurrentView()
  }
})

watch(
  [filterDoctor, filterPatient, filterStatus, filterDateFrom, filterDateTo],
  () => debouncedReloadCurrentView(),
)

const { doctorOptions, patientOptions, loadDoctorsAndPatients } = useDoctorPatientOptions()

const appointmentForms = useAppointmentForms()
const {
  DURATION_PRESETS,
  form,
  editForm,
  formDurationMinutes,
  editFormDurationMinutes,
} = appointmentForms
const formEndTimeModel = appointmentForms.formEndTimeModel
const editFormEndTimeModel = appointmentForms.editFormEndTimeModel

const dialogVisible = ref(false)
const saving = ref(false)
const viewingAppointment = ref<Appointment | null>(null)
const editingAppointmentId = ref<number | null>(null)

const { saveAppointment, saveEditAppointment, changeStatus } = useAppointmentMutations({
  toast,
  canCreateAppointments,
  canUpdateAppointments,
  canUpdateAppointmentStatus,
  form,
  editForm,
  appointments,
  totalRecords,
  lazyParams,
  timetableAppointments,
  viewMode,
  dialogVisible,
  viewingAppointment,
  editingAppointmentId,
  saving,
})

const detailsModalOpen = computed({
  get: () => viewingAppointment.value != null,
  set: (v) => {
    if (!v) closeDetailsModal()
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

function openCreateDialog() {
  resetAppointmentForm(form)
  dialogVisible.value = true
}

function onEditChangeStatus(status: Appointment['status']) {
  const apt = viewingAppointment.value
  if (apt) void changeStatus(apt, status)
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

    <AppointmentsFiltersBar
      :filter-doctor="filterDoctor"
      :filter-patient="filterPatient"
      :filter-status="filterStatus"
      :filter-date-from="filterDateFrom"
      :filter-date-to="filterDateTo"
      :search="search"
      :view-mode="viewMode"
      :timetable-scope="timetableScope"
      :doctor-options="doctorOptions"
      :patient-options="patientOptions"
      @update:filter-doctor="filterDoctor = $event"
      @update:filter-patient="filterPatient = $event"
      @update:filter-status="filterStatus = $event"
      @update:filter-date-from="filterDateFrom = $event"
      @update:filter-date-to="filterDateTo = $event"
      @update:search="search = $event"
      @update:view-mode="viewMode = $event"
      @update:timetable-scope="timetableScope = $event"
      @apply-timetable-scope="applyTimetableScopeToFilters"
    />

    <AppointmentsTable
      v-if="viewMode === 'table'"
      :rows="filteredAppointments"
      :totalRecords="totalRecords"
      :loading="loading"
      :lazyParams="lazyParams"
      :canUpdateAppointments="canUpdateAppointments"
      :canUpdateAppointmentStatus="canUpdateAppointmentStatus"
      @page="onPage"
      @sort="onSort"
      @edit="openDetailsModal"
      @change-status="(apt, st) => void changeStatus(apt, st)"
    />

    <AppointmentsTimetable
      v-else
      :loading="timetableLoading"
      :days="timetableDays"
      :timeSlots="timeSlots"
      :getAppointmentsForCell="getAppointmentsForCell"
      @block-click="openDetailsModal"
    />

    <AppointmentCreateDialog
      v-if="dialogVisible"
      v-model:visible="dialogVisible"
      v-model:form-end-time="formEndTimeModel"
      :form="form"
      :doctor-options="doctorOptions"
      :patient-options="patientOptions"
      :saving="saving"
      :duration-presets="DURATION_PRESETS"
      :form-duration-minutes="formDurationMinutes"
      @save="saveAppointment"
    />

    <AppointmentEditDialog
      v-if="viewingAppointment"
      v-model:visible="detailsModalOpen"
      v-model:edit-form-end-time="editFormEndTimeModel"
      :viewing-appointment="viewingAppointment"
      :edit-form="editForm"
      :doctor-options="doctorOptions"
      :patient-options="patientOptions"
      :saving="saving"
      :can-update-appointments="canUpdateAppointments"
      :can-update-appointment-status="canUpdateAppointmentStatus"
      :duration-presets="DURATION_PRESETS"
      :edit-form-duration-minutes="editFormDurationMinutes"
      @close="closeDetailsModal"
      @update="saveEditAppointment"
      @change-status="onEditChangeStatus"
    />
  </div>
</template>
