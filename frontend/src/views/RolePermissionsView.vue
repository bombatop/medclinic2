<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  getPermissions,
  getRolePermissions,
  getRoles,
  updateRolePermissions,
  type Permission,
  type Role,
} from '@/api/rbac'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import MultiSelect from 'primevue/multiselect'
import Select from 'primevue/select'

const toast = useToast()

const loading = ref(true)
const saving = ref(false)
const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const selectedRoleId = ref<number | null>(null)
const selectedPermissionCodes = ref<string[]>([])

const permissionOptions = computed(() =>
  permissions.value.map((permission) => ({
    label: `${permission.code} - ${permission.name}`,
    value: permission.code,
  })),
)

const roleOptions = computed(() =>
  roles.value
    .filter((role) => role.active)
    .map((role) => ({ label: `${role.name} (${role.code})`, value: role.id })),
)

function getErrorMessage(err: unknown, fallback: string): string {
  const apiErr = err as { response?: { data?: { message?: string } }; message?: string }
  return apiErr.response?.data?.message ?? apiErr.message ?? fallback
}

async function loadBaseData() {
  loading.value = true
  try {
    const [rolesRes, permissionsRes] = await Promise.all([
      getRoles({ page: 0, size: 200 }),
      getPermissions(),
    ])
    roles.value = rolesRes.content.sort((a, b) => a.code.localeCompare(b.code))
    permissions.value = permissionsRes.sort((a, b) => a.code.localeCompare(b.code))
    if (!selectedRoleId.value && roles.value.length) {
      selectedRoleId.value = roles.value[0]!.id
      await loadRolePermissions()
    }
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Load failed',
      detail: getErrorMessage(err, 'Unable to load role permissions data.'),
    })
  } finally {
    loading.value = false
  }
}

async function loadRolePermissions() {
  if (!selectedRoleId.value) {
    selectedPermissionCodes.value = []
    return
  }
  try {
    const res = await getRolePermissions(selectedRoleId.value)
    selectedPermissionCodes.value = [...res.permissions]
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Load failed',
      detail: getErrorMessage(err, 'Unable to load selected role permissions.'),
    })
  }
}

async function savePermissions() {
  if (!selectedRoleId.value) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Select a role first.' })
    return
  }
  saving.value = true
  try {
    await updateRolePermissions(selectedRoleId.value, selectedPermissionCodes.value)
    toast.add({ severity: 'success', summary: 'Saved', detail: 'Role permissions updated.' })
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Save failed',
      detail: getErrorMessage(err, 'Unable to update role permissions.'),
    })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void loadBaseData()
})

watch(selectedRoleId, () => {
  void loadRolePermissions()
})
</script>

<template>
  <div class="role-permissions-page">
    <div class="page-header">
      <div>
        <h1>Role Permissions</h1>
        <p class="page-subtitle">Map business-action permissions to a role.</p>
      </div>
      <Button label="Save" icon="pi pi-check" :loading="saving" :disabled="loading" @click="savePermissions" />
    </div>

    <div class="card">
      <div class="field">
        <label for="role-select">Role</label>
        <Select
          id="role-select"
          v-model="selectedRoleId"
          :options="roleOptions"
          option-label="label"
          option-value="value"
          :disabled="loading || saving"
          placeholder="Select role"
        />
      </div>

      <div class="field">
        <label for="permissions-select">Permissions</label>
        <MultiSelect
          id="permissions-select"
          v-model="selectedPermissionCodes"
          :options="permissionOptions"
          option-label="label"
          option-value="value"
          display="chip"
          filter
          :disabled="loading || saving || !selectedRoleId"
          placeholder="Select permissions"
          class="w-full"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.role-permissions-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-subtitle {
  color: var(--p-text-muted-color);
}

.card {
  border: 1px solid var(--p-content-border-color);
  border-radius: 0.75rem;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.field :deep(.p-select),
.field :deep(.p-multiselect) {
  width: 100%;
}
</style>
