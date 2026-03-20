import { storeToRefs } from 'pinia'
import { computed } from 'vue'
import { useReferenceDataStore } from '@/stores/referenceData'
import type {
  DoctorSelectOption,
  PatientSelectOption,
} from '@/views/appointments/appointmentTypes'

export function useDoctorPatientOptions() {
  const store = useReferenceDataStore()
  const { doctors, patients } = storeToRefs(store)

  const doctorOptions = computed((): DoctorSelectOption[] =>
    doctors.value
      .filter((d) => d.active)
      .map((d) => ({
        label: `${d.firstName} ${d.lastName}${d.specialization ? ` (${d.specialization})` : ''}`,
        value: d.id,
      })),
  )

  const patientOptions = computed((): PatientSelectOption[] =>
    patients.value.map((p) => ({
      label: `${p.firstName} ${p.lastName}`,
      value: p.id,
    })),
  )

  function loadDoctorsAndPatients(): Promise<void> {
    return store.ensureDoctorsPatients()
  }

  return {
    doctors,
    patients,
    doctorOptions,
    patientOptions,
    loadDoctorsAndPatients,
  }
}
