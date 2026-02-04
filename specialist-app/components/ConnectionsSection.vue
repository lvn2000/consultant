<template>
  <section class="section">
    <div class="section-header">
      <div class="header-content">
        <h2><span class="icon">🤝</span>My Connections</h2>
        <p class="header-subtitle">Manage your professional network and partnerships</p>
      </div>
      <button type="button" class="btn btn-primary" @click="startEditConnection(null)">
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
      <button type="button" class="btn btn-primary" @click="startEditConnection(null)">
        <span class="btn-icon">+</span> Add Connection
      </button>
    </div>
    <div v-else class="connections-list">
      <div class="connection-card" v-for="connection in connections" :key="connection.id">
        <div class="connection-header">
          <div class="connection-type-badge">
            {{ getConnectionTypeName(connection.connectionTypeId) }}
          </div>
          <div class="connection-status" :class="{ verified: connection.isVerified }">
            <span class="status-dot"></span>
            {{ connection.isVerified ? 'Verified' : 'Not verified' }}
          </div>
        </div>
        <div class="connection-value">{{ connection.connectionValue }}</div>
        <div class="connection-actions">
          <button type="button" class="btn btn-sm btn-secondary" @click="startEditConnection(connection)">
            ✏️ Edit
          </button>
          <button type="button" class="btn btn-sm btn-danger" @click="removeConnection(connection.id)">
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
          <button type="button" class="close-btn" @click="cancelEditConnection">×</button>
        </div>
        <form class="form-body" @submit.prevent="saveConnection">
          <div class="form-field">
            <label for="connection-type">Connection Type *</label>
            <select
              id="connection-type"
              v-model="connectionForm.connectionTypeId"
              required
            >
              <option value="">Select a connection type</option>
              <option v-for="type in connectionTypes" :key="type.id" :value="type.id">
                {{ type.name }} {{ type.description ? `- ${type.description}` : '' }}
              </option>
            </select>
          </div>
          <div class="form-field">
            <label for="connection-value">Connection Value *</label>
            <input 
              id="connection-value" 
              v-model="connectionForm.connectionValue" 
              type="text" 
              placeholder="e.g., +1234567890 or username"
              required 
            />
          </div>
          <div v-if="connectionMessage" :class="['form-message', connectionSuccess ? 'success' : 'error']">
            {{ connectionMessage }}
          </div>
          <div class="form-footer">
            <button type="button" class="btn btn-secondary" @click="cancelEditConnection" :disabled="connectionSaving">
              Cancel
            </button>
            <button type="submit" class="btn btn-primary" :disabled="connectionSaving || !connectionForm.connectionTypeId || !connectionForm.connectionValue">
              {{ connectionSaving ? 'Saving...' : 'Save Connection' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'

const config = useRuntimeConfig()

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
    const connectionsData = await $fetch(`${config.public.apiBase}/specialists/${userId}/connections`)
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
    // New connection
    connectionForm.value = { connectionTypeId: '', connectionValue: '', isVerified: false }
    editingConnectionId.value = 'new'
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
    
    if (editingConnectionId.value && editingConnectionId.value !== 'new') {
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
    setTimeout(() => {
      cancelEditConnection()
      loadConnections()
    }, 1500)
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
    if (!userId) {
      alert('User ID not found')
      return
    }
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
  padding: 2rem 1rem;
  background: white;
  border-radius: 12px;
  text-align: center;
  color: #6b7280;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.list-state.error {
  background: #fee2e2;
  color: #dc2626;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e5e7eb;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.empty-icon {
  font-size: 3rem;
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
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1rem;
}

.connection-card {
  background: white;
  border-radius: 12px;
  padding: 1rem;
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

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0.625rem 1.25rem;
  font-weight: 600;
  box-shadow: 0 4px 6px rgba(102, 126, 234, 0.4);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(102, 126, 234, 0.5);
}

.btn-secondary {
  background: #6b7280;
}

.btn-secondary:hover:not(:disabled) {
  background: #4b5563;
}

.btn-danger {
  background: #dc2626;
}

.btn-danger:hover:not(:disabled) {
  background: #b91c1c;
}

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.8125rem;
}

.btn {
  padding: 0.5rem 1rem;
  background: #4f46e5;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn:hover:not(:disabled) {
  background: #4338ca;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 1.125rem;
  margin-right: 0.25rem;
}

.form-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.form-card {
  background: white;
  border-radius: 12px;
  max-width: 500px;
  width: 100%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2);
  max-height: 90vh;
  overflow-y: auto;
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
  font-size: 2rem;
  color: #6b7280;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.15s, color 0.15s;
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
  margin-top: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  margin-bottom: 1rem;
}

.form-field label {
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #374151;
  font-size: 0.875rem;
}

.form-field input,
.form-field select {
  padding: 0.625rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: border-color 0.15s, box-shadow 0.15s;
  background: white;
  color: #1f2937;
}

.form-field input:focus,
.form-field select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-message {
  padding: 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.form-message.success {
  background: #d1fae5;
  color: #065f46;
  border-left: 4px solid #059669;
}

.form-message.error {
  background: #fee2e2;
  color: #dc2626;
  border-left: 4px solid #dc2626;
}
</style>
