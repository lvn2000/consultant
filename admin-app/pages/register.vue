<template>
  <div class="login-container">
    <form @submit.prevent="onRegister">
      <div class="login-header">
        <h2>{{ $t('auth.registerTitle') }}</h2>
        <LocaleSwitcher />
      </div>
      <div>
        <label for="login">{{ $t('auth.registerLogin') }}</label>
        <input id="login" v-model="form.login" type="text" required />
      </div>
      <div>
        <label for="email">{{ $t('auth.registerEmail') }}</label>
        <input id="email" v-model="form.email" type="email" required />
      </div>
      <div>
        <label for="name">{{ $t('auth.registerName') }}</label>
        <input id="name" v-model="form.name" type="text" required />
      </div>
      <div>
        <label for="phone">{{ $t('auth.registerPhone') }}</label>
        <input id="phone" v-model="form.phone" type="tel" />
      </div>
      <div>
        <label for="role">{{ $t('auth.registerRole') }}</label>
        <select id="role" v-model="form.role" required>
          <option value="client">{{ $t('auth.registerRoleClient') }}</option>
          <option value="specialist">{{ $t('auth.registerRoleSpecialist') }}</option>
          <option value="admin">{{ $t('auth.registerRoleAdmin') }}</option>
        </select>
      </div>
      <div>
        <label for="password">{{ $t('auth.registerPassword') }}</label>
        <input id="password" v-model="form.password" type="password" required />
      </div>
      <div>
        <label for="confirmPassword">{{ $t('auth.registerConfirmPassword') }}</label>
        <input id="confirmPassword" v-model="form.confirmPassword" type="password" required />
      </div>
      <button type="submit" :disabled="loading">
        {{ loading ? $t('common.loading') : $t('auth.registerButton') }}
      </button>
      <div v-if="error" class="error">{{ error }}</div>
      <div v-if="success" class="success">{{ $t('auth.registerSuccess') }}</div>
      <div class="register-link">
        {{ $t('auth.alreadyHaveAccount') }}
        <NuxtLink to="/login">{{ $t('auth.loginLink') }}</NuxtLink>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { registerRequest, adminRegisterRequest } from '../composables/useRegister'

const { t } = useI18n()
const router = useRouter()

const form = reactive({
  login: '',
  email: '',
  name: '',
  phone: '',
  role: 'client',
  password: '',
  confirmPassword: '',
})

const error = ref('')
const success = ref(false)
const loading = ref(false)

/**
 * Determine whether the current session is an admin that can use the
 * admin-only endpoint. When the page is opened by a non-authenticated visitor
 * (or by a non-admin user) the public /auth/register endpoint is used instead.
 */
const isAdminSession = (): boolean => {
  const role = sessionStorage.getItem('role')
  return role?.toLowerCase() === 'admin'
}

const onRegister = async () => {
  error.value = ''
  success.value = false

  if (
    !form.login ||
    !form.email ||
    !form.name ||
    !form.password ||
    !form.confirmPassword
  ) {
    error.value = t('auth.pleaseEnterAllFields')
    return
  }

  if (form.password !== form.confirmPassword) {
    error.value = t('auth.passwordsDoNotMatch')
    return
  }

  loading.value = true

  const params = {
    login: form.login,
    email: form.email,
    password: form.password,
    name: form.name,
    phone: form.phone || undefined,
    role: form.role,
  }

  if (isAdminSession()) {
    // Admin-created account via the privileged endpoint
    const result = await adminRegisterRequest(params)
    loading.value = false
    if (result.success) {
      success.value = true
    } else {
      error.value = result.error || t('auth.registerFailed')
    }
  } else {
    // Public self-registration (only client/specialist allowed)
    const result = await registerRequest(params)
    loading.value = false
    if (result.success) {
      success.value = true
      localStorage.setItem('admin_session', '1')
      router.push('/main')
    } else {
      error.value = result.error || t('auth.registerFailed')
    }
  }
}
</script>

<style scoped>
.login-container {
  max-width: 400px;
  margin: 60px auto;
  padding: 2rem;
  border: 1px solid #ccc;
  border-radius: 8px;
  background: #fff;
}
.login-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.login-header h2 {
  margin: 0;
}
form > div {
  margin-bottom: 1rem;
}
label {
  display: block;
  margin-bottom: 0.5rem;
}
input,
select {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
}
button {
  width: 100%;
  padding: 0.75rem;
  background: #007bff;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
}
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.error {
  color: red;
  margin-top: 1rem;
}
.success {
  color: green;
  margin-top: 1rem;
}
.register-link {
  margin-top: 1rem;
  text-align: center;
  font-size: 0.9rem;
}
.register-link a {
  color: #007bff;
  text-decoration: none;
}
.register-link a:hover {
  text-decoration: underline;
}
</style>
