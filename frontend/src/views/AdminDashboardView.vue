<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getUsers } from '@/api/users'
import { getRoles, getRbacAuditLogs } from '@/api/rbac'
import Card from 'primevue/card'

const authStore = useAuthStore()

const usersCount = ref<number | null>(null)
const rolesCount = ref<number | null>(null)
const auditCount = ref<number | null>(null)

const showUsersCard = computed(() => authStore.hasPermission('users.read_all'))
const showRbacCards = computed(() => authStore.canManageRbac)

async function loadCounts() {
  try {
    const promises: Promise<void>[] = []
    if (showUsersCard.value) {
      promises.push(
        getUsers({ page: 0, size: 1 }).then((r) => {
          usersCount.value = r.totalElements
        }),
      )
    }
    if (showRbacCards.value) {
      promises.push(
        getRoles({ page: 0, size: 1 }).then((r) => {
          rolesCount.value = r.totalElements
        }),
        getRbacAuditLogs({ page: 0, size: 1 }).then((r) => {
          auditCount.value = r.totalElements
        }),
      )
    }
    await Promise.all(promises)
  } catch {
    if (showUsersCard.value) usersCount.value = 0
    if (showRbacCards.value) {
      rolesCount.value = 0
      auditCount.value = 0
    }
  }
}

onMounted(() => {
  void loadCounts()
})
</script>

<template>
  <div>
    <h1>Admin dashboard</h1>
    <p class="page-lead">Users, roles, permissions, and audit.</p>

    <div v-if="!showUsersCard && !showRbacCards" class="empty-admin">
      <p>You don’t have access to any admin tools. Contact an administrator if you need access.</p>
    </div>

    <div v-else class="dashboard-grid">
      <router-link v-if="showUsersCard" to="/admin/users" class="dashboard-card-link">
        <Card class="dashboard-card">
          <template #title>
            <span class="card-title">
              <i class="pi pi-id-card" />
              Users
            </span>
          </template>
          <template #content>
            <p class="card-desc">Accounts, roles assignment, activate/deactivate.</p>
            <p v-if="usersCount !== null" class="card-count">{{ usersCount }} users</p>
          </template>
        </Card>
      </router-link>

      <router-link v-if="showRbacCards" to="/admin/roles" class="dashboard-card-link">
        <Card class="dashboard-card">
          <template #title>
            <span class="card-title">
              <i class="pi pi-sitemap" />
              Roles
            </span>
          </template>
          <template #content>
            <p class="card-desc">Create and manage roles (baseline and custom).</p>
            <p v-if="rolesCount !== null" class="card-count">{{ rolesCount }} roles</p>
          </template>
        </Card>
      </router-link>

      <router-link v-if="showRbacCards" to="/admin/role-permissions" class="dashboard-card-link">
        <Card class="dashboard-card">
          <template #title>
            <span class="card-title">
              <i class="pi pi-lock" />
              Permissions
            </span>
          </template>
          <template #content>
            <p class="card-desc">Map permissions to each role.</p>
          </template>
        </Card>
      </router-link>

      <router-link v-if="showRbacCards" to="/admin/rbac-audit" class="dashboard-card-link">
        <Card class="dashboard-card">
          <template #title>
            <span class="card-title">
              <i class="pi pi-history" />
              Audit
            </span>
          </template>
          <template #content>
            <p class="card-desc">Role and permission change history.</p>
            <p v-if="auditCount !== null" class="card-count">{{ auditCount }} log entries</p>
          </template>
        </Card>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.page-lead {
  margin: 0.25rem 0 0;
  color: var(--p-text-muted-color);
  font-size: 0.95rem;
}

.empty-admin {
  margin-top: 1.5rem;
  padding: 1.5rem;
  border-radius: var(--p-border-radius-md);
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  color: var(--p-text-muted-color);
}

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
  height: 100%;
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
