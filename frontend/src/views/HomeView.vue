<script setup lang="ts">
import Card from 'primevue/card'
</script>

<template>
  <div>
    <h1>Dashboard</h1>
    <div class="dashboard-grid">
      <Card>
        <template #title>Patients</template>
        <template #content>
          <p>Manage patient records and history.</p>
        </template>
      </Card>
      <Card>
        <template #title>Appointments</template>
        <template #content>
          <p>Schedule and track appointments.</p>
        </template>
      </Card>
      <Card>
        <template #title>Doctors</template>
        <template #content>
          <p>View doctor schedules and specializations.</p>
        </template>
      </Card>
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
</style>
