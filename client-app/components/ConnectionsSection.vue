<template>
  <div class="section">
    <div class="section-header">
      <div class="header-content">
        <h2><span class="icon">🤝</span>My Connections</h2>
        <p class="header-subtitle">Manage your professional network and partnerships</p>
      </div>
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

    <!-- Connection Form Modal -->
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
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { useApi } from '../composables/useApi'

const config = useRuntimeConfig()
const { $fetch } = useApi()

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
    const connectionsData = (await $fetch(`${config.public.apiBase}/users/${userId}/connections`)) as any[]
    connections.value = connectionsData || []
    
    // Load connection types (public endpoint, but include auth anyway)
    const typesData = (await $fetch(`${config.public.apiBase}/connection-types`)) as any[]
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

onMounted(() => {
  loadConnections()
})

defineExpose({
  loadConnections
})
</script>

<style scoped>
.section {
  margin-top: 2rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  gap: 1.5rem;
}

.header-content {
  flex: 1;
}

.section-header h2 {
  color: #1f2937;
  margin: 0 0 0.35rem 0;
  font-size: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-subtitle {
  color: #6b7280;
  margin: 0;
  font-size: 0.875rem;
  font-weight: 400;
}

.icon {
  font-size: 1.5rem;
}

.list-state {
  padding: 2rem;
  text-align: center;
  color: #666;
  background: white;
  border-radius: 8px;
}

.list-state.error {
  color: #dc2626;
  background: #fee2e2;
}

.empty-state {
  padding: 3rem 2rem;
  text-align: center;
  background: white;
  border-radius: 8px;
  border: 2px dashed #d1d5db;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: #1f2937;
  margin: 0.5rem 0;
}

.empty-state p {
  color: #666;
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
  display: flex;
  flex-direction: column;
  gap: 1rem;
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
  margin-bottom: 0.5rem;
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
  margin-bottom: 0.5rem;
  word-break: break-all;
}

.connection-actions {
  display: flex;
  gap: 0.75rem;
}

.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary {
  background: #4f46e5;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #4338ca;
}

.btn-secondary {
  background: #6b7280;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background: #4b5563;
}

.btn-danger {
  background: #dc2626;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #b91c1c;
}

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.85rem;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  margin-right: 0.5rem;
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
}

.form-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 90%;
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
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #6b7280;
}

.form-body {
  padding: 1.5rem;
}

.form-field {
  margin-bottom: 1.5rem;
}

.form-field:last-child {
  margin-bottom: 0;
}

.form-field label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
  font-size: 0.9rem;
}

.form-field input,
.form-field select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.95rem;
  font-family: inherit;
  box-sizing: border-box;
}

.form-field input:focus,
.form-field select:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-message {
  padding: 1rem;
  border-radius: 6px;
  margin-top: 1rem;
}

.form-message.success {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #86efac;
}

.form-message.error {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fca5a5;
}

.form-footer {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
}
</style>
