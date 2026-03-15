<script setup lang="ts">
import { computed, onMounted } from 'vue'
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

const menuItems = computed<MenuItem[]>(() => {
  const items: MenuItem[] = [
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
  ]

  if (authStore.isAdmin) {
    items.push({
      label: 'Admin',
      icon: 'pi pi-shield',
      items: [
        {
          label: 'Users',
          icon: 'pi pi-id-card',
          command: () => router.push('/admin/users'),
        },
      ],
    })
  }

  items.push(
    {
      label: 'Profile',
      icon: 'pi pi-id-card',
      command: () => router.push('/profile'),
    },
    {
      label: 'Logout',
      icon: 'pi pi-sign-out',
      command: () => {
        authStore.clearTokens()
        router.push({ name: 'login' })
      },
    },
  )

  return items
})
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
