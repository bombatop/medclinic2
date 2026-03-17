<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getCurrentUser } from '@/api/auth'
import Menubar from 'primevue/menubar'
import Toast from 'primevue/toast'
import Button from 'primevue/button'
import type { MenuItem } from 'primevue/menuitem'

const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  getCurrentUser().catch(() => { /* 401 handled by interceptor */ })
})

const menuItems = computed<MenuItem[]>(() => {
  const items: MenuItem[] = [
    { label: 'Home', icon: 'pi pi-home', command: () => router.push('/') },
    { label: 'Appointments', icon: 'pi pi-calendar', command: () => router.push('/appointments') },
    { label: 'Doctors', icon: 'pi pi-user', command: () => router.push('/doctors') },
    { label: 'Patients', icon: 'pi pi-users', command: () => router.push('/patients') },
  ]

  if (authStore.isAdmin) {
    items.push({
      label: 'Admin',
      icon: 'pi pi-shield',
      items: [
        { label: 'Users', icon: 'pi pi-id-card', command: () => router.push('/admin/users') },
      ],
    })
  }

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
      <template #end>
        <div class="nav-end">
          <Button label="Profile" icon="pi pi-id-card" text @click="router.push('/profile')" />
          <Button
            label="Logout"
            icon="pi pi-sign-out"
            text
            @click="authStore.clearTokens(); router.push({ name: 'login' })"
          />
        </div>
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

.nav-end {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.app-content {
  max-width: 1200px;
  margin: 2rem auto;
  padding: 0 1.5rem;
}
</style>
