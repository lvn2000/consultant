<template>
  <div class="main-container">
    <nav class="menu-panel">
      <div class="menu-title">Client Menu</div>
      <ul>
        <li :class="{ active: selectedMenu === 'profile' }" @click="selectMenu('profile')">Profile</li>
        <li :class="{ active: selectedMenu === 'connections' }" @click="selectMenu('connections')">My Connections</li>
      </ul>
      <div class="menu-divider"></div>
      <ul>
        <li class="logout" @click="logout">Logout</li>
      </ul>
    </nav>
    <div class="content">
      <h1>Client Dashboard</h1>

      <!-- Profile Section -->
      <div v-if="selectedMenu === 'profile'" class="section">
        <div v-if="profileLoading" class="list-state">Loading profile...</div>
        <div v-else-if="profileError" class="list-state error">{{ profileError }}</div>
        <div v-else class="form">
          <h3>Profile Information</h3>
          <div class="form-grid">
            <div class="form-field">
              <label>Name</label>
              <input v-model="profileForm.name" type="text" placeholder="Your name" />
            </div>
            <div class="form-field">
              <label>Email</label>
              <input v-model="profileForm.email" type="email" placeholder="your@email.com" />
            </div>
            <div class="form-field">
              <label>Phone</label>
              <input v-model="profileForm.phone" type="tel" placeholder="+1234567890" />
            </div>
          </div>
          <div class="form-actions">
            <button class="btn btn-primary" @click="updateProfile" :disabled="profileUpdating">
              {{ profileUpdating ? 'Updating...' : 'Update Profile' }}
            </button>
            <button class="btn btn-danger" @click="showRemoveAccountConfirm = true">
              Remove Account
            </button>
          </div>
          <div v-if="profileUpdateMessage" :class="['form-message', profileUpdateSuccess ? 'success' : 'error']">
            {{ profileUpdateMessage }}
          </div>
        </div>
      </div>

      <!-- Connections Section -->
      <div v-if="selectedMenu === 'connections'" class="section">
        <div class="section-header">
          <h2>My Connections</h2>
          <button class="btn btn-primary" @click="startEditConnection(null)">
            <span class="btn-icon">+</span> Add Connection
          </button>
        </div>
        
        <div v-if="connectionsLoading" class="list-state">
          <div class="spinner"></div>
          <p>Loading connections...</p>
        </div>
        <div v-else-if="connectionsError" class="list-state error">{{ connectionsError }}</div>
        <div v-else-if="connections.length === 0" class="empty-state">
          <div class="empty-icon">📱</div>
          <h3>No connections yet</h3>
          <p>Add your first connection to get started</p>
          <button class="btn btn-primary" @click="startEditConnection(null)">
            <span class="btn-icon">+</span> Add Connection
          </button>
        </div>
        <div v-else class="connections-list">
          <div class="connection-card" v-for="conn in connections" :key="conn.id">
            <div class="connection-header">
              <div class="connection-type-badge">
                {{ getConnectionTypeName(conn.connectionTypeId) }}
              </div>
              <div class="connection-status" :class="{ verified: conn.isVerified }">
                <span class="status-dot"></span>
                {{ conn.isVerified ? 'Verified' : 'Not verified' }}
              </div>
            </div>
            <div class="connection-value">{{ conn.connectionValue }}</div>
            <div class="connection-actions">
              <button class="btn btn-sm btn-secondary" @click="startEditConnection(conn)">
                ✏️ Edit
              </button>
              <button class="btn btn-sm btn-danger" @click="removeConnection(conn.id)">
                🗑️ Remove
              </button>
            </div>
          </div>
        </div>

        <!-- Connection Form -->
        <div v-if="editingConnectionId !== null" class="form-modal">
          <div class="form-card">
            <div class="form-header">
              <h3>{{ editingConnectionId ? 'Edit Connection' : 'Add New Connection' }}</h3>
              <button class="close-btn" @click="cancelEditConnection">×</button>
            </div>
            <div class="form-body">
              <div class="form-field">
                <label>Connection Type *</label>
                <select v-model="connectionForm.connectionTypeId">
                  <option value="">Select a connection type</option>
                  <option v-for="type in connectionTypes" :key="type.id" :value="type.id">
                    {{ type.name }} {{ type.description ? `- ${type.description}` : '' }}
                  </option>
                </select>
              </div>
              <div class="form-field">
                <label>Connection Value *</label>
                <input 
                  v-model="connectionForm.connectionValue" 
                  type="text" 
                  placeholder="e.g., +1234567890 or username" 
                />
              </div>
              <div v-if="connectionMessage" :class="['form-message', connectionSuccess ? 'success' : 'error']">
                {{ connectionMessage }}
              </div>
            </div>
            <div class="form-footer">
              <button class="btn btn-secondary" @click="cancelEditConnection" :disabled="connectionSaving">
                Cancel
              </button>
              <button class="btn btn-primary" @click="saveConnection" :disabled="connectionSaving || !connectionForm.connectionTypeId || !connectionForm.connectionValue">
                {{ connectionSaving ? 'Saving...' : 'Save Connection' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Remove Account Confirmation Modal -->
    <div v-if="showRemoveAccountConfirm" class="modal-overlay" @click.self="showRemoveAccountConfirm = false">
      <div class="modal">
        <h3>Remove Account</h3>
        <p>Are you sure you want to remove your account? This action cannot be undone.</p>
        <div class="modal-actions">
          <button class="btn danger" @click="removeAccount" :disabled="accountRemoving">
            {{ accountRemoving ? 'Removing...' : 'Yes, Remove' }}
          </button>
          <button class="btn" @click="showRemoveAccountConfirm = false">Cancel</button>
        </div>
      </div>
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
  phone: ''
})
const profileUpdating = ref(false)
const profileUpdateMessage = ref('')
const profileUpdateSuccess = ref(false)
const showRemoveAccountConfirm = ref(false)
const accountRemoving = ref(false)

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
const editingConnectionId = ref<string | null>(null)
const connectionSaving = ref(false)
const connectionMessage = ref('')
const connectionSuccess = ref(false)

