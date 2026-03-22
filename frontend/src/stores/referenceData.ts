import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dedupePromise } from '@/api/inflightDedupe'
import { getDoctors, type Doctor } from '@/api/doctors'
import { getPatients, type Patient } from '@/api/patients'

const TTL_MS = 5 * 60 * 1000

const DOCTORS_KEY = 'get:doctors:page0size500'
const PATIENTS_KEY = 'get:patients:page0size500'

export const useReferenceDataStore = defineStore('referenceData', () => {
  const doctors = ref<Doctor[]>([])
  const patients = ref<Patient[]>([])
  let fetchedAt = 0
  let loadPromise: Promise<void> | null = null

  function isFresh(): boolean {
    return fetchedAt > 0 && Date.now() - fetchedAt < TTL_MS
  }

  async function ensureDoctorsPatients(): Promise<void> {
    if (isFresh()) return
    if (loadPromise) return loadPromise
    loadPromise = (async () => {
      try {
        const [docRes, patRes] = await Promise.all([
          dedupePromise(DOCTORS_KEY, () => getDoctors({ page: 0, size: 500 })),
          dedupePromise(PATIENTS_KEY, () => getPatients({ page: 0, size: 500 })),
        ])
        doctors.value = docRes.content
        patients.value = patRes.content
        fetchedAt = Date.now()
      } catch {
        doctors.value = []
        patients.value = []
        fetchedAt = 0
      }
    })().finally(() => {
      loadPromise = null
    })
    return loadPromise
  }

  function invalidateDoctorsPatients(): void {
    fetchedAt = 0
  }

  return {
    doctors,
    patients,
    ensureDoctorsPatients,
    invalidateDoctorsPatients,
  }
})
