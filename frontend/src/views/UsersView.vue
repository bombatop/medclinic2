<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  activateUser,
  createUser,
  deactivateUser,
  getUsers,
  updateUser,
  type User,
} from '@/api/users'
import { isBlankInput } from '@/utils/validation'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import Button from 'primevue/button'
import Column from 'primevue/column'
import ConfirmDialog from 'primevue/confirmdialog'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

const toast = useToast()
const confirm = useConfirm()

const users = ref<User[]>([])
const totalRecords = ref(0)
const loading = ref(true)
const search = ref('')
const lazyParams = ref({ first: 0, rows: 10 })

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const editingUserId = ref<number | null>(null)

const roleOptions = [
  { label: 'Admin', value: 'ADMIN' },
  { label: 'Doctor', value: 'DOCTOR' },
  { label: 'Receptionist', value: 'RECEPTIONIST' },
]

const form = reactive({
  username: '',
  password: '',
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  role: 'DOCTOR' as 'ADMIN' | 'DOCTOR' | 'RECEPTIONIST',
})

const editForm = reactive({
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
})

const filteredUsers = computed(() => {
  const q = search.value.toLowerCase().trim()
  if (!q) return users.value
  return users.value.filter((u) => {
    const haystack =
      `${u.username} ${u.firstName} ${u.lastName} ${u.email}`.toLowerCase()
    return haystack.includes(q)
  })
})

function getErrorMessage(err: unknown, fallback: string): string {
  const apiErr = err as { response?: { data?: { message?: string } }; message?: string }
  return apiErr.response?.data?.message ?? apiErr.message ?? fallback
}

async function loadUsers() {
  loading.value = true
  try {
    const page = Math.floor(lazyParams.value.first / lazyParams.value.rows)
    const res = await getUsers({ page, size: lazyParams.value.rows })
    users.value = res.content
    totalRecords.value = res.totalElements
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Load failed',
      detail: getErrorMessage(err, 'Unable to load users.'),
    })
  } finally {
    loading.value = false
  }
}

function onPage(event: { first: number; rows: number }) {
  lazyParams.value = { first: event.first, rows: event.rows }
  void loadUsers()
}

function openCreateDialog() {
  dialogMode.value = 'create'
  editingUserId.value = null
  form.username = ''
  form.password = ''
  form.firstName = ''
  form.lastName = ''
  form.email = ''
  form.phone = ''
  form.role = 'DOCTOR'
  dialogVisible.value = true
}

function openEditDialog(user: User) {
  dialogMode.value = 'edit'
  editingUserId.value = user.id
  editForm.firstName = user.firstName
  editForm.lastName = user.lastName
  editForm.email = user.email
  editForm.phone = user.phone ?? ''
  dialogVisible.value = true
}

async function saveUser() {
  if (dialogMode.value === 'create') {
    if (isBlankInput(form.username)) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'Username is required.' })
      return
    }
    if (isBlankInput(form.password) || form.password.length < 6) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'Password must be at least 6 characters.' })
      return
    }
    if (isBlankInput(form.firstName)) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'First name is required.' })
      return
    }
    if (isBlankInput(form.lastName)) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'Last name is required.' })
      return
    }
    if (isBlankInput(form.email)) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'Email is required.' })
      return
    }
  } else {
    if (isBlankInput(editForm.firstName)) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'First name is required.' })
      return
    }
    if (isBlankInput(editForm.lastName)) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'Last name is required.' })
      return
    }
    if (isBlankInput(editForm.email)) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'Email is required.' })
      return
    }
  }

  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      const created = await createUser({
        username: form.username.trim(),
        password: form.password,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || undefined,
        roles: [form.role],
      })
      totalRecords.value += 1
      users.value = [created, ...users.value].slice(0, lazyParams.value.rows)
      toast.add({
        severity: 'success',
        summary: 'User created',
        detail: `${created.username} added.`,
      })
    } else {
      const updated = await updateUser(editingUserId.value!, {
        firstName: editForm.firstName.trim(),
        lastName: editForm.lastName.trim(),
        email: editForm.email.trim(),
        phone: editForm.phone.trim() || undefined,
      })
      const idx = users.value.findIndex((u) => u.id === updated.id)
      if (idx >= 0) users.value[idx] = updated
      toast.add({
        severity: 'success',
        summary: 'User updated',
        detail: `${updated.username} saved.`,
      })
    }
    dialogVisible.value = false
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Save failed',
      detail: getErrorMessage(err, 'Unable to save user.'),
    })
  } finally {
    saving.value = false
  }
}

function confirmToggleActive(user: User) {
  const action = user.active ? 'deactivate' : 'activate'
  confirm.require({
    message: `${user.active ? 'Deactivate' : 'Activate'} ${user.username}?`,
    header: `${user.active ? 'Deactivate' : 'Activate'} User`,
    icon: user.active ? 'pi pi-ban' : 'pi pi-check-circle',
    rejectLabel: 'Cancel',
    acceptLabel: user.active ? 'Deactivate' : 'Activate',
    acceptClass: user.active ? 'p-button-danger' : 'p-button-success',
    accept: () => void performToggleActive(user, action),
  })
}

