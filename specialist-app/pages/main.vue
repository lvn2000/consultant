<template>
  <div class="main-container">
    <nav class="menu-panel">
      <div class="menu-title">Specialist Menu</div>
      <ul>
        <li :class="{ active: selectedMenu === 'profile' }" @click="selectMenu('profile')">
          Profile
        </li>
        <li :class="{ active: selectedMenu === 'rates' }" @click="selectMenu('rates')">
          My Rates
        </li>
        <li :class="{ active: selectedMenu === 'connections' }" @click="selectMenu('connections')">
          My Connections
        </li>
      </ul>
      <div class="menu-divider"></div>
      <ul>
        <li class="logout" @click="logout">Logout</li>
      </ul>
    </nav>
    <div class="content">
      <h1>Welcome to Specialist Dashboard</h1>

      <!-- Profile Section -->
      <section v-if="selectedMenu === 'profile'" class="section">
        <div class="section-header">
          <h2>My Profile</h2>
          <button type="button" class="btn" @click="loadProfile">Refresh</button>
        </div>

        <div class="list-state" v-if="profileLoading">Loading profile...</div>
        <div class="list-state error" v-else-if="profileError">{{ profileError }}</div>

        <form v-else class="form" @submit.prevent="updateProfile">
          <div class="form-grid">
            <div class="form-field">
              <label for="name">Full Name</label>
              <input id="name" v-model="profileForm.name" type="text" placeholder="Your name" required />
            </div>
            <div class="form-field">
              <label for="email">Email</label>
              <input id="email" v-model="profileForm.email" type="email" placeholder="your@email.com" required />
            </div>
            <div class="form-field">
              <label for="phone">Phone</label>
              <input id="phone" v-model="profileForm.phone" type="tel" placeholder="+1 555 123 4567" />
            </div>
            <div class="form-field">
              <label for="availability">Availability Status</label>
              <select id="availability" v-model="profileForm.isAvailable">
                <option :value="true">Available</option>
                <option :value="false">Unavailable</option>
              </select>
            </div>
          </div>
          <div class="form-actions">
            <button type="submit" class="btn" :disabled="profileUpdating">
              {{ profileUpdating ? 'Updating...' : 'Update Profile' }}
            </button>
            <button type="button" class="btn danger" @click="showRemoveAccountConfirm = true">
              Remove Account
            </button>
          </div>
          <div v-if="profileUpdateMessage" :class="['form-message', profileUpdateSuccess ? 'success' : 'error']">
            {{ profileUpdateMessage }}
          </div>
        </form>

        <!-- Remove Account Confirmation -->
        <div v-if="showRemoveAccountConfirm" class="modal-overlay" @click="showRemoveAccountConfirm = false">
          <div class="modal" @click.stop>
            <h3>Remove Account</h3>
            <p>Are you sure you want to remove your account? This action cannot be undone.</p>
            <div class="modal-actions">
              <button type="button" class="btn danger" @click="removeAccount" :disabled="accountRemoving">
                {{ accountRemoving ? 'Removing...' : 'Yes, Remove Account' }}
              </button>
              <button type="button" class="btn" @click="showRemoveAccountConfirm = false">Cancel</button>
            </div>
          </div>
        </div>
      </section>

      <!-- Rates Section -->
      <section v-if="selectedMenu === 'rates'" class="section">
        <div class="section-header">
          <h2>My Rates</h2>
          <button type="button" class="btn" @click="loadRates">Refresh</button>
        </div>

        <div class="list-state" v-if="ratesLoading">Loading rates...</div>
        <div class="list-state error" v-else-if="ratesError">{{ ratesError }}</div>

        <div class="table" v-else-if="rates.length > 0">
          <div class="table-header rates-table">
            <span>Category</span>
            <span>Hourly Rate</span>
            <span>Experience (years)</span>
            <span>Actions</span>
          </div>
          <div v-for="rate in rates" :key="rate.categoryId" class="table-row rates-table">
            <span>{{ getCategoryName(rate.categoryId) }}</span>
            <span>${{ rate.hourlyRate }}</span>
            <span>{{ rate.experienceYears }}</span>
            <span class="row-actions">
              <button type="button" class="btn" @click="startEditRate(rate)">Update</button>
              <button type="button" class="btn danger" @click="removeRate(rate.categoryId)">Delete</button>
            </span>
          </div>
        </div>
        <div v-else class="list-state">No rates configured yet.</div>

        <!-- Rate Form -->
        <form class="form" @submit.prevent="saveRate">
          <h3>{{ editingRateId ? 'Update Rate' : 'Add New Rate' }}</h3>
          <div class="form-grid">
            <div class="form-field">
              <label for="rate-category">Category</label>
              <select id="rate-category" v-model="rateForm.categoryId" required :disabled="!!editingRateId">
                <option value="">Select category</option>
                <option v-for="category in categories" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
            </div>
            <div class="form-field">
              <label for="rate-hourly">Hourly Rate ($)</label>
              <input id="rate-hourly" v-model.number="rateForm.hourlyRate" type="number" min="1" step="0.01" required />
            </div>
            <div class="form-field">
              <label for="rate-experience">Experience (years)</label>
              <input id="rate-experience" v-model.number="rateForm.experienceYears" type="number" min="0" step="1" required />
            </div>
          </div>
          <div class="form-actions">
            <button type="submit" class="btn" :disabled="rateSaving">
              {{ rateSaving ? 'Saving...' : (editingRateId ? 'Update Rate' : 'Add Rate') }}
            </button>
            <button v-if="editingRateId" type="button" class="btn" @click="cancelEditRate">Cancel</button>
          </div>
          <div v-if="rateMessage" :class="['form-message', rateSuccess ? 'success' : 'error']">
            {{ rateMessage }}
          </div>
        </form>
      </section>

      <!-- Connections Section -->
      <section v-if="selectedMenu === 'connections'" class="section">
        <div class="section-header">
          <h2>My Connections</h2>
          <button type="button" class="btn" @click="loadConnections">Refresh</button>
        </div>

        <div class="list-state" v-if="connectionsLoading">Loading connections...</div>
        <div class="list-state error" v-else-if="connectionsError">{{ connectionsError }}</div>

        <div class="table" v-else-if="connections.length > 0">
          <div class="table-header connections-table">
            <span>Type</span>
            <span>Value</span>
            <span>Verified</span>
            <span>Actions</span>
          </div>
          <div v-for="connection in connections" :key="connection.id" class="table-row connections-table">
            <span>{{ getConnectionTypeName(connection.connectionTypeId) }}</span>
            <span>{{ connection.connectionValue }}</span>
            <span>{{ connection.isVerified ? 'Yes' : 'No' }}</span>
            <span class="row-actions">
              <button type="button" class="btn" @click="startEditConnection(connection)">Update</button>
              <button type="button" class="btn danger" @click="removeConnection(connection.id)">Delete</button>
            </span>
          </div>
        </div>
        <div v-else class="list-state">No connections configured yet.</div>

        <!-- Connection Form -->
        <form class="form" @submit.prevent="saveConnection">
          <h3>{{ editingConnectionId ? 'Update Connection' : 'Add New Connection' }}</h3>
          <div class="form-grid">
            <div class="form-field">
              <label for="connection-type">Connection Type</label>
              <select id="connection-type" v-model="connectionForm.connectionTypeId" required>
                <option value="">Select type</option>
                <option v-for="type in connectionTypes" :key="type.id" :value="type.id">
                  {{ type.name }}
                </option>
              </select>
            </div>
            <div class="form-field">
              <label for="connection-value">Connection Value</label>
              <input id="connection-value" v-model="connectionForm.connectionValue" type="text" required />
            </div>
            <div class="form-field">
              <label for="connection-verified">Verified</label>
              <select id="connection-verified" v-model="connectionForm.isVerified">
                <option :value="true">Yes</option>
                <option :value="false">No</option>
              </select>
            </div>
          </div>
          <div class="form-actions">
            <button type="submit" class="btn" :disabled="connectionSaving">
              {{ connectionSaving ? 'Saving...' : (editingConnectionId ? 'Update Connection' : 'Add Connection') }}
            </button>
            <button v-if="editingConnectionId" type="button" class="btn" @click="cancelEditConnection">Cancel</button>
          </div>
          <div v-if="connectionMessage" :class="['form-message', connectionSuccess ? 'success' : 'error']">
            {{ connectionMessage }}
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'