const selectMenu = (menu: string) => {
  selectedMenu.value = menu
  if (menu === 'profile') loadProfile()
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
    const user = await $fetch(`${config.public.apiBase}/users/${userId}`)
    profileForm.value = {
      name: user.name || '',
      email: user.email || '',
      phone: user.phone || ''
    }
  } catch (error: any) {
    console.error('Profile load error:', error)
    profileError.value = error.data?.message || error.message || 'Failed to load profile'
  } finally {
    profileLoading.value = false
  }
}

const updateProfile = async () => {
  profileUpdating.value = true
  profileUpdateMessage.value = ''
  profileUpdateSuccess.value = false
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      profileUpdateMessage.value = 'User ID not found'
      return
    }
    
    await $fetch(`${config.public.apiBase}/users/${userId}`, {
      method: 'PUT',
      body: {
        name: profileForm.value.name,
        email: profileForm.value.email,
        phone: profileForm.value.phone || null
      }
    })
    
    profileUpdateMessage.value = 'Profile updated successfully'
    profileUpdateSuccess.value = true
    
    // Reload profile to get the updated data
    setTimeout(() => {
      loadProfile()
      profileUpdateMessage.value = ''
    }, 2000)
  } catch (error: any) {
    profileUpdateMessage.value = error.data?.message || error.message || 'Failed to update profile'
    profileUpdateSuccess.value = false
  } finally {
    profileUpdating.value = false
  }
}

const removeAccount = async () => {
  accountRemoving.value = true
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      alert('User ID not found')
      return
    }
    // Note: User deletion endpoint not yet implemented in API
    // TODO: Add DELETE /api/users/:id endpoint to the backend
    alert('Account deletion not yet available. API endpoint needs to be implemented.')
    return
  } catch (error: any) {
    alert(error.message || 'Failed to remove account')
  } finally {
    accountRemoving.value = false
    showRemoveAccountConfirm.value = false
  }
}

// Connections operations
const loadConnections = async () => {
  connectionsLoading.value = true
  connectionsError.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      connectionsError.value = 'User ID not found'
      return
    }
    // Load client connections from API
    const connectionsData = await $fetch(`${config.public.apiBase}/users/${userId}/connections`)
    connections.value = connectionsData || []
    
    // Load connection types
    const typesData = await $fetch(`${config.public.apiBase}/connection-types`)
    connectionTypes.value = typesData || []
  } catch (error: any) {
    connectionsError.value = error.message || 'Failed to load connections'
  } finally {
    connectionsLoading.value = false
  }
}

