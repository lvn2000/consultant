<template>
  <section v-if="visible" class="section">
    <div class="section-header">
      <h2>Clients</h2>
      <button type="button" class="btn" @click="loadClients">🔄 Refresh</button>
    </div>

    <div class="search-bar">
      <input
        v-model="clientsSearchQuery"
        type="text"
        placeholder="Search by name, email, or phone..."
        class="search-input"
      />
      <button type="button" class="btn" @click="clearClientsSearch" v-if="clientsSearchQuery">❌ Clear</button>
    </div>

    <div class="list-state" v-if="clientsLoading">Loading clients...</div>
    <div class="list-state error" v-else-if="clientsError">{{ clientsError }}</div>

    <div class="table" v-else>
      <div class="table-header clients-table">
        <span>Name</span>
        <span>Email</span>
        <span>Phone</span>
        <span>Consultations</span>
        <span>Actions</span>
      </div>
      <div v-for="client in pagedFilteredClients" :key="client.id" class="table-row clients-table">
        <span>{{ client.name }}</span>
        <span>{{ client.email }}</span>
        <span>{{ client.phone }}</span>
        <span>{{ client.consultationIds?.length || 0 }}</span>
        <span class="row-actions">
          <button type="button" class="btn" @click="startEditClient(client)">✏️ Select</button>
          <button type="button" class="btn danger" @click="removeClient(client.id)">🗑️ Delete</button>
        </span>
      </div>
    </div>

    <div v-if="!clientsLoading && !clientsError" class="pagination">
      <div class="pagination-info">
        Page {{ clientCurrentPage }}
      </div>
      <div class="pagination-controls">
        <button type="button" class="btn" :disabled="clientCurrentPage === 1" @click="goToPreviousClientPage">
          ⬅️ Previous
        </button>
        <button type="button" class="btn" :disabled="isLastClientPage" @click="goToNextClientPage">
          Next ➡️
        </button>
      </div>
      <div class="pagination-size">
        <label for="client-page-size">Page size</label>
        <select id="client-page-size" v-model.number="clientPageSize" @change="handleClientPageSizeChange">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </div>
    </div>

    <form ref="clientFormRef" class="form" @submit.prevent>
      <div class="form-grid">
        <div class="form-field">
          <label for="client-name">Name</label>
          <input id="client-name" v-model="clientForm.name" type="text" placeholder="John Doe" />
        </div>
        <div class="form-field">
          <label for="client-email">Email</label>
          <input id="client-email" v-model="clientForm.email" type="email" placeholder="john@example.com" />
        </div>
        <div class="form-field">
          <label for="client-phone">Phone</label>
          <input id="client-phone" v-model="clientForm.phone" type="tel" placeholder="+1234567890" />
        </div>
      </div>

      <div class="form-actions">
        <button type="button" class="btn primary" @click="addClient">➕ Add Client</button>
        <button type="button" class="btn" :disabled="!selectedClientId" @click="updateClient">
          ✏️ Update Client
        </button>
        <button type="button" class="btn danger" :disabled="!selectedClientId" @click="deleteSelectedClient">
          🗑️ Delete Client
        </button>
        <button type="button" class="btn" @click="resetClientForm">❌ Clear</button>
      </div>

      <p v-if="clientActionMessage" class="form-message">{{ clientActionMessage }}</p>
    </form>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, nextTick } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'

defineProps<{ visible: boolean }>()

const config = useRuntimeConfig()

type Client = {
  id: string
  name: string
  email: string
  phone: string
  consultationIds?: string[]
}

const clients = ref<Client[]>([])
const clientsLoading = ref(false)
const clientsError = ref('')
const selectedClientId = ref<string | null>(null)
const clientActionMessage = ref('')
const clientCurrentPage = ref(1)
const clientPageSize = ref(20)
const clientsSearchQuery = ref('')
const clientFormRef = ref<HTMLFormElement | null>(null)
const clientForm = ref({
  name: '',
  email: '',
  phone: '',
})