const router = useRouter()
const config = useRuntimeConfig()

// Menu state
const selectedMenu = ref('profile')

// Profile state
const profileLoading = ref(false)
const profileError = ref('')
const profileForm = ref({
  name: '',
  email: '',
  phone: '',
  isAvailable: true
})
const profileUpdating = ref(false)
const profileUpdateMessage = ref('')
const profileUpdateSuccess = ref(false)
const showRemoveAccountConfirm = ref(false)
const accountRemoving = ref(false)

// Rates state
const ratesLoading = ref(false)
const ratesError = ref('')
const rates = ref<any[]>([])
const categories = ref<any[]>([])
const rateForm = ref({
  categoryId: '',
  hourlyRate: 0,
  experienceYears: 0
})
const editingRateId = ref('')
const rateSaving = ref(false)
const rateMessage = ref('')
const rateSuccess = ref(false)

// Connections state
const connectionsLoading = ref(false)
const connectionsError = ref('')
const connections = ref<any[]>([])
const connectionTypes = ref<any[]>([])
const connectionForm = ref({
  connectionTypeId: '',
  connectionValue: '',
  isVerified: false
})
const editingConnectionId = ref('')
const connectionSaving = ref(false)
const connectionMessage = ref('')
const connectionSuccess = ref(false)