const startEditConnection = (connection: any) => {
  if (connection) {
    connectionForm.value = {
      connectionTypeId: connection.connectionTypeId,
      connectionValue: connection.connectionValue,
      isVerified: connection.isVerified
    }
    editingConnectionId.value = connection.id
  } else {
    connectionForm.value = { connectionTypeId: '', connectionValue: '', isVerified: false }
    editingConnectionId.value = ''
  }
  connectionMessage.value = ''
}

const cancelEditConnection = () => {
  connectionForm.value = { connectionTypeId: '', connectionValue: '', isVerified: false }
  editingConnectionId.value = null
  connectionMessage.value = ''
}

const saveConnection = async () => {
  connectionSaving.value = true
  connectionMessage.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      connectionMessage.value = 'User ID not found'
      connectionSuccess.value = false
      return
    }
    
    if (editingConnectionId.value) {
      // Update existing connection
      await $fetch(`${config.public.apiBase}/users/${userId}/connections/${editingConnectionId.value}`, {
        method: 'PUT',
        body: {
          connectionValue: connectionForm.value.connectionValue
        }
      })
      connectionMessage.value = 'Connection updated successfully'
    } else {
      // Create new connection
      await $fetch(`${config.public.apiBase}/users/${userId}/connections`, {
        method: 'POST',
        body: {
          connectionTypeId: connectionForm.value.connectionTypeId,
          connectionValue: connectionForm.value.connectionValue
        }
      })
      connectionMessage.value = 'Connection added successfully'
    }
    
    connectionSuccess.value = true
    setTimeout(() => {
      cancelEditConnection()
      loadConnections()
    }, 1500)
  } catch (error: any) {
    connectionMessage.value = error.data?.message || error.message || 'Failed to save connection'
    connectionSuccess.value = false
  } finally {
    connectionSaving.value = false
  }
}

const removeConnection = async (connectionId: string) => {
  if (!confirm('Are you sure you want to remove this connection?')) return
  
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      alert('User ID not found')
      return
    }
    
    await $fetch(`${config.public.apiBase}/users/${userId}/connections/${connectionId}`, {
      method: 'DELETE'
    })
    
    loadConnections()
  } catch (error: any) {
    alert(error.data?.message || error.message || 'Failed to remove connection')
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
    localStorage.removeItem('client_session')
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
  padding: 2rem;
  background: white;
  border-radius: 8px;
  text-align: center;
  color: #6b7280;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.list-state.error {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e5e7eb;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  background: white;
  border-radius: 12px;
  padding: 3rem 2rem;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.empty-state p {
  color: #6b7280;
  margin-bottom: 1.5rem;
}

.connections-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.connection-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
  border: 2px solid transparent;
}

.connection-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-color: #e0e7ff;
  transform: translateY(-2px);
}

.connection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.connection-type-badge {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0.375rem 0.875rem;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: #ef4444;
  font-weight: 500;
}

.connection-status.verified {
  color: #10b981;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.connection-value {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 1rem;
  word-break: break-all;
}

.connection-actions {
  display: flex;
  gap: 0.75rem;
}

.form-modal {
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
  padding: 1rem;
}

.form-card {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.form-header h3 {
  margin: 0;
  color: #1f2937;
  font-size: 1.25rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 2rem;
  color: #9ca3af;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.15s;
}

.close-btn:hover {
  background: #f3f4f6;
  color: #1f2937;
}

.form-body {
  padding: 1.5rem;
}

.form-footer {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
  background: #f9fafb;
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
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
  color: #1f2937;
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
  margin-bottom: 1.5rem;
}

.form-field label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.5rem;
}

.form-field input,
.form-field select,
.form-field textarea {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e5e7eb;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: all 0.15s;
}

.form-field input:focus,
.form-field select:focus,
.form-field textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-field select:disabled {
  background: #f9fafb;
  cursor: not-allowed;
}

.form-actions {
  display: flex;
  gap: 1rem;
}

.form-message {
  padding: 0.875rem 1rem;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
}

.form-message.success {
  background: #d1fae5;
  color: #065f46;
  border: 1px solid #6ee7b7;
}

.form-message.error {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.btn {
  padding: 0.625rem 1.25rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.15s;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #f3f4f6;
  color: #4b5563;
}

.btn-secondary:hover:not(:disabled) {
  background: #e5e7eb;
}

.btn-danger {
  background: #ef4444;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #dc2626;
}

.btn-sm {
  padding: 0.5rem 1rem;
  font-size: 0.8125rem;
  flex: 1;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 1.125rem;
  font-weight: 600;
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
