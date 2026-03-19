<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { ApiError, CurrentUser } from '@/api/auth'
import { changePassword, getCurrentUser, updateProfile } from '@/api/auth'
import { isBlankInput } from '@/utils/validation'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Tag from 'primevue/tag'

const toast = useToast()

const PASSWORD_MIN_LENGTH = 6
const PASSWORD_MAX_LENGTH = 100

const user = ref<CurrentUser | null>(null)
const loadingProfile = ref(true)
const savingProfile = ref(false)
const savingPassword = ref(false)
const editing = ref(false)

const profileForm = reactive({
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const createdAtLabel = computed(() => {
  if (!user.value?.createdAt) return 'N/A'
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(user.value.createdAt))
})

const displayRoles = computed(() => {
  if (!user.value) return []
  return Array.isArray(user.value.roles) ? user.value.roles : []
})

const profileDirty = computed(() => {
  if (!user.value) return false
  return (
    profileForm.firstName !== user.value.firstName ||
    profileForm.lastName !== user.value.lastName ||
    profileForm.email !== user.value.email ||
    profileForm.phone !== (user.value.phone ?? '')
  )
})

function getErrorMessage(err: unknown, fallback: string): string {
  const apiErr = err as { response?: { data?: ApiError }; message?: string }
  return apiErr.response?.data?.message ?? apiErr.message ?? fallback
}

function syncFormFromUser() {
  if (!user.value) return
  profileForm.firstName = user.value.firstName
  profileForm.lastName = user.value.lastName
  profileForm.email = user.value.email
  profileForm.phone = user.value.phone ?? ''
}

async function loadProfile() {
  loadingProfile.value = true
  try {
    user.value = await getCurrentUser()
    syncFormFromUser()
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Profile unavailable',
      detail: getErrorMessage(err, 'Unable to load your profile right now.'),
    })
  } finally {
    loadingProfile.value = false
  }
}

function startEditing() {
  syncFormFromUser()
  editing.value = true
}

function cancelEditing() {
  syncFormFromUser()
  editing.value = false
}

async function saveProfile() {
  if (isBlankInput(profileForm.firstName)) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'First name is required.' })
    return
  }
  if (isBlankInput(profileForm.lastName)) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Last name is required.' })
    return
  }
  if (isBlankInput(profileForm.email)) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Email is required.' })
    return
  }

  savingProfile.value = true
  try {
    user.value = await updateProfile({
      firstName: profileForm.firstName.trim(),
      lastName: profileForm.lastName.trim(),
      email: profileForm.email.trim(),
      phone: profileForm.phone.trim() || null,
    })
    syncFormFromUser()
    editing.value = false
    toast.add({
      severity: 'success',
      summary: 'Profile updated',
      detail: 'Your profile has been saved.',
    })
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Update failed',
      detail: getErrorMessage(err, 'Unable to save your profile right now.'),
    })
  } finally {
    savingProfile.value = false
  }
}

function resetPasswordForm() {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

async function onChangePassword() {
  const { currentPassword, newPassword, confirmPassword } = passwordForm

  if (isBlankInput(currentPassword)) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Current password is required.' })
    return
  }
  if (isBlankInput(newPassword)) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'New password is required.' })
    return
  }
  if (isBlankInput(confirmPassword)) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Please confirm your new password.' })
    return
  }
  if (newPassword !== confirmPassword) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'New password and confirmation must match.' })
    return
  }
  if (newPassword.length < PASSWORD_MIN_LENGTH) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: `New password must be at least ${PASSWORD_MIN_LENGTH} characters.` })
    return
  }
  if (newPassword.length > PASSWORD_MAX_LENGTH) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: `New password must be at most ${PASSWORD_MAX_LENGTH} characters.` })
    return
  }
  if (currentPassword === newPassword) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'New password must differ from your current one.' })
    return
  }

  savingPassword.value = true
  try {
    await changePassword({ currentPassword, newPassword })
    resetPasswordForm()
    toast.add({
      severity: 'success',
      summary: 'Password updated',
      detail: 'Your password has been changed successfully.',
    })
  } catch (err: unknown) {
    toast.add({
      severity: 'error',
      summary: 'Password update failed',
      detail: getErrorMessage(err, 'Unable to change your password right now.'),
    })
  } finally {
    savingPassword.value = false
  }
}

onMounted(() => {
  void loadProfile()
})
</script>