const selectMenu = (menu: string) => {
  selectedMenu.value = menu
  if (menu === 'profile') loadProfile()
  if (menu === 'rates') loadRates()
  if (menu === 'connections') loadConnections()
}

// Profile operations
const loadProfile = async () => {
  profileLoading.value = true
  profileError.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      profileError.value = 'User ID not found'
      return
    }
    const specialist = await $fetch(`${config.public.apiBase}/specialists/${userId}`)
    profileForm.value = {
      name: specialist.name || '',
      email: specialist.email || '',
      phone: specialist.phone || '',
      isAvailable: specialist.isAvailable ?? true
    }
  } catch (error: any) {
    profileError.value = error.message || 'Failed to load profile'
  } finally {
    profileLoading.value = false
  }
}

const updateProfile = async () => {
  profileUpdating.value = true
  profileUpdateMessage.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    await $fetch(`${config.public.apiBase}/specialists/${userId}`, {
      method: 'PUT',
      body: profileForm.value
    })
    profileUpdateMessage.value = 'Profile updated successfully'
    profileUpdateSuccess.value = true
  } catch (error: any) {
    profileUpdateMessage.value = error.message || 'Failed to update profile'
    profileUpdateSuccess.value = false
  } finally {
    profileUpdating.value = false
  }
}

const removeAccount = async () => {
  accountRemoving.value = true
  try {
    const userId = sessionStorage.getItem('userId')
    await $fetch(`${config.public.apiBase}/specialists/${userId}`, {
      method: 'DELETE'
    })
    alert('Account removed successfully')
    logout()
  } catch (error: any) {
    alert(error.message || 'Failed to remove account')
  } finally {
    accountRemoving.value = false
    showRemoveAccountConfirm.value = false
  }
}

// Rates operations
const loadRates = async () => {
  ratesLoading.value = true
  ratesError.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    const specialist = await $fetch(`${config.public.apiBase}/specialists/${userId}`)
    rates.value = specialist.categoryRates || []
    // Load categories
    const categoriesData = await $fetch(`${config.public.apiBase}/categories?page=1&pageSize=100`)
    categories.value = categoriesData.categories || []
  } catch (error: any) {
    ratesError.value = error.message || 'Failed to load rates'
  } finally {
    ratesLoading.value = false
  }
}

