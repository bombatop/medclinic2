import { computed, ref } from 'vue'
import { getDoctors, type Doctor } from '@/api/doctors'
import { getPatients, type Patient } from '@/api/patients'
import type {
  DoctorSelectOption,
  PatientSelectOption,
} from '@/views/appointments/appointmentTypes'

export function useDoctorPatientOptions() {
  const doctors = ref<Doctor[]>([])
  const patients = ref<Patient[]>([])

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

  return {
    doctors,
    patients,
    doctorOptions,
    patientOptions,
    loadDoctorsAndPatients,
  }
}
