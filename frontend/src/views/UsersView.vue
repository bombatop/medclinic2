<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  lazySortStateFromDataTable,
  pageFromLazyFirst,
  springSortFromPrime,
  useDebouncedSearchReload,
} from '@/composables/useLazyPrimeTable'
import { getLinkedAuthUserIds } from '@/api/doctors'
import {
  activateUser,
  createUser,
  deactivateUser,
  getUsers,
  getUserRoles,
  updateUserRoles,
  updateUser,
  type User,
} from '@/api/users'
import { getRoles as getRbacRoles, type Role as RbacRole } from '@/api/rbac'
import { DIALOG_WIDTH_DEFAULT } from '@/constants/ui'
import { getApiErrorMessage } from '@/utils/apiError'
import { formatDate } from '@/utils/formatting'
import { isBlankInput } from '@/utils/validation'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import Button from 'primevue/button'
import Column from 'primevue/column'
import ConfirmDialog from 'primevue/confirmdialog'
import DataTable from 'primevue/datatable'
import type { DataTableSortEvent } from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import MultiSelect from 'primevue/multiselect'
import Password from 'primevue/password'
import Tag from 'primevue/tag'

const toast = useToast()
const confirm = useConfirm()

const users = ref<User[]>([])
const totalRecords = ref(0)
const loading = ref(true)
const loadingRoles = ref(false)
const search = ref('')
const linkedAuthUserIds = ref<Set<number>>(new Set())
const lazyParams = ref({
  first: 0,
  rows: 10,
  sortField: 'lastName' as string | null,
  sortOrder: 1 as number,
})

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const editingUserId = ref<number | null>(null)

const roleOptions = ref<Array<{ label: string; value: string }>>([])

const form = reactive({
  username: '',
  password: '',
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  roles: ['DOCTOR'] as string[],
})

const editForm = reactive({
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  roles: [] as string[],
})

function doctorMissingClinicProfile(user: User): boolean {
  return user.roles.includes('DOCTOR') && !linkedAuthUserIds.value.has(user.id)
}

function userHasNoRoles(user: User): boolean {
  return user.roles.length === 0
}

const noRolesWarningDetail =
  'This user has no roles. Consider assigning at least one role, or deactivate the account if they should not access the system.'

async function loadUsers() {
  loading.value = true
  try {
    const page = pageFromLazyFirst(lazyParams.value.first, lazyParams.value.rows)
    const sort = springSortFromPrime(lazyParams.value.sortField, lazyParams.value.sortOrder)
    const [res, linkedIds] = await Promise.all([
      getUsers({
        page,
        size: lazyParams.value.rows,
        sort,
        search: search.value.trim() || undefined,
      }),
      getLinkedAuthUserIds().catch(() => [] as number[]),
    ])
    linkedAuthUserIds.value = new Set(linkedIds)
    users.value = res.content
    totalRecords.value = res.totalElements
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Load failed',
      detail: getApiErrorMessage(err, 'Unable to load users.'),
    })
  } finally {
    loading.value = false
  }
}

async function loadRoleOptions() {
  loadingRoles.value = true
  try {
    const res = await getRbacRoles({ page: 0, size: 200 })
    const roles = res.content
      .filter((role) => role.active)
      .sort((a, b) => a.code.localeCompare(b.code))
      .map((role: RbacRole) => ({ label: `${role.name} (${role.code})`, value: role.code }))
    roleOptions.value = roles
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Roles load failed',
      detail: getApiErrorMessage(err, 'Unable to load roles.'),
    })
  } finally {
    loadingRoles.value = false
  }
}

function onPage(event: { first: number; rows: number }) {
  lazyParams.value = {
    ...lazyParams.value,
    first: event.first,
    rows: event.rows,
  }
  void loadUsers()
}

function onSort(event: DataTableSortEvent) {
  const { sortField, sortOrder } = lazySortStateFromDataTable(event)
  lazyParams.value = {
    first: 0,
    rows: lazyParams.value.rows,
    sortField,
    sortOrder,
  }
  void loadUsers()
}

useDebouncedSearchReload(search, lazyParams, loadUsers)

function openCreateDialog() {
  dialogMode.value = 'create'
  editingUserId.value = null
  form.username = ''
  form.password = ''
  form.firstName = ''
  form.lastName = ''
  form.email = ''
  form.phone = ''
  form.roles = ['DOCTOR']
  dialogVisible.value = true
}

