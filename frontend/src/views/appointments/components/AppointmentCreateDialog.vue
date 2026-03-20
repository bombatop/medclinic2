<script setup lang="ts">
import { DIALOG_WIDTH_DEFAULT } from '@/constants/ui'
import type { AppointmentFormState } from '@/views/appointments/composables/useAppointmentForms'
import {
  applyDurationPreset,
  formatDuration,
} from '@/views/appointments/composables/useAppointmentForms'
import type {
  DoctorSelectOption,
  PatientSelectOption,
} from '@/views/appointments/composables/useDoctorPatientOptions'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'

defineProps<{
  form: AppointmentFormState
  doctorOptions: DoctorSelectOption[]
  patientOptions: PatientSelectOption[]
  saving: boolean
  durationPresets: readonly { label: string; value: number }[]
  formDurationMinutes: number | null
}>()

const visible = defineModel<boolean>('visible', { required: true })

const formEndTimeModel = defineModel<Date | null>('formEndTime', { required: true })

const emit = defineEmits<{
  save: []
}>()
</script>

<template>
  <Dialog
    v-model:visible="visible"
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
          option-label="label"
          option-value="value"
          placeholder="Select doctor"
          show-clear
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
          option-label="label"
          option-value="value"
          placeholder="Select patient"
          show-clear
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
            v-for="p in durationPresets"
            :key="p.value"
            size="small"
            severity="secondary"
            :label="p.label"
            :disabled="saving"
            @click="applyDurationPreset(form.startTime, (d) => (form.endTime = d), p.value)"
          />
        </div>
        <p v-if="formDurationMinutes != null" class="field-helper">
          {{ formatDuration(formDurationMinutes) }}
        </p>
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
        @click="visible = false"
      />
      <Button
        label="Create"
        icon="pi pi-check"
        :loading="saving"
        @click="emit('save')"
      />
    </template>
  </Dialog>
</template>

<style scoped>
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
</style>
