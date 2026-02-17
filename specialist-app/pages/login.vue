<template>
  <div class="login-container">
    <form @submit.prevent="onLogin">
      <div class="login-header">
        <h2>{{ $t('auth.loginTitle') }}</h2>
        <LocaleSwitcher />
      </div>
      <div>
        <label for="login">{{ $t('auth.username') }}</label>
        <input id="login" v-model="login" type="text" required />
      </div>
      <div>
        <label for="password">{{ $t('auth.password') }}</label>
        <input id="password" v-model="password" type="password" required />
      </div>
      <button type="submit">{{ $t('auth.loginButton') }}</button>
      <div v-if="error" class="error">{{ error }}</div>
      <div class="register-link">
        {{ $t('auth.registerLink') }}
        <NuxtLink to="/register">{{ $t('auth.registerButton') }}</NuxtLink>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { loginRequest } from '../composables/useLogin'

const { t } = useI18n()

const login = ref('')
const password = ref('')
const error = ref('')
const router = useRouter()
const route = useRoute()

const onLogin = async () => {
  error.value = ''
  if (!login.value || !password.value) {
    error.value = t('auth.pleaseEnterCredentials')
    return
  }
  const result = await loginRequest(login.value, password.value)
  if (result.success) {
    localStorage.setItem('specialist_session', '1')
    router.push('/main')
  } else {
    error.value = result.error || t('auth.invalidCredentials')
  }
}
</script>

<style scoped>
.login-container {
  max-width: 400px;
  margin: 100px auto;
  padding: 2rem;
  border: 1px solid #ccc;
  border-radius: 8px;
  background: #fff;
  color-scheme: light;
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
input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: #fff;
  color: #1f2937;
}

input:-webkit-autofill,
input:-webkit-autofill:hover,
input:-webkit-autofill:focus {
  -webkit-box-shadow: 0 0 0 1000px #fff inset;
  -webkit-text-fill-color: #1f2937;
}
button {
  width: 100%;
  padding: 0.75rem;
  background: #4f46e5;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
}
.error {
  color: red;
  margin-top: 1rem;
}
.register-link {
  margin-top: 1rem;
  text-align: center;
  font-size: 0.9rem;
}
.register-link a {
  color: #4f46e5;
  text-decoration: none;
}
.register-link a:hover {
  text-decoration: underline;
}
</style>
