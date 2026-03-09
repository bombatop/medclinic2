<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { ApiError, CurrentUser } from '@/api/auth'
import { changePassword, getCurrentUser } from '@/api/auth'
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
const savingPassword = ref(false)

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const fullName = computed(() => {
  if (!user.value) {
    return ''
  }

  return [user.value.firstName, user.value.lastName].filter(Boolean).join(' ') || user.value.username
})

const createdAtLabel = computed(() => {
  if (!user.value?.createdAt) {
    return 'N/A'
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(user.value.createdAt))
})

function getErrorMessage(err: unknown, fallback: string): string {
  const apiErr = err as { response?: { data?: ApiError }; message?: string }
  return apiErr.response?.data?.message ?? apiErr.message ?? fallback
}

async function loadProfile() {
  loadingProfile.value = true
  try {
    user.value = await getCurrentUser()
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

function resetPasswordForm() {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

async function onChangePassword() {
  const currentPassword = passwordForm.currentPassword
  const newPassword = passwordForm.newPassword
  const confirmPassword = passwordForm.confirmPassword

  if (isBlankInput(currentPassword)) {
    toast.add({
      severity: 'warn',
      summary: 'Validation',
      detail: 'Current password is required.',
    })
    return
  }

  if (isBlankInput(newPassword)) {
    toast.add({
      severity: 'warn',
      summary: 'Validation',
      detail: 'New password is required.',
    })
    return
  }

  if (isBlankInput(confirmPassword)) {
    toast.add({
      severity: 'warn',
      summary: 'Validation',
      detail: 'Please confirm your new password.',
    })
    return
  }

  if (newPassword !== confirmPassword) {
    toast.add({
      severity: 'warn',
      summary: 'Validation',
      detail: 'New password and confirmation must match.',
    })
    return
  }

  if (newPassword.length < PASSWORD_MIN_LENGTH) {
    toast.add({
      severity: 'warn',
      summary: 'Validation',
      detail: `New password must be at least ${PASSWORD_MIN_LENGTH} characters long.`,
    })
    return
  }

  if (newPassword.length > PASSWORD_MAX_LENGTH) {
    toast.add({
      severity: 'warn',
      summary: 'Validation',
      detail: `New password must be at most ${PASSWORD_MAX_LENGTH} characters long.`,
    })
    return
  }

  if (currentPassword === newPassword) {
    toast.add({
      severity: 'warn',
      summary: 'Validation',
      detail: 'New password must be different from your current password.',
    })
    return
  }

  savingPassword.value = true
  try {
    await changePassword({
      currentPassword,
      newPassword,
    })
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
        <p class="page-subtitle">View your account details and update your password.</p>
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
              <label>Full name</label>
              <InputText :model-value="fullName" readonly />
            </div>
            <div class="field">
              <label>Username</label>
              <InputText :model-value="user.username" readonly />
            </div>
            <div class="field">
              <label>Email</label>
              <InputText :model-value="user.email || 'N/A'" readonly />
            </div>
            <div class="field">
              <label>Phone</label>
              <InputText :model-value="user.phone || 'N/A'" readonly />
            </div>
            <div class="field">
              <label>Role</label>
              <div class="tag-row">
                <Tag :value="user.role" />
              </div>
            </div>
            <div class="field">
              <label>Status</label>
              <div class="tag-row">
                <Tag :value="user.active ? 'Active' : 'Inactive'" :severity="user.active ? 'success' : 'danger'" />
              </div>
            </div>
            <div class="field field-full">
              <label>Created at</label>
              <InputText :model-value="createdAtLabel" readonly />
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

  .field-full {
    grid-column: auto;
  }

  .password-actions {
    flex-direction: column-reverse;
  }
}
</style>