async function performToggleActive(user: User, action: 'activate' | 'deactivate') {
  try {
    if (action === 'deactivate') {
      await deactivateUser(user.id)
    } else {
      await activateUser(user.id)
    }
    const idx = users.value.findIndex((u) => u.id === user.id)
    if (idx >= 0) users.value[idx] = { ...users.value[idx]!, active: action === 'activate' }
    toast.add({
      severity: 'success',
      summary: `User ${action}d`,
      detail: `${user.username} ${action}d.`,
    })
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: `${action} failed`,
      detail: getErrorMessage(err, `Unable to ${action} user.`),
    })
  }
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value))
}

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <div class="users-page">
    <ConfirmDialog />

    <div class="page-header">
      <div>
        <h1>Users</h1>
        <p class="page-subtitle">Manage user accounts and roles.</p>
      </div>
      <Button label="Add User" icon="pi pi-plus" @click="openCreateDialog" />
    </div>

    <IconField class="search-field">
      <InputIcon class="pi pi-search" />
      <InputText v-model="search" placeholder="Search by username, name, or email..." />
    </IconField>

    <DataTable
      :value="filteredUsers"
      :loading="loading"
      :lazy="true"
      :totalRecords="totalRecords"
      dataKey="id"
      paginator
      :rows="lazyParams.rows"
      :rowsPerPageOptions="[10, 25, 50]"
      stripedRows
      removableSort
      @page="onPage"
    >
      <template #empty>
        <div class="table-empty">No users found.</div>
      </template>

      <Column field="username" header="Username" sortable />

      <Column header="Name" sortable sortField="lastName">
        <template #body="{ data }">
          {{ data.firstName }} {{ data.lastName }}
        </template>
      </Column>

      <Column field="email" header="Email" />

      <Column header="Roles" style="width: 12rem">
        <template #body="{ data }">
          <Tag
            :value="data.roles.join(', ')"
            :severity="data.roles.includes('ADMIN') ? 'warn' : 'info'"
          />
        </template>
      </Column>

      <Column header="Status" sortable sortField="active" style="width: 8rem">
        <template #body="{ data }">
          <Tag
            :value="data.active ? 'Active' : 'Inactive'"
            :severity="data.active ? 'success' : 'danger'"
          />
        </template>
      </Column>

      <Column header="Created" sortable sortField="createdAt">
        <template #body="{ data }">
          {{ formatDate(data.createdAt) }}
        </template>
      </Column>

      <Column style="width: 9rem">
        <template #body="{ data }">
          <div class="row-actions">
            <Button
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              size="small"
              v-tooltip.top="'Edit'"
              @click="openEditDialog(data)"
            />
            <Button
              :icon="data.active ? 'pi pi-ban' : 'pi pi-check-circle'"
              :severity="data.active ? 'danger' : 'success'"
              text
              rounded
              size="small"
              v-tooltip.top="data.active ? 'Deactivate' : 'Activate'"
              @click="confirmToggleActive(data)"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- Create dialog -->
    <Dialog
      v-model:visible="dialogVisible"
      :header="dialogMode === 'create' ? 'New User' : 'Edit User'"
      modal
      :style="{ width: '480px' }"
    >
      <div class="dialog-form">
        <template v-if="dialogMode === 'create'">
          <div class="field">
            <label for="dlg-username">Username *</label>
            <InputText id="dlg-username" v-model="form.username" :disabled="saving" />
          </div>
          <div class="field">
            <label for="dlg-password">Password *</label>
            <Password
              id="dlg-password"
              v-model="form.password"
              :disabled="saving"
              toggle-mask
              :feedback="false"
              input-class="w-full"
              class="w-full"
            />
          </div>
          <div class="field">
            <label for="dlg-role">Role *</label>
            <Select
              id="dlg-role"
              v-model="form.role"
              :options="roleOptions"
              option-label="label"
              option-value="value"
              :disabled="saving"
            />
          </div>
          <div class="field">
            <label for="dlg-firstName">First name *</label>
            <InputText id="dlg-firstName" v-model="form.firstName" :disabled="saving" />
          </div>
          <div class="field">
            <label for="dlg-lastName">Last name *</label>
            <InputText id="dlg-lastName" v-model="form.lastName" :disabled="saving" />
          </div>
          <div class="field">
            <label for="dlg-email">Email *</label>
            <InputText id="dlg-email" v-model="form.email" :disabled="saving" />
          </div>
          <div class="field">
            <label for="dlg-phone">Phone</label>
            <InputText id="dlg-phone" v-model="form.phone" :disabled="saving" />
          </div>
        </template>

        <template v-else>
          <div class="field">
            <label for="dlg-firstName">First name *</label>
            <InputText id="dlg-firstName" v-model="editForm.firstName" :disabled="saving" />
          </div>
          <div class="field">
            <label for="dlg-lastName">Last name *</label>
            <InputText id="dlg-lastName" v-model="editForm.lastName" :disabled="saving" />
          </div>
          <div class="field">
            <label for="dlg-email">Email *</label>
            <InputText id="dlg-email" v-model="editForm.email" :disabled="saving" />
          </div>
          <div class="field">
            <label for="dlg-phone">Phone</label>
            <InputText id="dlg-phone" v-model="editForm.phone" :disabled="saving" />
          </div>
        </template>
      </div>

      <template #footer>
        <Button
          label="Cancel"
          severity="secondary"
          text
          :disabled="saving"
          @click="dialogVisible = false"
        />
        <Button
          :label="dialogMode === 'create' ? 'Create' : 'Save'"
          icon="pi pi-check"
          :loading="saving"
          @click="saveUser"
        />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.users-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
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

.dialog-form .field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.dialog-form .field label {
  font-weight: 500;
}

.dialog-form .field :deep(.p-inputtext),
.dialog-form .field :deep(.p-password),
.dialog-form .field :deep(.p-select) {
  width: 100%;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .search-field :deep(.p-inputtext) {
    max-width: none;
  }
}
</style>