const confirmState = ref({
  visible: false,
  title: '',
  message: '',
})
const confirmResolver = ref<((value: boolean) => void) | null>(null)

const clientTotalPages = computed(() => Math.max(1, Math.ceil(filteredClients.value.length / clientPageSize.value)))
const isLastClientPage = computed(() => clientCurrentPage.value === clientTotalPages.value)

const filteredClients = computed(() => {
  if (!clientsSearchQuery.value.trim()) {
    return clients.value
  }

  const query = clientsSearchQuery.value.toLowerCase().trim()
  return clients.value.filter(client => {
    return (
      client.name.toLowerCase().includes(query) ||
      client.email.toLowerCase().includes(query) ||
      client.phone.toLowerCase().includes(query)
    )
  })
})

const pagedFilteredClients = computed(() => {
  const start = (clientCurrentPage.value - 1) * clientPageSize.value
  return filteredClients.value.slice(start, start + clientPageSize.value)
})

const confirmAction = (title: string, message: string) =>
  new Promise<boolean>(resolve => {
    confirmState.value = { visible: true, title, message }
    confirmResolver.value = resolve
  })

const loadClients = async () => {
  clientsLoading.value = true
  clientsError.value = ''

  try {
    const data = await $fetch<Client[]>(`${config.public.apiBase}/users?offset=0&limit=1000`, {
      method: 'GET',
    })
    // Filter only clients (users with Client role)
    clients.value = data.filter((user: any) => user.role === 'Client').map((user: any) => ({
      id: user.id,
      name: user.name || '',
      email: user.email || '',
      phone: user.phone || '',
      consultationIds: user.consultationIds || [],
    }))
    if (clientCurrentPage.value > clientTotalPages.value) {
      clientCurrentPage.value = clientTotalPages.value
    }
  } catch (error) {
    clients.value = []
    clientsError.value = 'Failed to load clients'
  } finally {
    clientsLoading.value = false
  }
}

const clearClientsSearch = () => {
  clientsSearchQuery.value = ''
  clientCurrentPage.value = 1
}

const goToPreviousClientPage = () => {
  if (clientCurrentPage.value > 1) {
    clientCurrentPage.value -= 1
  }
}

const goToNextClientPage = () => {
  if (clientCurrentPage.value < clientTotalPages.value) {
    clientCurrentPage.value += 1
  }
}

const handleClientPageSizeChange = () => {
  clientCurrentPage.value = 1
}

const startEditClient = (client: Client) => {
  selectedClientId.value = client.id
  clientForm.value = {
    name: client.name,
    email: client.email,
    phone: client.phone,
  }
  clientActionMessage.value = ''
  // Scroll to form and focus
  nextTick(() => {
    if (clientFormRef.value) {
      clientFormRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
      const firstInput = clientFormRef.value.querySelector('input') as HTMLInputElement
      if (firstInput) {
        setTimeout(() => firstInput.focus(), 300)
      }
    }
  })
}

const resetClientForm = () => {
  selectedClientId.value = null
  clientForm.value = {
    name: '',
    email: '',
    phone: '',
  }
  clientActionMessage.value = ''
}

const addClient = async () => {
  if (!clientForm.value.name || !clientForm.value.email || !clientForm.value.phone) {
    clientActionMessage.value = 'Please fill in all required fields'
    return
  }

  try {
    clientActionMessage.value = ''
    await $fetch(`${config.public.apiBase}/register`, {
      method: 'POST',
      body: {
        login: clientForm.value.email.split('@')[0],
        password: 'DefaultPassword123!',
        name: clientForm.value.name,
        email: clientForm.value.email,
        phone: clientForm.value.phone,
        role: 'Client',
      },
    })
    clientActionMessage.value = 'Client added successfully!'
    resetClientForm()
    await loadClients()
  } catch (error) {
    clientActionMessage.value = 'Failed to add client'
  }
}