const startEditRate = (rate: any) => {
  rateForm.value = {
    categoryId: rate.categoryId,
    hourlyRate: rate.hourlyRate,
    experienceYears: rate.experienceYears
  }
  editingRateId.value = rate.categoryId
}

const cancelEditRate = () => {
  rateForm.value = { categoryId: '', hourlyRate: 0, experienceYears: 0 }
  editingRateId.value = ''
  rateMessage.value = ''
}

const saveRate = async () => {
  rateSaving.value = true
  rateMessage.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    const specialist = await $fetch(`${config.public.apiBase}/specialists/${userId}`)
    
    let updatedRates = [...(specialist.categoryRates || [])]
    
    if (editingRateId.value) {
      // Update existing rate
      const index = updatedRates.findIndex(r => r.categoryId === editingRateId.value)
      if (index !== -1) {
        updatedRates[index] = { ...rateForm.value }
      }
    } else {
      // Add new rate
      updatedRates.push({ ...rateForm.value })
    }
    
    await $fetch(`${config.public.apiBase}/specialists/${userId}`, {
      method: 'PUT',
      body: {
        ...specialist,
        categoryRates: updatedRates
      }
    })
    
    rateMessage.value = editingRateId.value ? 'Rate updated successfully' : 'Rate added successfully'
    rateSuccess.value = true
    cancelEditRate()
    await loadRates()
  } catch (error: any) {
    rateMessage.value = error.message || 'Failed to save rate'
    rateSuccess.value = false
  } finally {
    rateSaving.value = false
  }
}

const removeRate = async (categoryId: string) => {
  if (!confirm('Are you sure you want to remove this rate?')) return
  
  try {
    const userId = sessionStorage.getItem('userId')
    const specialist = await $fetch(`${config.public.apiBase}/specialists/${userId}`)
    const updatedRates = (specialist.categoryRates || []).filter((r: any) => r.categoryId !== categoryId)
    
    await $fetch(`${config.public.apiBase}/specialists/${userId}`, {
      method: 'PUT',
      body: {
        ...specialist,
        categoryRates: updatedRates
      }
    })
    
    await loadRates()
  } catch (error: any) {
    alert(error.message || 'Failed to remove rate')
  }
}

const getCategoryName = (categoryId: string) => {
  const category = categories.value.find(c => c.id === categoryId)
  return category ? category.name : 'Unknown'
}

// Connections operations
const loadConnections = async () => {
  connectionsLoading.value = true
  connectionsError.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    const connectionsData = await $fetch(`${config.public.apiBase}/specialists/${userId}/connections`)
    connections.value = connectionsData || []
    // Load connection types
    const typesData = await $fetch(`${config.public.apiBase}/connections?page=1&pageSize=100`)
    connectionTypes.value = typesData.typeConnections || []
  } catch (error: any) {
    connectionsError.value = error.message || 'Failed to load connections'
  } finally {
    connectionsLoading.value = false
  }
}

const startEditConnection = (connection: any) => {
  connectionForm.value = {
    connectionTypeId: connection.connectionTypeId,
    connectionValue: connection.connectionValue,
    isVerified: connection.isVerified
  }
  editingConnectionId.value = connection.id
}

const cancelEditConnection = () => {
  connectionForm.value = { connectionTypeId: '', connectionValue: '', isVerified: false }
  editingConnectionId.value = ''
  connectionMessage.value = ''
}

