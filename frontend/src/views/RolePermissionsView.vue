<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useDebounceFn } from '@/composables/useDebounceFn'
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
import Checkbox from 'primevue/checkbox'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import { getApiErrorMessage } from '@/utils/apiError'

const toast = useToast()

const loading = ref(true)
const saving = ref(false)
const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const selectedRoleId = ref<number | null>(null)
const selectedPermissionCodes = ref<string[]>([])
const permissionSearch = ref('')
const debouncedPermissionQuery = ref('')

const applyDebouncedPermissionQuery = useDebounceFn((query: string) => {
  debouncedPermissionQuery.value = query
})

watch(permissionSearch, (v) => {
  applyDebouncedPermissionQuery(v)
})

const roleOptions = computed(() =>
  roles.value
    .filter((role) => role.active)
    .map((role) => ({ label: `${role.name} (${role.code})`, value: role.id })),
)

const filteredPermissions = computed(() => {
  const q = debouncedPermissionQuery.value.trim().toLowerCase()
  if (!q) return permissions.value
  return permissions.value.filter((p) => {
    const hay = `${p.code} ${p.name} ${p.description ?? ''}`.toLowerCase()
    return hay.includes(q)
  })
})

const permissionsTableDisabled = computed(() => loading.value || saving.value || !selectedRoleId.value)

const selectedPermissionCodeSet = computed(() => new Set(selectedPermissionCodes.value))

function togglePermission(code: string, granted: boolean) {
  if (granted) {
    if (!selectedPermissionCodes.value.includes(code)) {
      selectedPermissionCodes.value = [...selectedPermissionCodes.value, code]
    }
  } else {
    selectedPermissionCodes.value = selectedPermissionCodes.value.filter((c) => c !== code)
  }
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
    }
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Load failed',
      detail: getApiErrorMessage(err, 'Unable to load role permissions data.'),
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
      detail: getApiErrorMessage(err, 'Unable to load selected role permissions.'),
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
    toast.add({ severity: 'success', summary: 'Saved', detail: 'Permissions updated.' })
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Save failed',
      detail: getApiErrorMessage(err, 'Unable to update role permissions.'),
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
  <div class="mc-page role-permissions-page">
    <div class="mc-page-header">
      <div>
        <h1>Permissions</h1>
        <p class="mc-page-subtitle">Map business-action permissions to a role.</p>
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

      <div class="field permissions-table-block">
        <label for="permission-filter">Permissions</label>
        <IconField class="mc-search-field">
          <InputIcon class="pi pi-search" />
          <InputText
            id="permission-filter"
            v-model="permissionSearch"
            placeholder="Filter by code or name..."
            :disabled="loading"
          />
        </IconField>

        <DataTable
          :value="filteredPermissions"
          :loading="loading"
          dataKey="id"
          stripedRows
          sort-field="code"
          :sort-order="1"
          removable-sort
          class="permissions-table"
        >
          <template #empty>
            <div class="mc-table-empty">
              <template v-if="!selectedRoleId && !loading">Select a role to assign permissions.</template>
              <template v-else-if="debouncedPermissionQuery.trim() && !filteredPermissions.length">
                No permissions match your filter.
              </template>
              <template v-else>No permissions found.</template>
            </div>
          </template>

          <Column field="code" header="Code" sortable />
          <Column field="name" header="Name" sortable>
            <template #body="{ data }">
              <span class="name-cell">
                {{ data.name }}
                <i
                  v-if="data.description?.trim()"
                  class="pi pi-info-circle name-desc-icon"
                  v-tooltip.top="data.description"
                />
              </span>
            </template>
          </Column>
          <Column header="Granted" style="width: 8rem">
            <template #body="{ data }">
              <Checkbox
                :model-value="selectedPermissionCodeSet.has(data.code)"
                binary
                :disabled="permissionsTableDisabled"
                @update:model-value="(v) => togglePermission(data.code, !!v)"
              />
            </template>
          </Column>
        </DataTable>
      </div>
    </div>
  </div>
</template>

<style scoped>
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

.field :deep(.p-select) {
  width: 100%;
}

.permissions-table-block {
  gap: 0.75rem;
}

.permissions-table {
  width: 100%;
}

.name-cell {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
}

.name-desc-icon {
  font-size: 0.875rem;
  color: var(--p-text-muted-color);
  cursor: help;
}
</style>
