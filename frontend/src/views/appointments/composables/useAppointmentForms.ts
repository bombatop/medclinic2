import { computed, reactive, watch } from 'vue'
import { DURATION_PRESETS } from '@/views/appointments/appointmentConstants'
import type { AppointmentFormState } from '@/views/appointments/appointmentTypes'

function syncEndTimeOnStartChange(
  state: AppointmentFormState,
  start: Date | null,
  prevStart: Date | null,
) {
  if (!start) return
  if (!state.endTime) {
    const end = new Date(start)
    end.setMinutes(end.getMinutes() + 30)
    state.endTime = end
    return
  }
  if (prevStart && state.endTime.getTime() <= start.getTime()) {
    const durationMin = Math.max(
      5,
      Math.round((state.endTime.getTime() - prevStart.getTime()) / 60000),
    )
    const end = new Date(start)
    end.setMinutes(end.getMinutes() + durationMin)
    state.endTime = end
  }
}

function endTimeModelFor(state: AppointmentFormState) {
  return computed({
    get: () => state.endTime,
    set: (value: Date | null) => {
      if (!value) {
        state.endTime = null
        return
      }
      if (!state.startTime) {
        state.endTime = value
        return
      }
      const end = new Date(state.startTime)
      end.setHours(value.getHours(), value.getMinutes(), 0, 0)
      state.endTime = end
    },
  })
}

export function useAppointmentForms() {
  const form = reactive<AppointmentFormState>({
    employeeId: null,
    clientId: null,
    startTime: null,
    endTime: null,
    notes: '',
  })

  const editForm = reactive<AppointmentFormState>({
    employeeId: null,
    clientId: null,
    startTime: null,
    endTime: null,
    notes: '',
  })

  const formDurationMinutes = computed(() => {
    if (!form.startTime || !form.endTime) return null
    return Math.round((form.endTime.getTime() - form.startTime.getTime()) / 60000)
  })

  const editFormDurationMinutes = computed(() => {
    if (!editForm.startTime || !editForm.endTime) return null
    return Math.round((editForm.endTime.getTime() - editForm.startTime.getTime()) / 60000)
  })

  const formEndTimeModel = endTimeModelFor(form)
  const editFormEndTimeModel = endTimeModelFor(editForm)

  watch(
    () => form.startTime,
    (start, prevStart) => syncEndTimeOnStartChange(form, start, prevStart),
  )

  watch(
    () => editForm.startTime,
    (start, prevStart) => syncEndTimeOnStartChange(editForm, start, prevStart),
  )

  return {
    DURATION_PRESETS,
    form,
    editForm,
    formDurationMinutes,
    editFormDurationMinutes,
    formEndTimeModel,
    editFormEndTimeModel,
  }
}
