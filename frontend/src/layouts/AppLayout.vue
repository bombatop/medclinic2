<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getCurrentUser } from '@/api/auth'
import Menubar from 'primevue/menubar'
import Button from 'primevue/button'
import type { MenuItem } from 'primevue/menuitem'

const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  getCurrentUser().catch(() => { /* 401 handled by interceptor */ })
})

const menuItems = computed<MenuItem[]>(() => {
  const items: MenuItem[] = [
    { label: 'Clinic dashboard', icon: 'pi pi-home', command: () => router.push('/') },
    { label: 'Appointments', icon: 'pi pi-calendar', command: () => router.push('/appointments') },
    { label: 'Doctor profiles', icon: 'pi pi-user', command: () => router.push('/doctors') },
    { label: 'Patients', icon: 'pi pi-users', command: () => router.push('/patients') },
  ]

  if (authStore.canAccessAdmin) {
    items.push({ separator: true })
    const adminNav: MenuItem[] = [
      {
        label: 'Admin dashboard',
        icon: 'pi pi-th-large',
        command: () => router.push('/admin'),
      },
    ]
    if (authStore.hasPermission('users.read_all')) {
      adminNav.push({ label: 'Users', icon: 'pi pi-id-card', command: () => router.push('/admin/users') })
    }
    if (authStore.canManageRbac) {
      adminNav.push(
        { label: 'Roles', icon: 'pi pi-sitemap', command: () => router.push('/admin/roles') },
        {
          label: 'Permissions',
          icon: 'pi pi-lock',
          command: () => router.push('/admin/role-permissions'),
        },
        { label: 'Audit', icon: 'pi pi-history', command: () => router.push('/admin/rbac-audit') },
      )
    }
    if (adminNav.length > 0) {
      adminNav[0] = { ...adminNav[0], class: 'nav-admin-start' }
      const lastIdx = adminNav.length - 1
      const last = adminNav[lastIdx]!
      const endClass = 'nav-admin-end'
      adminNav[lastIdx] = {
        ...last,
        class: last.class ? `${last.class} ${endClass}` : endClass,
      }
      items.push(...adminNav)
    }
  }

  return items
})
</script>

<template>
  <div class="app-layout">
    <Menubar :model="menuItems" class="app-menubar">
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
            @click="authStore.clearTokens().then(() => router.push({ name: 'login' }))"
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

/* Clinic left; admin block (from .nav-admin-start) pushed to the right */
.app-menubar {
  width: 100%;
}

.app-menubar :deep(.p-menubar-root-list) {
  flex: 1 1 auto;
  display: flex;
  flex-wrap: wrap;
  gap: 0.125rem 0;
  justify-content: flex-start;
  align-items: center;
  min-width: 0;
}

.app-menubar :deep(li.nav-admin-start) {
  margin-left: auto;
}

/* Breathing room before Profile / Logout (~one text button width) */
.app-menubar :deep(li.nav-admin-end) {
  /* margin-right: 2.75rem; */
}

@media (max-width: 960px) {
  .app-menubar :deep(li.nav-admin-end) {
    margin-right: 1.25rem;
  }
}
</style>
