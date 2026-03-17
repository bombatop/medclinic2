<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getAppointments } from '@/api/appointments'
import { getDoctors } from '@/api/doctors'
import { getPatients } from '@/api/patients'
import Card from 'primevue/card'

const patientsCount = ref<number | null>(null)
const doctorsCount = ref<number | null>(null)
const appointmentsCount = ref<number | null>(null)
const todayCount = ref<number | null>(null)

async function loadCounts() {
  try {
    const [patientsRes, doctorsRes, appointmentsRes] = await Promise.all([
      getPatients({ page: 0, size: 1 }),
      getDoctors({ page: 0, size: 1 }),
      getAppointments({ page: 0, size: 1000 }),
    ])
    patientsCount.value = patientsRes.totalElements
    doctorsCount.value = doctorsRes.totalElements
    appointmentsCount.value = appointmentsRes.totalElements

    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const tomorrow = new Date(today)
    tomorrow.setDate(tomorrow.getDate() + 1)
    todayCount.value = appointmentsRes.content.filter((a) => {
      const d = new Date(a.startTime)
      return d >= today && d < tomorrow && a.status !== 'CANCELLED'
    }).length
  } catch {
    patientsCount.value = 0
    doctorsCount.value = 0
    appointmentsCount.value = 0
    todayCount.value = 0
  }
}

onMounted(() => {
  void loadCounts()
})
</script>

<template>
  <div>
    <h1>Dashboard</h1>
    <div class="dashboard-grid">
      <router-link to="/patients" class="dashboard-card-link">
        <Card class="dashboard-card">
          <template #title>
            <span class="card-title">
              <i class="pi pi-users" />
              Patients
            </span>
          </template>
          <template #content>
            <p class="card-desc">Manage patient records and history.</p>
            <p v-if="patientsCount !== null" class="card-count">{{ patientsCount }} patients</p>
          </template>
        </Card>
      </router-link>
      <router-link to="/appointments" class="dashboard-card-link">
        <Card class="dashboard-card">
          <template #title>
            <span class="card-title">
              <i class="pi pi-calendar" />
              Appointments
            </span>
          </template>
          <template #content>
            <p class="card-desc">Schedule and track appointments.</p>
            <p v-if="appointmentsCount !== null" class="card-count">
              {{ appointmentsCount }} total
              <template v-if="todayCount !== null && todayCount > 0"> · {{ todayCount }} today</template>
            </p>
          </template>
        </Card>
      </router-link>
      <router-link to="/doctors" class="dashboard-card-link">
        <Card class="dashboard-card">
          <template #title>
            <span class="card-title">
              <i class="pi pi-user" />
              Doctors
            </span>
          </template>
          <template #content>
            <p class="card-desc">View doctor schedules and specializations.</p>
            <p v-if="doctorsCount !== null" class="card-count">{{ doctorsCount }} doctors</p>
          </template>
        </Card>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
  margin-top: 1.5rem;
}

.dashboard-card-link {
  text-decoration: none;
  color: inherit;
}

.dashboard-card {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.dashboard-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.card-title i {
  font-size: 1.25rem;
}

.card-desc {
  margin: 0 0 0.5rem;
  color: var(--p-text-muted-color);
}

.card-count {
  margin: 0;
  font-weight: 500;
  font-size: 0.95rem;
}
</style>