const updateClient = async () => {
  if (!selectedClientId.value) return
  if (!clientForm.value.name || !clientForm.value.email || !clientForm.value.phone) {
    clientActionMessage.value = 'Please fill in all required fields'
    return
  }

  try {
    clientActionMessage.value = ''
    await $fetch(`${config.public.apiBase}/users/${selectedClientId.value}`, {
      method: 'PUT',
      body: {
        name: clientForm.value.name,
        email: clientForm.value.email,
        phone: clientForm.value.phone,
      },
    })
    clientActionMessage.value = 'Client updated successfully!'
    await loadClients()
  } catch (error) {
    clientActionMessage.value = 'Failed to update client'
  }
}

const removeClient = async (id: string) => {
  const confirmed = await confirmAction('Delete Client', 'Are you sure you want to delete this client?')
  if (!confirmed) return

  try {
    clientActionMessage.value = ''
    await $fetch(`${config.public.apiBase}/users/${id}`, {
      method: 'DELETE',
    })
    clientActionMessage.value = 'Client deleted successfully!'
    if (selectedClientId.value === id) {
      resetClientForm()
    }
    await loadClients()
  } catch (error) {
    clientActionMessage.value = 'Failed to delete client'
  }
}

const deleteSelectedClient = async () => {
  if (!selectedClientId.value) return
  await removeClient(selectedClientId.value)
}

onMounted(() => {
  loadClients()
})
</script>

<style scoped>
.section {
  margin-top: 1.25rem;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 1rem;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.section h2 {
  margin: 0;
  font-size: 1.25rem;
  color: #111827;
}

.search-bar {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.search-input {
  flex: 1;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.9rem;
}

.search-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.1);
}

.list-state {
  padding: 1rem;
  text-align: center;
  color: #6b7280;
}

.list-state.error {
  color: #dc2626;
  background: #fee2e2;
  border-radius: 6px;
}

.table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1rem;
}

.table-header {
  display: grid;
  gap: 0.75rem;
  padding: 0.75rem;
  background: #f9fafb;
  border-bottom: 2px solid #e5e7eb;
  font-weight: 600;
  color: #111827;
}

.clients-table {
  grid-template-columns: 1.5fr 2fr 1.5fr 1fr 2fr;
}

.table-row {
  display: grid;
  gap: 0.75rem;
  padding: 0.75rem;
  border-bottom: 1px solid #e5e7eb;
  align-items: center;
}

.table-row:hover {
  background: #f9fafb;
}

.row-actions {
  display: flex;
  gap: 0.5rem;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 0.75rem;
  background: #f9fafb;
  border-radius: 6px;
}

.pagination-info {
  font-size: 0.9rem;
  color: #6b7280;
}

.pagination-controls {
  display: flex;
  gap: 0.5rem;
}

.pagination-size {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pagination-size label {
  font-size: 0.9rem;
  color: #6b7280;
}

.pagination-size select {
  padding: 0.35rem 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 0.85rem;
}

.form {
  margin-top: 1.5rem;
  padding: 1rem;
  background: #f9fafb;
  border-radius: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
}

.form-field label {
  font-weight: 600;
  margin-bottom: 0.25rem;
  color: #111827;
  font-size: 0.9rem;
}

.form-field input {
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.9rem;
}

.form-field input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.1);
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
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
  transition: all 0.2s;
}

.btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.btn.primary {
  background: #007bff;
  color: #ffffff;
  border-color: #0056b3;
}

.btn.primary:hover:not(:disabled) {
  background: #0056b3;
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

.form-message {
  padding: 0.75rem;
  margin: 0;
  border-radius: 6px;
  background: #dbeafe;
  color: #0c4a6e;
  font-size: 0.9rem;
}

.muted {
  color: #6b7280;
  font-size: 0.9rem;
}
</style>
