<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { createRole, deleteRole, getRoles, updateRole, type Role } from '@/api/rbac'
import { useDebounceFn } from '@/composables/useDebounceFn'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Column from 'primevue/column'
import ConfirmDialog from 'primevue/confirmdialog'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'

const toast = useToast()
const confirm = useConfirm()

const roles = ref<Role[]>([])
const loading = ref(true)
const saving = ref(false)
const totalRecords = ref(0)
const search = ref('')
const lazyParams = ref({
  first: 0,
  rows: 20,
  sortField: 'code' as string | null,
  sortOrder: 1 as number,
})

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingRoleId = ref<number | null>(null)

const form = reactive({
  code: '',
  name: '',
  description: '',
  active: true,
})

function getErrorMessage(err: unknown, fallback: string): string {
  const apiErr = err as { response?: { data?: { message?: string } }; message?: string }
  return apiErr.response?.data?.message ?? apiErr.message ?? fallback
}

async function loadRoles() {
  loading.value = true
  try {
    const page = Math.floor(lazyParams.value.first / lazyParams.value.rows)
    const sortField = lazyParams.value.sortField
    const sortOrder = lazyParams.value.sortOrder
    const sort =
      sortField != null && sortOrder !== 0
        ? `${sortField},${sortOrder === 1 ? 'asc' : 'desc'}`
        : undefined
    const res = await getRoles({
      page,
      size: lazyParams.value.rows,
      sort,
      search: search.value.trim() || undefined,
    })
    roles.value = res.content
    totalRecords.value = res.totalElements
  } catch (err: unknown) {
    toast.add({ severity: 'error', summary: 'Load failed', detail: getErrorMessage(err, 'Unable to load roles.') })
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  dialogMode.value = 'create'
  editingRoleId.value = null
  form.code = ''
  form.name = ''
  form.description = ''
  form.active = true
  dialogVisible.value = true
}

function openEditDialog(role: Role) {
  dialogMode.value = 'edit'
  editingRoleId.value = role.id
  form.code = role.code
  form.name = role.name
  form.description = role.description ?? ''
  form.active = role.active
  dialogVisible.value = true
}

async function saveRole() {
  if (!form.name.trim()) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Role name is required.' })
    return
  }
  if (dialogMode.value === 'create' && !form.code.trim()) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Role code is required.' })
    return
  }

  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      await createRole({
        code: form.code.trim(),
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        active: form.active,
      })
      toast.add({ severity: 'success', summary: 'Role created', detail: 'Role has been created.' })
    } else {
      await updateRole(editingRoleId.value!, {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        active: form.active,
      })
      toast.add({ severity: 'success', summary: 'Role updated', detail: 'Role has been updated.' })
    }
    dialogVisible.value = false
    await loadRoles()
  } catch (err: unknown) {
    toast.add({ severity: 'error', summary: 'Save failed', detail: getErrorMessage(err, 'Unable to save role.') })
  } finally {
    saving.value = false
  }
}

function confirmDelete(role: Role) {
  confirm.require({
    message: `Delete role ${role.code}?`,
    header: 'Delete Role',
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Cancel',
    acceptLabel: 'Delete',
    acceptClass: 'p-button-danger',
    accept: () => void performDelete(role),
  })
}

async function performDelete(role: Role) {
  try {
    await deleteRole(role.id)
    toast.add({ severity: 'success', summary: 'Role deleted', detail: `${role.code} deleted.` })
    await loadRoles()
  } catch (err: unknown) {
    toast.add({ severity: 'error', summary: 'Delete failed', detail: getErrorMessage(err, 'Unable to delete role.') })
  }
}

function onPage(event: { first: number; rows: number }) {
  lazyParams.value = {
    ...lazyParams.value,
    first: event.first,
    rows: event.rows,
  }
  void loadRoles()
}

function onSort(event: { sortField?: string; sortOrder?: number }) {
  lazyParams.value = {
    first: 0,
    rows: lazyParams.value.rows,
    sortField: event.sortField ?? null,
    sortOrder: event.sortOrder ?? 0,
  }
  void loadRoles()
}

