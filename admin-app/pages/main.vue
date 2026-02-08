<template>
  <div class="main-container">
    <nav class="menu-panel">
      <div class="menu-title">Admin Menu</div>
      <ul>
        <li :class="{ active: selectedMenu === 'specialists' }" @click="selectMenu('specialists')">
          Specialists
        </li>
        <li :class="{ active: selectedMenu === 'clients' }" @click="selectMenu('clients')">
          Clients
        </li>
        <li :class="{ active: selectedMenu === 'connections' }" @click="selectMenu('connections')">
          Type Connections
        </li>
        <li :class="{ active: selectedMenu === 'categories' }" @click="selectMenu('categories')">
          Categories
        </li>
      </ul>
      <div class="menu-divider"></div>
      <ul>
        <li class="logout" @click="logout">Logout</li>
      </ul>
    </nav>
    <div class="content">
      <h1>Welcome to Admin Panel</h1>

      <!-- Statistics Dashboard -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">👥</div>
          <div class="stat-content">
            <div class="stat-value">{{ specialistsCount }}</div>
            <div class="stat-label">Specialists</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📂</div>
          <div class="stat-content">
            <div class="stat-value">{{ categoriesCount }}</div>
            <div class="stat-label">Categories</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🔗</div>
          <div class="stat-content">
            <div class="stat-value">{{ connectionTypesCount }}</div>
            <div class="stat-label">Connection Types</div>
          </div>
        </div>
        <div class="stat-card available">
          <div class="stat-icon">✅</div>
          <div class="stat-content">
            <div class="stat-value">{{ availableSpecialistsCount }}</div>
            <div class="stat-label">Available</div>
          </div>
        </div>
      </div>

      <!-- Specialists Section Component -->
      <SpecialistsSection :visible="selectedMenu === 'specialists'" />

      <!-- Clients Section Component -->
      <ClientsSection :visible="selectedMenu === 'clients'" />

      <!-- Connection Types Section Component -->
      <ConnectionTypesSection :visible="selectedMenu === 'connections'" />

      <!-- Categories Section Component -->
      <CategoriesSection :visible="selectedMenu === 'categories'" />
    </div>

    <!-- Confirmation Modal -->
    <div v-if="confirmState.visible" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">{{ confirmState.title }}</div>
        <p class="modal-message">{{ confirmState.message }}</p>
        <div class="modal-actions">
          <button type="button" class="btn" @click="cancelConfirm">Cancel</button>
          <button type="button" class="btn danger" @click="acceptConfirm">Confirm</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useRuntimeConfig } from 'nuxt/app'
import { useApi } from '../composables/useApi'

const router = useRouter()
const config = useRuntimeConfig()
const { $fetch } = useApi()

type MenuKey = 'specialists' | 'clients' | 'connections' | 'categories'

const selectedMenu = ref<MenuKey>('specialists')
const specialistsCount = ref(0)
const categoriesCount = ref(0)
const connectionTypesCount = ref(0)
const availableSpecialistsCount = ref(0)

const confirmState = ref({
  visible: false,
  title: '',
  message: '',
})
const confirmResolver = ref<((value: boolean) => void) | null>(null)

const selectMenu = (menu: MenuKey) => {
  selectedMenu.value = menu
}

const loadStats = async () => {
  try {
    // Load specialists
    const specialists = await $fetch<Array<{ isAvailable?: boolean }>>(
      `${config.public.apiBase}/specialists/search?offset=0&limit=1000`
    )
    specialistsCount.value = specialists.length
    availableSpecialistsCount.value = specialists.filter((specialist: { isAvailable?: boolean }) => specialist.isAvailable).length

    // Load categories
    const categories = await $fetch<any[]>(`${config.public.apiBase}/categories`)
    categoriesCount.value = categories.length

    // Load connection types
    const connectionTypes = await $fetch<any[]>(`${config.public.apiBase}/connection-types`)
    connectionTypesCount.value = connectionTypes.length
  } catch (error) {
    console.error('Failed to load statistics:', error)
  }
}

const logout = async () => {
  try {
    const sessionId = sessionStorage.getItem('sessionId')
    if (sessionId) {
      await $fetch(`${config.public.apiBase}/users/logout`, {
        method: 'POST',
        body: { sessionId },
      })
    }
  } finally {
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('sessionId')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('login')
    sessionStorage.removeItem('email')
    sessionStorage.removeItem('role')
    localStorage.removeItem('admin_session')
    router.push('/login')
  }
}

const acceptConfirm = () => {
  confirmState.value = { ...confirmState.value, visible: false }
  confirmResolver.value?.(true)
  confirmResolver.value = null
}

const cancelConfirm = () => {
  confirmState.value = { ...confirmState.value, visible: false }
  confirmResolver.value?.(false)
  confirmResolver.value = null
}

onMounted(() => {
  loadStats()
})
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
  margin: 1rem 0;
}

.menu-panel ul {
  list-style: none;
  padding: 0;
}

.menu-panel li {
  padding: 0.55rem 0.75rem;
  cursor: pointer;
  color: #007bff;
  border-radius: 4px;
  transition: background 0.2s;
  font-size: 0.9rem;
}

.menu-panel li.active {
  background: #e0e7ff;
  color: #1d4ed8;
  font-weight: 600;
}

.menu-panel li.logout {
  color: #dc2626;
}

.menu-panel li:hover {
  background: #e6e6e6;
}

.content {
  flex: 1;
  padding: 1.25rem;
  max-width: 90vw;
}

.content h1 {
  margin-bottom: 1.5rem;
  font-size: 1.875rem;
  color: #111827;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.stat-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.stat-card.available {
  border-left: 4px solid #10b981;
}

.stat-icon {
  font-size: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 60px;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.875rem;
  font-weight: 700;
  color: #111827;
  line-height: 1;
  margin-bottom: 0.25rem;
}

.stat-label {
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.modal {
  background: #ffffff;
  padding: 1.5rem;
  border-radius: 12px;
  width: min(420px, 90vw);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
}

.modal-header {
  font-size: 1.2rem;
  font-weight: 700;
  color: #111827;
  margin-bottom: 0.75rem;
}

.modal-message {
  color: #374151;
  margin-bottom: 1.25rem;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn {
  padding: 0.45rem 0.9rem;
  border-radius: 6px;
  border: 1px solid #cbd5f5;
  background: #ffffff;
  color: #1f2937;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.85rem;
}

.btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.btn.danger {
  background: #fee2e2;
  color: #b91c1c;
  border-color: #fecaca;
}

.btn.danger:hover:not(:disabled) {
  background: #fca5a5;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
