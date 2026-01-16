<template>
  <div class="main-container">
    <nav class="menu-panel">
      <ul>
        <li @click="logout">Logout</li>
      </ul>
    </nav>
    <div class="content">
      <h1>Welcome to Admin Panel</h1>
      <!-- Add more content here -->
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'

const router = useRouter()
const config = useRuntimeConfig()

const logout = async () => {
  const sessionId = sessionStorage.getItem('sessionId')

  try {
    if (sessionId) {
      await $fetch(`${config.public.apiBase}/users/logout`, {
        method: 'POST',
        body: { sessionId },
      })
    }
  } finally {
    sessionStorage.removeItem('sessionId')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('login')
    sessionStorage.removeItem('email')
    sessionStorage.removeItem('role')
    localStorage.removeItem('admin_session')
    router.push('/login')
  }
}
</script>

<style scoped>
.main-container {
  display: flex;
  min-height: 100vh;
}
.menu-panel {
  width: 200px;
  background: #f5f5f5;
  padding: 2rem 1rem;
  border-right: 1px solid #ddd;
}
.menu-panel ul {
  list-style: none;
  padding: 0;
}
.menu-panel li {
  padding: 0.75rem 1rem;
  cursor: pointer;
  color: #007bff;
  border-radius: 4px;
  transition: background 0.2s;
}
.menu-panel li:hover {
  background: #e6e6e6;
}
.content {
  flex: 1;
  padding: 2rem;
}
</style>
