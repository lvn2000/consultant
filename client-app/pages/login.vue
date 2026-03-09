/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
import { useRouter } from 'vue-router'
import { loginRequest } from '../composables/useLogin'

const { t } = useI18n()

const login = ref('')
const password = ref('')
const error = ref('')
const router = useRouter()

const onLogin = async () => {
  error.value = ''
  if (!login.value || !password.value) {
    error.value = t('auth.pleaseEnterCredentials')
    return
  }
  const result = await loginRequest(login.value, password.value)
  if (result.success) {
    localStorage.setItem('client_session', '1')
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