<template>
  <div class="profile-page">
    <div class="page-header">
      <div>
        <h1>My Profile</h1>
        <p class="page-subtitle">View and edit your account details.</p>
      </div>
      <Button
        label="Refresh"
        icon="pi pi-refresh"
        severity="secondary"
        text
        :loading="loadingProfile"
        @click="loadProfile"
      />
    </div>

    <div class="profile-grid">
      <Card>
        <template #title>Account Details</template>
        <template #content>
          <div v-if="loadingProfile" class="profile-state">
            <i class="pi pi-spin pi-spinner" aria-hidden="true" />
            <span>Loading your profile...</span>
          </div>

          <div v-else-if="user" class="details-grid">
            <div class="field">
              <label for="firstName">First name</label>
              <InputText
                id="firstName"
                v-model="profileForm.firstName"
                :readonly="!editing"
                :disabled="savingProfile"
              />
            </div>
            <div class="field">
              <label for="lastName">Last name</label>
              <InputText
                id="lastName"
                v-model="profileForm.lastName"
                :readonly="!editing"
                :disabled="savingProfile"
              />
            </div>
            <div class="field">
              <label for="email">Email</label>
              <InputText
                id="email"
                v-model="profileForm.email"
                :readonly="!editing"
                :disabled="savingProfile"
              />
            </div>
            <div class="field">
              <label for="phone">Phone</label>
              <InputText
                id="phone"
                v-model="profileForm.phone"
                :readonly="!editing"
                :disabled="savingProfile"
                placeholder="Not set"
              />
            </div>
            <div class="field">
              <label>Username</label>
              <InputText :model-value="user.username" readonly />
            </div>
            <div class="field">
              <label>Roles</label>
              <div class="tag-row">
                <Tag v-for="roleValue in displayRoles" :key="roleValue" :value="roleValue" />
              </div>
            </div>
            <div class="field">
              <label>Status</label>
              <div class="tag-row">
                <Tag :value="user.active ? 'Active' : 'Inactive'" :severity="user.active ? 'success' : 'danger'" />
              </div>
            </div>
            <div class="field">
              <label>Created at</label>
              <InputText :model-value="createdAtLabel" readonly />
            </div>
            <div class="field-full profile-actions">
              <template v-if="editing">
                <Button
                  label="Cancel"
                  severity="secondary"
                  text
                  :disabled="savingProfile"
                  @click="cancelEditing"
                />
                <Button
                  label="Save"
                  icon="pi pi-check"
                  :loading="savingProfile"
                  :disabled="!profileDirty"
                  @click="saveProfile"
                />
              </template>
              <Button
                v-else
                label="Edit"
                icon="pi pi-pencil"
                severity="secondary"
                outlined
                @click="startEditing"
              />
            </div>
          </div>

          <div v-else class="profile-state">
            <i class="pi pi-exclamation-circle" aria-hidden="true" />
            <span>Profile data could not be loaded.</span>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>Change Password</template>
        <template #content>
          <form class="password-form" @submit.prevent="onChangePassword">
            <div class="field">
              <label for="currentPassword">Current password</label>
              <Password
                id="currentPassword"
                v-model="passwordForm.currentPassword"
                :feedback="false"
                toggle-mask
                autocomplete="current-password"
                input-class="w-full"
                class="w-full"
                :disabled="savingPassword"
              />
            </div>

            <div class="field">
              <label for="newPassword">New password</label>
              <Password
                id="newPassword"
                v-model="passwordForm.newPassword"
                toggle-mask
                autocomplete="new-password"
                input-class="w-full"
                class="w-full"
                :disabled="savingPassword"
              />
            </div>

            <div class="field">
              <label for="confirmPassword">Confirm new password</label>
              <Password
                id="confirmPassword"
                v-model="passwordForm.confirmPassword"
                :feedback="false"
                toggle-mask
                autocomplete="new-password"
                input-class="w-full"
                class="w-full"
                :disabled="savingPassword"
              />
            </div>

            <div class="password-actions">
              <Button
                type="button"
                label="Clear"
                severity="secondary"
                text
                :disabled="savingPassword"
                @click="resetPasswordForm"
              />
              <Button
                type="submit"
                label="Update Password"
                icon="pi pi-lock"
                :loading="savingPassword"
              />
            </div>
          </form>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
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

.profile-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

.details-grid,
.password-form {
  display: grid;
  gap: 1rem;
}

.details-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.field label {
  font-weight: 500;
}

.field :deep(.p-inputtext),
.field :deep(.p-password),
.field :deep(.p-password-input) {
  width: 100%;
}

.field-full {
  grid-column: 1 / -1;
}

.tag-row {
  min-height: 2.625rem;
  display: flex;
  align-items: center;
  gap: 0.375rem;
  flex-wrap: wrap;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  grid-column: 1 / -1;
}

.password-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.profile-state {
  min-height: 12rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  color: var(--p-text-muted-color);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .details-grid {
    grid-template-columns: 1fr;
  }

  .field-full,
  .profile-actions {
    grid-column: auto;
  }

  .password-actions {
    flex-direction: column-reverse;
  }
}
</style>
