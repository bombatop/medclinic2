<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'
import { login } from '@/api/auth'
import { isBlankInput } from '@/utils/validation'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import type { ApiError } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const toast = useToast()

const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

async function onSubmit() {
  if (isBlankInput(form.username) || isBlankInput(form.password)) {
    toast.add({
      severity: 'warn',
      summary: 'Validation',
      detail: 'Username and password are required',
    })
    return
  }

  loading.value = true
  try {
    const res = await login({
      username: form.username.trim(),
      password: form.password,
    })
    authStore.setTokens(res.accessToken, res.refreshToken, res.role)
    toast.add({
      severity: 'success',
      summary: 'Welcome',
      detail: `Logged in as ${res.username}`,
    })
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch (err: unknown) {
    const apiErr = err as { response?: { data?: ApiError; status?: number }; message?: string }
    const message =
      apiErr.response?.data?.message ??
      (apiErr.response ? 'Invalid username or password' : 'Network error. Is the backend running?')
    toast.add({
      severity: 'error',
      summary: 'Login failed',
      detail: message,
    })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1>MedClinic</h1>
      <p class="subtitle">Sign in to your account</p>

      <form @submit.prevent="onSubmit" class="login-form">
        <div class="field">
          <label for="username">Username</label>
          <InputText
            id="username"
            v-model="form.username"
            placeholder="Enter username"
            :disabled="loading"
            autocomplete="username"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <Password
            id="password"
            v-model="form.password"
            placeholder="Enter password"
            :disabled="loading"
            :feedback="false"
            toggle-mask
            class="w-full"
            input-class="w-full"
          />
        </div>
        <Button
          type="submit"
          label="Sign in"
          :loading="loading"
          class="w-full"
        />
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.login-card {
  width: 100%;
  max-width: 360px;
  padding: 2rem;
  background: var(--p-surface-card);
  border-radius: var(--p-border-radius);
  box-shadow: var(--p-shadow-2);
}

.login-card h1 {
  text-align: center;
  margin-bottom: 0.25rem;
}

.subtitle {
  text-align: center;
  color: var(--p-text-muted-color);
  margin-bottom: 1.5rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.375rem;
}
</style>
