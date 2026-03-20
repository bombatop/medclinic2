import { computed, reactive, watch } from 'vue'
import {
  DURATION_PRESETS,
} from '@/views/appointments/appointmentConstants'

export type AppointmentFormState = {
  employeeId: number | null
  clientId: number | null
  startTime: Date | null
  endTime: Date | null
  notes: string
}

export function formatDuration(minutes: number): string {
  if (minutes < 60) return `${minutes} min`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m === 0 ? `${h} h` : `${h} h ${m} min`
}

export function applyDurationPreset(
  startTime: Date | null,
  setEndTime: (d: Date | null) => void,
  minutes: number,
) {
  if (!startTime) return
  const end = new Date(startTime)
  end.setMinutes(end.getMinutes() + minutes)
  setEndTime(end)
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
        const durationMin = Math.max(
          5,
          Math.round((editForm.endTime.getTime() - prevStart.getTime()) / 60000),
        )
        const end = new Date(start)
        end.setMinutes(end.getMinutes() + durationMin)
        editForm.endTime = end
      }
    },
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