const saveConnection = async () => {
  connectionSaving.value = true
  connectionMessage.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    
    if (editingConnectionId.value) {
      // Update existing connection
      await $fetch(`${config.public.apiBase}/specialists/${userId}/connections/${editingConnectionId.value}`, {
        method: 'PUT',
        body: connectionForm.value
      })
      connectionMessage.value = 'Connection updated successfully'
    } else {
      // Add new connection
      await $fetch(`${config.public.apiBase}/specialists/${userId}/connections`, {
        method: 'POST',
        body: connectionForm.value
      })
      connectionMessage.value = 'Connection added successfully'
    }
    
    connectionSuccess.value = true
    cancelEditConnection()
    await loadConnections()
  } catch (error: any) {
    connectionMessage.value = error.message || 'Failed to save connection'
    connectionSuccess.value = false
  } finally {
    connectionSaving.value = false
  }
}

const removeConnection = async (connectionId: string) => {
  if (!confirm('Are you sure you want to remove this connection?')) return
  
  try {
    const userId = sessionStorage.getItem('userId')
    await $fetch(`${config.public.apiBase}/specialists/${userId}/connections/${connectionId}`, {
      method: 'DELETE'
    })
    await loadConnections()
  } catch (error: any) {
    alert(error.message || 'Failed to remove connection')
  }
}

const getConnectionTypeName = (typeId: string) => {
  const type = connectionTypes.value.find(t => t.id === typeId)
  return type ? type.name : 'Unknown'
}

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

onMounted(() => {
  loadProfile()
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
  transition: background 0.15s, color 0.15s;
  font-size: 0.9rem;
}

.menu-panel li:hover {
  background: #e0e7ff;
}

.menu-panel li.active {
  background: #4f46e5;
  color: white;
}

.menu-panel li.logout {
  color: #dc2626;
}

.menu-panel li.logout:hover {
  background: #fee2e2;
}

.content {
  flex: 1;
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.content h1 {
  margin-bottom: 2rem;
  color: #1f2937;
}

.section {
  margin-top: 2rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-header h2 {
  color: #1f2937;
  margin: 0;
}

.list-state {
  padding: 1rem;
  background: #f3f4f6;
  border-radius: 4px;
  text-align: center;
  color: #6b7280;
}

.list-state.error {
  background: #fee2e2;
  color: #dc2626;
}

.table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
}

.table-header, .table-row {
  display: grid;
  padding: 0.75rem 1rem;
  align-items: center;
  gap: 1rem;
}

.rates-table {
  grid-template-columns: 2fr 1fr 1fr 1.5fr;
}

.connections-table {
  grid-template-columns: 1.5fr 2fr 1fr 1.5fr;
}

.table-header {
  background: #f9fafb;
  font-weight: 600;
  font-size: 0.875rem;
  color: #6b7280;
  border-bottom: 1px solid #e5e7eb;
}

.table-row {
  border-bottom: 1px solid #f3f4f6;
}

.table-row:last-child {
  border-bottom: none;
}

.table-row:hover {
  background: #f9fafb;
}

.row-actions {
  display: flex;
  gap: 0.5rem;
}

.form {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.form h3 {
  margin-top: 0;
  margin-bottom: 1rem;
  color: #1f2937;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
}

.form-field label {
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #374151;
  font-size: 0.875rem;
}

.form-field input,
.form-field select {
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.875rem;
}

.form-field input:focus,
.form-field select:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-actions {
  display: flex;
  gap: 1rem;
}

.form-message {
  margin-top: 1rem;
  padding: 0.75rem;
  border-radius: 4px;
  font-size: 0.875rem;
}

.form-message.success {
  background: #d1fae5;
  color: #065f46;
}

.form-message.error {
  background: #fee2e2;
  color: #dc2626;
}

.btn {
  padding: 0.5rem 1rem;
  background: #4f46e5;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: background 0.15s;
}

.btn:hover:not(:disabled) {
  background: #4338ca;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn.danger {
  background: #dc2626;
}

.btn.danger:hover:not(:disabled) {
  background: #b91c1c;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  max-width: 500px;
  width: 90%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal h3 {
  margin-top: 0;
  color: #1f2937;
}

.modal p {
  color: #6b7280;
  margin: 1rem 0;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 1.5rem;
}
</style>
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
