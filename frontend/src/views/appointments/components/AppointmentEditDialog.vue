<script setup lang="ts">
import type { Appointment } from '@/api/appointments'
import { DIALOG_WIDTH_DEFAULT } from '@/constants/ui'
import { appointmentStatusSeverity } from '@/utils/formatting'
import {
  applyDurationPreset,
  formatDuration,
} from '@/views/appointments/appointmentHelpers'
import type {
  AppointmentFormState,
  DoctorSelectOption,
  PatientSelectOption,
} from '@/views/appointments/appointmentTypes'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'

defineProps<{
  viewingAppointment: Appointment
  editForm: AppointmentFormState
  doctorOptions: DoctorSelectOption[]
  patientOptions: PatientSelectOption[]
  saving: boolean
  canUpdateAppointments: boolean
  canUpdateAppointmentStatus: boolean
  durationPresets: readonly { label: string; value: number }[]
  editFormDurationMinutes: number | null
}>()

const visible = defineModel<boolean>('visible', { required: true })

const editFormEndTimeModel = defineModel<Date | null>('editFormEndTime', { required: true })

const emit = defineEmits<{
  close: []
  update: []
  'change-status': [status: Appointment['status']]
}>()
</script>

<template>
  <Dialog
    v-model:visible="visible"
    header="Edit Appointment"
    modal
    :style="{ width: DIALOG_WIDTH_DEFAULT }"
  >
    <div class="mc-dialog-form dialog-form">
      <div class="field">
        <label>Status</label>
        <Tag
          :value="viewingAppointment.status"
          :severity="appointmentStatusSeverity(viewingAppointment.status)"
        />
      </div>
      <div class="field">
        <label for="edit-doctor">Doctor *</label>
        <Select
          id="edit-doctor"
          v-model="editForm.employeeId"
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
        <label for="edit-patient">Patient *</label>
        <Select
          id="edit-patient"
          v-model="editForm.clientId"
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
        <div v-if="editForm.startTime" class="mc-duration-presets">
          <Button
            v-for="p in durationPresets"
            :key="p.value"
            size="small"
            severity="secondary"
            :label="p.label"
            :disabled="saving"
            @click="applyDurationPreset(editForm.startTime, (d) => (editForm.endTime = d), p.value)"
          />
        </div>
        <p v-if="editFormDurationMinutes != null" class="mc-field-hint">
          {{ formatDuration(editFormDurationMinutes) }}
        </p>
      </div>
      <div class="field">
        <label for="edit-notes">Notes</label>
        <Textarea id="edit-notes" v-model="editForm.notes" :disabled="saving" rows="3" autoResize />
      </div>
      <div
        v-if="
          canUpdateAppointmentStatus &&
          viewingAppointment.status !== 'CANCELLED' &&
          viewingAppointment.status !== 'COMPLETED'
        "
        class="field mc-detail-actions"
      >
        <div class="mc-row-actions">
          <Button
            v-if="viewingAppointment.status === 'SCHEDULED'"
            label="Start"
            size="small"
            severity="success"
            @click="emit('change-status', 'IN_PROGRESS')"
          />
          <Button
            v-if="viewingAppointment.status === 'SCHEDULED'"
            label="Cancel"
            size="small"
            severity="danger"
            text
            @click="emit('change-status', 'CANCELLED')"
          />
          <Button
            v-if="viewingAppointment.status === 'IN_PROGRESS'"
            label="Complete"
            size="small"
            severity="success"
            @click="emit('change-status', 'COMPLETED')"
          />
          <Button
            v-if="viewingAppointment.status === 'IN_PROGRESS'"
            label="Cancel"
            size="small"
            severity="danger"
            text
            @click="emit('change-status', 'CANCELLED')"
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
        @click="emit('close')"
      />
      <Button
        v-if="canUpdateAppointments"
        label="Update"
        icon="pi pi-check"
        :loading="saving"
        @click="emit('update')"
      />
    </template>
  </Dialog>
</template>
