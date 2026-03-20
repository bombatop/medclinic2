<script setup lang="ts">
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import { STATUS_FILTER_OPTIONS } from '@/views/appointments/appointmentConstants'
import type {
  AppointmentStatus,
  DoctorSelectOption,
  PatientSelectOption,
} from '@/views/appointments/appointmentTypes'

defineProps<{
  filterDoctor: number | null
  filterPatient: number | null
  filterStatus: AppointmentStatus | null
  filterDateFrom: Date | null
  filterDateTo: Date | null
  search: string
  viewMode: 'table' | 'timetable'
  timetableScope: 'day' | 'week'
  doctorOptions: DoctorSelectOption[]
  patientOptions: PatientSelectOption[]
}>()

const emit = defineEmits<{
  (e: 'update:filterDoctor', value: number | null): void
  (e: 'update:filterPatient', value: number | null): void
  (e: 'update:filterStatus', value: AppointmentStatus | null): void
  (e: 'update:filterDateFrom', value: Date | null): void
  (e: 'update:filterDateTo', value: Date | null): void
  (e: 'update:search', value: string): void
  (e: 'update:viewMode', value: 'table' | 'timetable'): void
  (e: 'update:timetableScope', value: 'day' | 'week'): void
  (e: 'applyTimetableScope'): void
}>()

function dateOnlyFromPicker(v: unknown): Date | null {
  if (v == null) return null
  if (Array.isArray(v)) return v[0] instanceof Date ? v[0] : null
  if (v instanceof Date) return v
  return null
}

function onFilterStatusUpdate(v: unknown) {
  emit('update:filterStatus', (v as AppointmentStatus | null) ?? null)
}

function setTimetableScope(scope: 'day' | 'week') {
  emit('update:timetableScope', scope)
  emit('applyTimetableScope')
}

const statusOptions = [...STATUS_FILTER_OPTIONS]
</script>

<template>
  <div class="mc-filters-bar">
    <Select
      :model-value="filterDoctor"
      :options="doctorOptions"
      option-label="label"
      option-value="value"
      placeholder="Doctor"
      show-clear
      class="mc-filter-select"
      @update:model-value="emit('update:filterDoctor', $event)"
    />
    <Select
      :model-value="filterPatient"
      :options="patientOptions"
      option-label="label"
      option-value="value"
      placeholder="Patient"
      show-clear
      class="mc-filter-select"
      @update:model-value="emit('update:filterPatient', $event)"
    />
    <DatePicker
      :model-value="filterDateFrom"
      placeholder="From date"
      :show-time="false"
      show-clear
      class="mc-filter-date"
      @update:model-value="emit('update:filterDateFrom', dateOnlyFromPicker($event))"
    />
    <DatePicker
      :model-value="filterDateTo"
      placeholder="To date"
      :show-time="false"
      :show-clear="!(viewMode === 'timetable' && timetableScope === 'day')"
      :disabled="viewMode === 'timetable' && timetableScope === 'day'"
      class="mc-filter-date"
      @update:model-value="emit('update:filterDateTo', dateOnlyFromPicker($event))"
    />
    <Select
      :model-value="filterStatus"
      :options="statusOptions"
      option-label="label"
      option-value="value"
      placeholder="Status"
      show-clear
      class="mc-filter-select"
      @update:model-value="onFilterStatusUpdate($event)"
    />
    <IconField class="mc-search-field">
      <InputIcon class="pi pi-search" />
      <InputText
        :model-value="search"
        placeholder="Search in loaded results..."
        @update:model-value="emit('update:search', String($event ?? ''))"
      />
    </IconField>
    <div class="mc-toggle-group">
      <Button
        label="Table"
        :severity="viewMode === 'table' ? 'primary' : 'secondary'"
        size="small"
        @click="emit('update:viewMode', 'table')"
      />
      <Button
        label="Timetable"
        :severity="viewMode === 'timetable' ? 'primary' : 'secondary'"
        size="small"
        @click="emit('update:viewMode', 'timetable')"
      />
    </div>
    <div v-if="viewMode === 'timetable'" class="mc-toggle-group">
      <Button
        label="Day"
        :severity="timetableScope === 'day' ? 'primary' : 'secondary'"
        size="small"
        @click="setTimetableScope('day')"
      />
      <Button
        label="Week"
        :severity="timetableScope === 'week' ? 'primary' : 'secondary'"
        size="small"
        @click="setTimetableScope('week')"
      />
    </div>
  </div>
</template>
