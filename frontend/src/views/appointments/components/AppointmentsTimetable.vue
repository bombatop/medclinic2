<script setup lang="ts">
import type { Appointment } from '@/api/appointments'
import { formatDate } from '@/utils/formatting'

defineProps<{
  loading: boolean
  days: Date[]
  timeSlots: string[]
  getAppointmentsForCell: (day: Date, slotStart: string) => Appointment[]
}>()

const emit = defineEmits<{
  'block-click': [appointment: Appointment]
}>()
</script>

<template>
  <div class="timetable-view">
    <div v-if="loading" class="timetable-loading">
      <i class="pi pi-spin pi-spinner" />
      Loading timetable...
    </div>
    <div v-else class="timetable-grid">
      <div class="timetable-header" :style="{ '--day-count': days.length }">
        <div class="timetable-corner">Time</div>
        <div
          v-for="day in days"
          :key="day.toISOString()"
          class="timetable-day-header"
        >
          {{ formatDate(day.toISOString()) }}
        </div>
      </div>
      <div
        v-for="slot in timeSlots"
        :key="slot"
        class="timetable-row"
        :style="{ '--day-count': days.length }"
      >
        <div class="timetable-slot-label">{{ slot }}</div>
        <div
          v-for="day in days"
          :key="`${slot}-${day.toISOString()}`"
          class="timetable-cell"
        >
          <div
            v-for="apt in getAppointmentsForCell(day, slot)"
            :key="apt.id"
            class="timetable-block"
            :class="{ 'timetable-block-completed': apt.status === 'COMPLETED' }"
            @click="emit('block-click', apt)"
          >
            <span class="timetable-block-patient">{{ apt.clientName }}</span>
            <span class="timetable-block-doctor">{{ apt.employeeName }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.timetable-view {
  min-height: 400px;
}

.timetable-loading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem;
  color: var(--p-text-muted-color);
}

.timetable-grid {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--p-surface-border);
  border-radius: 8px;
  overflow-x: auto;
}

.timetable-header {
  display: grid;
  grid-template-columns: 80px repeat(var(--day-count, 8), minmax(120px, 1fr));
  background: var(--p-surface-100);
  font-weight: 600;
  font-size: 0.875rem;
}

.timetable-corner {
  padding: 0.75rem 1rem;
  border-right: 1px solid var(--p-surface-border);
  border-bottom: 1px solid var(--p-surface-border);
}

.timetable-day-header {
  padding: 0.75rem 1rem;
  border-right: 1px solid var(--p-surface-border);
  border-bottom: 1px solid var(--p-surface-border);
  text-align: center;
}

.timetable-row {
  display: grid;
  grid-template-columns: 80px repeat(var(--day-count, 8), minmax(120px, 1fr));
  min-height: 2.5rem;
}

.timetable-slot-label {
  padding: 0.5rem 1rem;
  font-size: 0.8rem;
  color: var(--p-text-muted-color);
  border-right: 1px solid var(--p-surface-border);
  border-bottom: 1px solid var(--p-surface-border);
}

.timetable-cell {
  padding: 2px;
  border-right: 1px solid var(--p-surface-border);
  border-bottom: 1px solid var(--p-surface-border);
  min-height: 2rem;
}

.timetable-block {
  padding: 0.25rem 0.5rem;
  margin-bottom: 2px;
  background: var(--p-primary-color);
  color: var(--p-primary-contrast-color);
  border-radius: 4px;
  font-size: 0.75rem;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
}

.timetable-block:hover {
  opacity: 0.9;
}

.timetable-block-patient {
  display: block;
  font-weight: 500;
}

.timetable-block-doctor {
  display: block;
  font-size: 0.7rem;
  opacity: 0.9;
}

.timetable-block-completed {
  background: var(--p-surface-400);
}
</style>