async function openEditDialog(user: User) {
  dialogMode.value = 'edit'
  editingUserId.value = user.id
  editForm.firstName = user.firstName
  editForm.lastName = user.lastName
  editForm.email = user.email
  editForm.phone = user.phone ?? ''
  editForm.roles = user.roles
  try {
    const current = await getUserRoles(user.id)
    editForm.roles = [...current.roles]
  } catch {
    // fallback to table data if detailed role fetch fails
  }
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
    if (!form.roles.length) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'At least one role is required.' })
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
    if (!editForm.roles.length) {
      toast.add({ severity: 'warn', summary: 'Validation', detail: 'At least one role is required.' })
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
        roles: form.roles,
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
      const updatedRoles = await updateUserRoles(updated.id, editForm.roles)
      updated.roles = updatedRoles.roles
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
      detail: getApiErrorMessage(err, 'Unable to save user.'),
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
      detail: getApiErrorMessage(err, `Unable to ${action} user.`),
    })
  }
}

onMounted(() => {
  void loadRoleOptions()
  void loadUsers()
})
</script>

<template>
  <div class="mc-page users-page">
    <ConfirmDialog />

    <div class="mc-page-header">
      <div>
        <h1>Users</h1>
        <p class="mc-page-subtitle">Manage user accounts and roles.</p>
      </div>
      <Button label="Add User" icon="pi pi-plus" @click="openCreateDialog" />
    </div>

    <IconField class="mc-search-field">
      <InputIcon class="pi pi-search" />
      <InputText v-model="search" placeholder="Search by username, name, or email..." />
    </IconField>

    <DataTable
      :value="users"
      :loading="loading"
      :lazy="true"
      :totalRecords="totalRecords"
      dataKey="id"
      paginator
      :rows="lazyParams.rows"
      :rowsPerPageOptions="[10, 25, 50]"
      :sortField="lazyParams.sortField ?? undefined"
      :sortOrder="lazyParams.sortOrder"
      stripedRows
      removableSort
      @page="onPage"
      @sort="onSort"
    >
      <template #empty>
        <div class="mc-table-empty">No users found.</div>
      </template>

      <Column field="username" header="Username" sortable sortField="username" />

      <Column header="Name" sortable sortField="lastName">
        <template #body="{ data }">
          {{ data.firstName }} {{ data.lastName }}
        </template>
      </Column>

      <Column field="email" header="Email" />

      <Column header="Roles" style="width: 14rem">
        <template #body="{ data }">
          <Tag
            v-if="userHasNoRoles(data)"
            value="No roles"
            severity="warn"
            v-tooltip.top="noRolesWarningDetail"
          />
          <Tag
            v-else
            :value="data.roles.join(', ')"
            :severity="data.roles.includes('ADMIN') ? 'warn' : 'info'"
          />
        </template>
      </Column>

      <Column header="Clinic" style="width: 10rem">
        <template #body="{ data }">
          <Tag
            v-if="doctorMissingClinicProfile(data)"
            value="No clinic profile"
            severity="warn"
            v-tooltip.top="'Doctor role but no linked clinic profile. Use Doctor profiles → Link doctor profile.'"
          />
          <span v-else-if="data.roles.includes('DOCTOR')" class="text-muted">Linked</span>
          <span v-else class="text-muted">—</span>
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
          <div class="mc-row-actions">
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
      :style="{ width: DIALOG_WIDTH_DEFAULT }"
    >
      <div class="mc-dialog-form dialog-form">
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
            <label for="dlg-role">Roles *</label>
            <MultiSelect
              id="dlg-role"
              v-model="form.roles"
              :options="roleOptions"
              option-label="label"
              option-value="value"
              :disabled="saving || loadingRoles"
              display="chip"
              placeholder="Select roles"
              class="w-full"
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
          <div class="field">
            <label for="edit-dlg-role">Roles *</label>
            <Message
              v-if="editForm.roles.length === 0"
              severity="warn"
              :closable="false"
              class="roles-warning-message"
            >
              {{ noRolesWarningDetail }}
            </Message>
            <MultiSelect
              id="edit-dlg-role"
              v-model="editForm.roles"
              :options="roleOptions"
              option-label="label"
              option-value="value"
              :disabled="saving || loadingRoles"
              display="chip"
              placeholder="Select roles"
              class="w-full"
            />
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
.text-muted {
  color: var(--p-text-muted-color);
  font-size: 0.875rem;
}

.dialog-form .field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.dialog-form .field label {
  font-weight: 500;
}

.roles-warning-message {
  margin-bottom: 0.5rem;
}

.roles-warning-message :deep(.p-message-text) {
  font-size: 0.875rem;
}
</style>
