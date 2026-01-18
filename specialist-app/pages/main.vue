<template>
  <div class="main-container">
    <nav class="menu-panel">
      <div class="menu-title">Specialist Menu</div>
      <ul>
        <li @click="logout">Logout</li>
      </ul>
      <div class="menu-divider"></div>
    </nav>
    <div class="content">
      <h1>Welcome to Specialist Dashboard</h1>
      <p>Manage your consultations and availability here.</p>
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
    localStorage.removeItem('specialist_session')
    router.push('/login')
  }
}
</script>

<style scoped>
.main-container {
  display: flex;
  min-height: 100vh;
  width: 100%;
  justify-content: center;
  background: #f8fafc;
}
.menu-panel {
  width: 200px;
  background: #f5f5f5;
  padding: 1.5rem 0.75rem;
  border-right: 1px solid #ddd;
  flex-shrink: 0;
}
.menu-title {
  font-weight: 700;
  margin-bottom: 0.75rem;
  color: #1f2937;
  font-size: 0.95rem;
}
.menu-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 0.75rem 0;
}
.menu-panel ul {
  list-style: none;
  padding: 0;
}
.menu-panel li {
  padding: 0.55rem 0.75rem;
  cursor: pointer;
  color: #4f46e5;
  border-radius: 4px;
  transition: background 0.2s;
  font-size: 0.9rem;
}
.menu-panel li:hover {
  background: #e6e6e6;
}
.content {
  flex: 1;
  padding: 1.25rem;
  max-width: 90vw;
}
</style>
