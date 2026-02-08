<template>
  <div class="login-container">
    <form @submit.prevent="onLogin">
      <h2>Login</h2>
      <div>
        <label for="login">Username</label>
        <input id="login" v-model="login" type="text" required />
      </div>
      <div>
        <label for="password">Password</label>
        <input id="password" v-model="password" type="password" required />
      </div>
      <button type="submit">Login</button>
      <div v-if="error" class="error">{{ error }}</div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const login = ref('')
const password = ref('')
const error = ref('')
const router = useRouter()

import { loginRequest } from '../composables/useLogin'

const onLogin = async () => {
  error.value = ''
  if (!login.value || !password.value) {
    error.value = 'Please enter login and password'
    return
  }
  const result = await loginRequest(login.value, password.value)
  if (result.success) {
    localStorage.setItem('admin_session', '1')
    router.push('/main')
  } else {
    error.value = result.error || 'Invalid credentials'
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
  background: #007bff;
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
</style>
