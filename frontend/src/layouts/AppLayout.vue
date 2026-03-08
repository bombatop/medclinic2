<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getCurrentUser } from '@/api/auth'
import Menubar from 'primevue/menubar'
import Toast from 'primevue/toast'
import type { MenuItem } from 'primevue/menuitem'

const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  getCurrentUser().catch(() => { /* 401 handled by interceptor */ })
})

const menuItems = ref<MenuItem[]>([
  {
    label: 'Home',
    icon: 'pi pi-home',
    command: () => router.push('/'),
  },
  {
    label: 'Patients',
    icon: 'pi pi-users',
    command: () => router.push('/patients'),
  },
  {
    label: 'Appointments',
    icon: 'pi pi-calendar',
    command: () => router.push('/appointments'),
  },
  {
    label: 'Doctors',
    icon: 'pi pi-user',
    command: () => router.push('/doctors'),
  },
  {
    label: 'Logout',
    icon: 'pi pi-sign-out',
    command: () => {
      authStore.clearTokens()
      router.push({ name: 'login' })
    },
  },
])
</script>

<template>
  <div class="app-layout">
    <Toast />
    <Menubar :model="menuItems">
      <template #start>
        <span class="app-title">MedClinic</span>
      </template>
    </Menubar>
    <main class="app-content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app-title {
  font-weight: 700;
  font-size: 1.25rem;
  margin-right: 1.5rem;
}

.app-content {
  max-width: 1200px;
  margin: 2rem auto;
  padding: 0 1.5rem;
}
</style>