const debouncedLoadRoles = useDebounceFn(() => loadRoles(), 300)

watch(search, () => {
  lazyParams.value.first = 0
  debouncedLoadRoles()
})

onMounted(() => {
  void loadRoles()
})
</script>

<template>
  <div class="roles-page">
    <ConfirmDialog />

    <div class="page-header">
      <div>
        <h1>Roles</h1>
        <p class="page-subtitle">Create and manage dynamic roles.</p>
      </div>
      <Button label="Add Role" icon="pi pi-plus" @click="openCreateDialog" />
    </div>

    <IconField class="search-field">
      <InputIcon class="pi pi-search" />
      <InputText v-model="search" placeholder="Search by code, name, or description..." />
    </IconField>

    <DataTable
      :value="roles"
      :loading="loading"
      :lazy="true"
      :totalRecords="totalRecords"
      dataKey="id"
      paginator
      :rows="lazyParams.rows"
      :rowsPerPageOptions="[20, 50, 100]"
      :sortField="lazyParams.sortField"
      :sortOrder="lazyParams.sortOrder"
      stripedRows
      removableSort
      @page="onPage"
      @sort="onSort"
    >
      <template #empty>
        <div class="table-empty">No roles found.</div>
      </template>

      <Column field="code" header="Code" sortable sortField="code" />
      <Column field="name" header="Name" sortable sortField="name" />
      <Column header="Description">
        <template #body="{ data }">
          {{ data.description || '-' }}
        </template>
      </Column>
      <Column header="Status" sortable sortField="active">
        <template #body="{ data }">
          <Tag :value="data.active ? 'Active' : 'Inactive'" :severity="data.active ? 'success' : 'danger'" />
        </template>
      </Column>
      <Column header="Type">
        <template #body="{ data }">
          <Tag :value="data.system ? 'System' : 'Custom'" :severity="data.system ? 'warn' : 'info'" />
        </template>
      </Column>
      <Column style="width: 10rem">
        <template #body="{ data }">
          <div class="row-actions">
            <Button icon="pi pi-pencil" text rounded size="small" @click="openEditDialog(data)" />
            <Button
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              size="small"
              :disabled="data.system"
              v-tooltip.top="data.system ? 'System roles cannot be deleted' : 'Delete'"
              @click="confirmDelete(data)"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <Dialog
      v-model:visible="dialogVisible"
      :header="dialogMode === 'create' ? 'Create Role' : 'Edit Role'"
      modal
      :style="{ width: '520px' }"
    >
      <div class="dialog-form">
        <div class="field">
          <label for="role-code">Code *</label>
          <InputText
            id="role-code"
            v-model="form.code"
            :disabled="dialogMode === 'edit' || saving"
            placeholder="e.g. LAB_MANAGER"
          />
        </div>
        <div class="field">
          <label for="role-name">Name *</label>
          <InputText id="role-name" v-model="form.name" :disabled="saving" />
        </div>
        <div class="field">
          <label for="role-description">Description</label>
          <Textarea id="role-description" v-model="form.description" rows="3" :disabled="saving" />
        </div>
        <div class="field-checkbox">
          <input id="role-active" v-model="form.active" type="checkbox" :disabled="saving" />
          <label for="role-active">Active</label>
        </div>
      </div>

      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="saving" @click="dialogVisible = false" />
        <Button :label="dialogMode === 'create' ? 'Create' : 'Save'" :loading="saving" @click="saveRole" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.roles-page {
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

.search-field :deep(.p-inputtext) {
  width: 100%;
  max-width: 400px;
}

.row-actions {
  display: flex;
  gap: 0.25rem;
}

.table-empty {
  text-align: center;
  padding: 2rem;
  color: var(--p-text-muted-color);
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.field-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.field :deep(.p-inputtext),
.field :deep(.p-textarea) {
  width: 100%;
}
</style>
