<template>
  <div class="main-container">
    <nav class="menu-panel">
      <div class="menu-title">Client Menu</div>
      <ul>
        <li :class="{ active: selectedMenu === 'profile' }" @click="selectMenu('profile')">Profile</li>
        <li :class="{ active: selectedMenu === 'connections' }" @click="selectMenu('connections')">My Connections</li>
        <li :class="{ active: selectedMenu === 'consultations' }" @click="selectMenu('consultations')">My Consultations</li>
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

      <!-- Consultations Section -->
      <div v-if="selectedMenu === 'consultations'" class="section">
        <h3>My Consultations</h3>
        
        <!-- Tabs for View and Book -->
        <div class="tabs-header">
          <button 
            :class="['tab-btn', activeTab === 'view' ? 'active' : '']"
            @click="activeTab = 'view'"
          >
            View Consultations
          </button>
          <button 
            :class="['tab-btn', activeTab === 'book' ? 'active' : '']"
            @click="activeTab = 'book'"
          >
            Book Consultation
          </button>
        </div>
        
        <!-- View Consultations Tab -->
        <div v-if="activeTab === 'view'" class="tab-panel">
          <div class="form" style="margin-bottom: 2rem;">
            <h4>Filter Consultations</h4>
            <div class="form-grid">
              <div class="form-field">
                <label>Status</label>
                <select v-model="consultationFilters.status">
                  <option value="">All Status</option>
                  <option value="Requested">Requested</option>
                  <option value="Scheduled">Scheduled</option>
                  <option value="InProgress">In Progress</option>
                  <option value="Completed">Completed</option>
                  <option value="Missed">Missed</option>
                  <option value="Cancelled">Cancelled</option>
                </select>
              </div>
              <div class="form-field">
                <label>From Date</label>
                <input type="date" v-model="consultationFilters.fromDate">
              </div>
              <div class="form-field">
                <label>To Date</label>
                <input type="date" v-model="consultationFilters.toDate">
              </div>
              <div class="form-field">
                <label>Search</label>
                <input type="text" v-model="consultationFilters.search" placeholder="Search by specialist or description">
              </div>
            </div>
            <div class="form-actions">
              <button class="btn btn-secondary" @click="clearConsultationFilters">Clear Filters</button>
              <button class="btn btn-primary" @click="loadConsultations">Apply Filters</button>
            </div>
          </div>

          <div v-if="consultationsLoading" class="list-state">Loading consultations...</div>
          <div v-else-if="consultationsError" class="list-state error">{{ consultationsError }}</div>
          <div v-else-if="consultations.length === 0" class="empty-state">
            <div class="empty-icon">📋</div>
            <h3>No consultations yet</h3>
            <p>You haven't booked any consultations. Use the "Book Consultation" tab to schedule your first consultation.</p>
          </div>
          <div v-else class="consultation-list">
            <div v-if="filteredConsultations.length === 0" class="empty-state">
              No consultations match your filters. Try adjusting your search criteria.
            </div>
            <div v-else>
              <div v-for="consultation in filteredConsultations" :key="consultation.id" class="consultation-item">
                <div class="consultation-header">
                  <div class="consultation-title">{{ consultation.description }}</div>
                  <div class="consultation-status" :class="consultation.status.toLowerCase()">
                    {{ consultation.status }}
                  </div>
                </div>
                <div class="consultation-details">
                  <div>Specialist: {{ consultation.specialistId }}</div>
                  <div>Duration: {{ consultation.duration }} minutes</div>
                  <div>Price: {{ consultation.price === 0 ? 'Free' : `$${consultation.price}` }}</div>
                </div>
              </div>
            </div>

            <!-- Pagination -->
            <div v-if="consultationPagination.totalPages > 1" class="pagination">
              <button 
                class="pagination-btn" 
                :disabled="consultationPagination.currentPage === 1"
                @click="goToPage(consultationPagination.currentPage - 1)"
              >
                Previous
              </button>
              <span class="pagination-info">
                Page {{ consultationPagination.currentPage }} of {{ consultationPagination.totalPages }}
                ({{ consultationPagination.totalCount }} total)
              </span>
              <button 
                class="pagination-btn" 
                :disabled="consultationPagination.currentPage === consultationPagination.totalPages"
                @click="goToPage(consultationPagination.currentPage + 1)"
              >
                Next
              </button>
            </div>
          </div>
        </div>

        <!-- Book Consultation Tab -->
        <div v-if="activeTab === 'book'" class="tab-panel">
          <div class="form" @click="closeDropdownsOnFormClick">
            <h4>Book New Consultation</h4>

            <div class="form-grid">
              <!-- Specialist selection -->
              <div class="form-field specialist-field" style="position: relative;">
                <label>Specialist *</label>
                <input 
                  type="text" 
                  v-model="specialistSearch" 
                  placeholder="Type to search specialist"
                  @focus="handleSpecialistFocus"
                  @click="handleSpecialistClick"
                  @input="handleSpecialistInput"
                />
                <div v-if="showSpecialistDropdown" class="dropdown" @click.stop style="display: block; background: white; border: 1px solid #e5e7eb; position: absolute; top: 100%; left: 0; right: 0; z-index: 1000; width: 100%; max-height: 300px; overflow-y: auto; border-radius: 6px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
                  <div v-if="specialistsLoading" style="padding: 0.75rem;">
                    <div class="text-gray-500">Loading specialists...</div>
                  </div>
                  <div v-else style="background: white;">
                    <div v-if="filteredSpecialists.length > 0" style="padding: 0;">
                      <div 
                        v-for="(s) in filteredSpecialists" 
                        :key="s.id"
                        @click="selectSpecialist(s)"
                        style="padding: 0.75rem; cursor: pointer; background: white; border-bottom: 1px solid #f3f4f6; color: #374151; font-size: 14px; transition: background 0.2s;"
                        @mouseenter="($event.target as HTMLElement).style.background = '#f9fafb'"
                        @mouseleave="($event.target as HTMLElement).style.background = 'white'"
                      >
                        {{ s.name }}
                      </div>
                    </div>
                    <div v-else style="padding: 0.75rem; color: #666; font-size: 14px;">
                      No specialists found
                    </div>
                  </div>
                </div>
              </div>

              <!-- Category selection -->
              <div class="form-field category-field" style="position: relative;">
                <label>Category *</label>
                <input 
                  type="text" 
                  :value="selectedCategory?.name || ''" 
                  readonly 
                  placeholder="Select category"
                  @focus="handleCategoryFocus"
                  @click="handleCategoryClick"
                />
                <div v-if="showCategoryDropdown" class="dropdown" @click.stop style="display: block; background: white; border: 1px solid #e5e7eb; position: absolute; top: 100%; left: 0; right: 0; z-index: 1000; width: 100%; max-height: 300px; overflow-y: auto; border-radius: 6px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
                  <div v-if="categoriesLoading" style="padding: 0.75rem;">
                    <div class="text-gray-500">Loading categories...</div>
                  </div>
                  <div v-else style="background: white;">
                    <div v-if="filteredCategories.length > 0" style="padding: 0;">
                      <div 
                        v-for="(c) in filteredCategories" 
                        :key="c.id"
                        @click="selectCategoryFromDropdown(c)"
                        style="padding: 0.75rem; cursor: pointer; background: white; border-bottom: 1px solid #f3f4f6; color: #374151; font-size: 14px; transition: background 0.2s;"
                        @mouseenter="($event.target as HTMLElement).style.background = '#f9fafb'"
                        @mouseleave="($event.target as HTMLElement).style.background = 'white'"
                      >
                        {{ c.name }}
                      </div>
                    </div>
                    <div v-else style="padding: 0.75rem; color: #666; font-size: 14px;">
                      <div v-if="!selectedSpecialist">Select a specialist first</div>
                      <div v-else>No categories available</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Description -->
              <div class="form-field" style="grid-column: 1 / -1;">
                <label>Description *</label>
                <textarea v-model="consultationForm.description" rows="3" placeholder="Describe your consultation request"></textarea>
              </div>

              <!-- Date -->
              <div class="form-field">
                <label>Scheduled Date *</label>
                <input type="date" v-model="consultationForm.scheduledDate" />
              </div>

              <!-- Time -->
              <div class="form-field">
                <label>Scheduled Time *</label>
                <input type="time" v-model="consultationForm.scheduledTime" />
              </div>

              <!-- Available Slots -->
              <div v-if="selectedSpecialist && consultationForm.scheduledDate" class="form-field" style="grid-column: 1 / -1;">
                <label>Available Slots</label>
                <div v-if="slotsLoading" class="slots-container">
                  <div class="slots-spinner">Loading available slots...</div>
                </div>
                <div v-else-if="slotsError" class="slots-container error">
                  <p>{{ slotsError }}</p>
                </div>
                <div v-else-if="availableSlots.length > 0" class="slots-container">
                  <div class="slots-grid">
                    <button 
                      v-for="(slot, idx) in availableSlots" 
                      :key="idx"
                      type="button"
                      class="slot-button"
                      :class="{ selected: consultationForm.scheduledTime === slot.startTime }"
                      @click="selectSlot(slot)"
                    >
                      {{ slot.startTime }} - {{ slot.endTime }}
                    </button>
                  </div>
                </div>
                <div v-else class="slots-container empty">
                  <p>No available slots for the selected date and duration</p>
                </div>
              </div>
            </div>

            <div class="form-actions">
              <button 
                class="btn btn-primary" 
                @click="createConsultation"
                :disabled="!isConsultationFormValid"
              >
                {{ consultationCreating ? 'Booking...' : 'Book Consultation' }}
              </button>
            </div>

            <div v-if="consultationMessage" :class="['form-message', consultationSuccess ? 'success' : 'error']">
              {{ consultationMessage }}
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
import { ref, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'

const router = useRouter()
const config = useRuntimeConfig()

// Menu state
const selectedMenu = ref('profile')
const activeTab = ref('view')

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

// Consultations state
const consultationsLoading = ref(false)
const consultationsError = ref('')
const consultations = ref<any[]>([])
const consultationFilters = ref({
  status: '',
  fromDate: '',
  toDate: '',
  search: ''
})
const consultationPagination = ref({
  currentPage: 1,
  pageSize: 10,
  totalCount: 0,
  totalPages: 0
})
const consultationForm = ref({
  specialistId: '',
  categoryId: '',
  description: '',
  scheduledDate: '',
  scheduledTime: ''
})
const consultationCreating = ref(false)
const consultationMessage = ref('')
const consultationSuccess = ref(false)

// Availability slots state
const availableSlots = ref<any[]>([])
const slotsLoading = ref(false)
const slotsError = ref('')

// Specialists and categories for dropdowns
const specialists = ref<any[]>([])
const categories = ref<any[]>([])
const specialistsLoading = ref(false)
const categoriesLoading = ref(false)
const specialistSearch = ref('')
const selectedSpecialist = ref<any>(null)
const selectedCategory = ref<any>(null)
const showSpecialistDropdown = ref(false)
const showCategoryDropdown = ref(false)
const justOpenedSpecialistDropdown = ref(false)
const justOpenedCategoryDropdown = ref(false)

const selectMenu = (menu: string) => {
  console.log('Selecting menu:', menu)
  selectedMenu.value = menu
  if (menu === 'profile') loadProfile()
  if (menu === 'connections') loadConnections()
  if (menu === 'consultations') {
    console.log('Loading consultations data...')
    loadConsultations()
    loadSpecialists()
    loadCategories()
  }
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

// Specialists and categories operations
const loadSpecialists = async () => {
  specialistsLoading.value = true
  try {
    // Backend search endpoint doesn't support text search, only structured filters
    // So we fetch without filters (uses defaults: offset=0, limit=20)
    // and filter on client side using specialistSearch
    const url = `${config.public.apiBase}/specialists/search`
    const response = await $fetch(url)
    
    if (Array.isArray(response)) {
      specialists.value = response
    } else {
      console.error('API did not return an array:', response)
      specialists.value = []
    }
  } catch (error: any) {
    console.error('Specialists load error:', error?.message || error)
    specialists.value = []
  } finally {
    specialistsLoading.value = false
  }
}

const loadCategories = async () => {
  categoriesLoading.value = true
  try {
    const data = await $fetch(`${config.public.apiBase}/categories`)
    categories.value = data || []
  } catch (error: any) {
    console.error('Categories load error:', error)
    // Don't show error to user, just log it
  } finally {
    categoriesLoading.value = false
  }
}

const filteredSpecialists = computed(() => {
  if (!specialistSearch.value) return specialists.value
  return specialists.value.filter(specialist =>
    specialist.name.toLowerCase().includes(specialistSearch.value.toLowerCase()) ||
    specialist.email.toLowerCase().includes(specialistSearch.value.toLowerCase()) ||
    (specialist.bio && specialist.bio.toLowerCase().includes(specialistSearch.value.toLowerCase()))
  )
})

const filteredCategories = computed(() => {
  if (!selectedSpecialist.value || !selectedSpecialist.value.categoryRates || selectedSpecialist.value.categoryRates.length === 0) {
    return categories.value
  }
  
  if (categories.value.length === 0) {
    return []
  }
  
  // Filter categories to only show those that the selected specialist offers
  const specialistCategoryIds = selectedSpecialist.value.categoryRates.map((rate: any) => rate.categoryId)
  const filtered = categories.value.filter(category => specialistCategoryIds.includes(category.id))
  return filtered
})

const selectedCategoryName = computed(() => {
  const name = selectedCategory.value?.name || ''
  return name
})

const isConsultationFormValid = computed(() => {
  return !consultationCreating.value &&
    consultationForm.value.specialistId &&
    consultationForm.value.categoryId &&
    consultationForm.value.description &&
    consultationForm.value.scheduledDate &&
    consultationForm.value.scheduledTime
    // Duration is not required - specialist will set it
})

const filteredConsultations = computed(() => {
  let filtered = consultations.value

  // Filter by status
  if (consultationFilters.value.status) {
    filtered = filtered.filter(c => c.status === consultationFilters.value.status)
  }

  // Filter by date range
  if (consultationFilters.value.fromDate) {
    const fromDate = new Date(consultationFilters.value.fromDate)
    filtered = filtered.filter(c => new Date(c.createdAt || c.date) >= fromDate)
  }

  if (consultationFilters.value.toDate) {
    const toDate = new Date(consultationFilters.value.toDate)
    toDate.setHours(23, 59, 59, 999) // End of day
    filtered = filtered.filter(c => new Date(c.createdAt || c.date) <= toDate)
  }

  // Filter by search text
  if (consultationFilters.value.search) {
    const search = consultationFilters.value.search.toLowerCase()
    filtered = filtered.filter(c => 
      c.description?.toLowerCase().includes(search) ||
      c.specialistId?.toLowerCase().includes(search)
    )
  }

  // Update pagination info based on filtered results
  const totalFiltered = filtered.length
  const totalPages = Math.ceil(totalFiltered / consultationPagination.value.pageSize)
  consultationPagination.value.totalPages = totalPages

  // Apply pagination
  const startIndex = (consultationPagination.value.currentPage - 1) * consultationPagination.value.pageSize
  const endIndex = startIndex + consultationPagination.value.pageSize
  const paginated = filtered.slice(startIndex, endIndex)

  return paginated
})

const selectSpecialist = (specialist: any) => {
  selectedSpecialist.value = specialist
  consultationForm.value.specialistId = specialist.id
  specialistSearch.value = specialist.name
  // Reset category selection when specialist changes
  consultationForm.value.categoryId = ''
  selectedCategory.value = null
  showSpecialistDropdown.value = false
  
  // Auto-open category dropdown to show available categories for this specialist
  setTimeout(() => {
    showCategoryDropdown.value = true
  }, 100)
}

const selectCategoryFromDropdown = (category: any) => {
  selectedCategory.value = category
  consultationForm.value.categoryId = category.id
  showCategoryDropdown.value = false
}

// Specialist field event handlers
const handleSpecialistFocus = (e: Event) => {
  e.stopImmediatePropagation()
  showSpecialistDropdown.value = true
  justOpenedSpecialistDropdown.value = true
  // Clear the flag after the click event completes
  setTimeout(() => { justOpenedSpecialistDropdown.value = false }, 50)
  if (specialists.value.length === 0) loadSpecialists()
}

const handleSpecialistClick = (e: Event) => {
  e.stopImmediatePropagation()
  showSpecialistDropdown.value = true
  justOpenedSpecialistDropdown.value = true
  // Clear the flag after the click event completes
  setTimeout(() => { justOpenedSpecialistDropdown.value = false }, 50)
}

const handleSpecialistInput = (e: Event) => {
  e.stopImmediatePropagation()
  showSpecialistDropdown.value = true
  if (specialists.value.length === 0) loadSpecialists()
}

// Category field event handlers
const handleCategoryFocus = (e: Event) => {
  e.stopImmediatePropagation()
  showCategoryDropdown.value = true
  if (categories.value.length === 0) loadCategories()
}

const handleCategoryClick = (e: Event) => {
  e.stopImmediatePropagation()
  showCategoryDropdown.value = true
  if (categories.value.length === 0) loadCategories()
}

// Close dropdown when clicking outside
const closeDropdowns = (e: MouseEvent) => {
  // Check all elements in the event path to see if we're inside the specialist or category field
  const path = e.composedPath() as HTMLElement[]
  
  const insideSpecialist = path.some(el => el.classList?.contains('specialist-field'))
  const insideCategory = path.some(el => el.classList?.contains('category-field'))
  const insideDropdown = path.some(el => el.classList?.contains('dropdown'))
  
  // Only close if not clicking inside the field or dropdown
  if (!insideDropdown && !insideSpecialist) {
    showSpecialistDropdown.value = false
  }
  if (!insideDropdown && !insideCategory) {
    showCategoryDropdown.value = false
  }
}

// Add click outside listener
// Handle Escape key to close dropdowns
const handleEscapeKey = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    showSpecialistDropdown.value = false
    showCategoryDropdown.value = false
  }
}

// Close dropdowns when clicking on form elements outside of fields
const closeDropdownsOnFormClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  const path = (e as any).composedPath() as HTMLElement[]
  
  const insideSpecialist = path.some(el => el.classList?.contains('specialist-field'))
  const insideCategory = path.some(el => el.classList?.contains('category-field'))
  const insideDropdown = path.some(el => el.classList?.contains('dropdown'))
  
  console.log('Form click - insideSpecialist:', insideSpecialist, 'insideCategory:', insideCategory, 'insideDropdown:', insideDropdown)
  
  // Close dropdowns if clicking outside the fields and dropdowns
  if (!insideSpecialist && !insideDropdown) {
    showSpecialistDropdown.value = false
  }
  if (!insideCategory && !insideDropdown) {
    showCategoryDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleEscapeKey)
  
  // Watch for changes to specialist or date to reload available slots
  watch([() => consultationForm.value.specialistId, () => consultationForm.value.scheduledDate], () => {
    loadAvailableSlots()
  })
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscapeKey)
})

// Consultations operations
const loadConsultations = async () => {
  consultationsLoading.value = true
  consultationsError.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      consultationsError.value = 'User ID not found'
      return
    }
    // Load all consultations for client-side pagination
    const data = await $fetch(`${config.public.apiBase}/consultations/user/${userId}`)
    console.log('Loaded consultations:', data)
    consultations.value = data
    consultationPagination.value.totalCount = data.length
    consultationPagination.value.totalPages = Math.ceil(data.length / consultationPagination.value.pageSize)
    consultationPagination.value.currentPage = 1 // Reset to first page when loading
  } catch (error: any) {
    console.error('Consultations load error:', error)
    consultationsError.value = error.data?.message || error.message || 'Failed to load consultations'
  } finally {
    consultationsLoading.value = false
  }
}

const clearConsultationFilters = () => {
  consultationFilters.value = {
    status: '',
    fromDate: '',
    toDate: '',
    search: ''
  }
  consultationPagination.value.currentPage = 1
}

const goToPage = (page: number) => {
  if (page >= 1 && page <= consultationPagination.value.totalPages) {
    consultationPagination.value.currentPage = page
  }
}

// Load available time slots for specialist and date
const loadAvailableSlots = async () => {
  if (!consultationForm.value.specialistId || !consultationForm.value.scheduledDate) {
    availableSlots.value = []
    return
  }

  slotsLoading.value = true
  slotsError.value = ''
  availableSlots.value = []

  try {
    const response = await $fetch(`${config.public.apiBase}/specialists/${consultationForm.value.specialistId}/availability/slots`, {
      query: {
        date: consultationForm.value.scheduledDate,
        durationMinutes: 60
      }
    })
    
    if (response.slots && Array.isArray(response.slots)) {
      availableSlots.value = response.slots
      if (response.slots.length === 0) {
        slotsError.value = 'No available slots for selected date'
      }
    }
  } catch (error: any) {
    console.error('Slots load error:', error)
    slotsError.value = error.data?.message || error.message || 'Failed to load available slots'
    availableSlots.value = []
  } finally {
    slotsLoading.value = false
  }
}

// Set selected slot time
const selectSlot = (slot: any) => {
  consultationForm.value.scheduledTime = slot.startTime
}

const createConsultation = async () => {
  consultationCreating.value = true
  consultationMessage.value = ''
  consultationSuccess.value = false
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      consultationMessage.value = 'User ID not found'
      return
    }
    
    // Combine date and time into ISO string
    const scheduledDateTime = consultationForm.value.scheduledDate && consultationForm.value.scheduledTime 
      ? new Date(`${consultationForm.value.scheduledDate}T${consultationForm.value.scheduledTime}`).toISOString()
      : null
    
    const body = {
      userId,
      specialistId: consultationForm.value.specialistId,
      categoryId: consultationForm.value.categoryId,
      description: consultationForm.value.description,
      scheduledAt: scheduledDateTime,
      duration: 60 // Default duration
    }
    
    const result = await $fetch(`${config.public.apiBase}/consultations`, {
      method: 'POST',
      body
    })
    
    consultationMessage.value = 'Consultation created successfully!'
    consultationSuccess.value = true
    consultationForm.value = {
      specialistId: '',
      categoryId: '',
      description: '',
      scheduledDate: '',
      scheduledTime: ''
    }
    selectedSpecialist.value = null
    selectedCategory.value = null
    specialistSearch.value = ''
    
    // Reload consultations to show the new one
    await loadConsultations()
    
  } catch (error: any) {
    console.error('Consultation create error:', error)
    consultationMessage.value = error.data?.message || error.message || 'Failed to create consultation'
  } finally {
    consultationCreating.value = false
  }
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

.list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.list-item {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.15s ease-in-out;
}

.list-item:hover {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.list-item-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.list-item-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #111827;
}

.list-item-subtitle {
  font-size: 0.875rem;
  color: #6b7280;
}

.list-item-details {
  font-size: 0.8125rem;
  color: #9ca3af;
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
  background: white;
  color: #374151;
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

/* Dropdown styles */
.dropdown-container {
  position: relative;
}

.dropdown-input {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e5e7eb;
  border-radius: 6px;
  font-size: 0.875rem;
  background: white;
  color: #374151;
  transition: all 0.15s;
}

.dropdown-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 6px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  max-height: 200px;
  overflow-y: auto;
  z-index: 1000;
  margin-top: 2px;
}

.dropdown-item {
  padding: 0.75rem;
  cursor: pointer;
  border-bottom: 1px solid #f3f4f6;
  transition: background-color 0.15s ease-in-out;
  color: #374151; /* Ensure text is visible */
  background: white; /* Ensure background is white */
}

.dropdown-item:last-child {
  border-bottom: none;
}

.dropdown-item:hover {
  background: #f9fafb;
}

.dropdown-item.loading,
.dropdown-item.no-results {
  color: #6b7280;
  font-style: italic;
  cursor: default;
}

.dropdown-item.no-results:hover {
  background: white;
}

.specialist-name {
  font-weight: 600;
  color: #111827;
  margin-bottom: 0.25rem;
}

.specialist-email {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.25rem;
}

.specialist-bio {
  font-size: 0.75rem;
  color: #9ca3af;
  margin-bottom: 0.25rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.specialist-status {
  font-size: 0.75rem;
  font-weight: 500;
}

.specialist-status.available {
  color: #059669;
}

.specialist-status:not(.available) {
  color: #dc2626;
}

.form-select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  background: white;
  transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
}

.form-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.debug-info {
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: #6b7280;
  font-family: monospace;
  background: #f9fafb;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
}

/* Simple Tabs Styling */
.simple-tabs {
  margin-top: 1rem;
}

.tab-buttons {
  display: flex;
  border-bottom: 2px solid #e5e7eb;
  margin-bottom: 1.5rem;
}

.tab-button {
  padding: 0.75rem 1.5rem;
  border: none;
  background: transparent;
  color: #6b7280;
  font-weight: 500;
  cursor: pointer;
  border-radius: 6px 6px 0 0;
  transition: all 0.2s ease;
}

.tab-button:hover {
  color: #374151;
  background: #f9fafb;
}

.tab-button.active {
  color: #667eea;
  background: white;
  border-bottom: 2px solid #667eea;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.tab-content {
  padding: 1.5rem 0;
}

/* Consultation Tabs Styling */
.consultation-tabs {
  margin-top: 1rem;
}

.consultation-tabs .p-tabview-panels {
  padding: 0;
  border: none;
  background: transparent;
}

.consultation-tabs .p-tabview-panel {
  padding: 1.5rem 0;
  border: none;
  background: transparent;
}

.consultation-tabs .p-tabview-nav {
  border-bottom: 2px solid #e5e7eb;
  margin-bottom: 1.5rem;
}

.consultation-tabs .p-tabview-nav-link {
  border: none;
  border-radius: 6px 6px 0 0;
  padding: 0.75rem 1.5rem;
  font-weight: 500;
  color: #6b7280;
  background: transparent;
  transition: all 0.2s ease;
}

.consultation-tabs .p-tabview-nav-link:hover {
  color: #374151;
  background: #f9fafb;
}

.consultation-tabs .p-tabview-nav-link:not(.p-disabled).p-highlight {
  color: #667eea;
  background: white;
  border-bottom: 2px solid #667eea;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* Custom Tabs Styling */
.tabs-header {
  display: flex;
  gap: 0.5rem;
  border-bottom: 2px solid #e5e7eb;
  margin-bottom: 1.5rem;
}

.tab-btn {
  padding: 0.75rem 1.5rem;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: #6b7280;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  bottom: -2px;
}

.tab-btn:hover {
  color: #374151;
  background: #f9fafb;
}

.tab-btn.active {
  color: #667eea;
  border-bottom-color: #667eea;
  background: white;
}

.tab-panel {
  padding: 1.5rem 0;
}

/* Ensure form headings are visible and spaced */
.form h4 {
  color: #1f2937;
  margin-bottom: 1rem;
  font-weight: 600;
}

/* Consultation List Styling */
.consultation-list {
  margin-top: 1rem;
}

.consultation-item {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1rem;
  transition: box-shadow 0.2s ease;
}

.consultation-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.consultation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.consultation-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #1f2937;
}

.consultation-status {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 500;
  text-transform: capitalize;
}

.consultation-status.requested {
  background: #dbeafe;
  color: #1e40af;
}

.consultation-status.confirmed {
  background: #d1fae5;
  color: #065f46;
}

.consultation-status.scheduled {
  background: #d1fae5;
  color: #065f46;
}

.consultation-status.inprogress {
  background: #fef3c7;
  color: #92400e;
}

.consultation-status.completed {
  background: #e0e7ff;
  color: #3730a3;
}

.consultation-status.missed {
  background: #fed7aa;
  color: #92400e;
}

.consultation-status.cancelled {
  background: #fee2e2;
  color: #991b1b;
}

.consultation-status.cancelled {
  background: #fee2e2;
  color: #991b1b;
}

.consultation-details {
  color: #6b7280;
  font-size: 0.875rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: #9ca3af;
  font-size: 0.95rem;
}

.empty-state .empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: #374151;
  font-size: 1.25rem;
  margin-bottom: 0.5rem;
}

.empty-state p {
  color: #6b7280;
  max-width: 400px;
  margin: 0 auto;
}

/* Dropdown styling for booking form */
.dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
  max-height: 240px;
  overflow-y: auto;
  z-index: 1000;
  width: 100%;
}

.dropdown-menu {
  max-height: inherit;
  overflow-y: auto;
}

.dropdown-item {
  padding: 0.75rem 1rem;
  cursor: pointer;
  color: #374151;
}

.dropdown-item:hover {
  background: #f9fafb;
}

/* Native input styling */
.form-field input[type="date"],
.form-field input[type="time"],
.form-field input[type="number"],
.form-field input[type="text"],
.form-field input[type="email"],
.form-field input[type="tel"] {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 1rem;
  font-family: inherit;
  background-color: #ffffff;
  color: #1f2937;
}

.form-field input[type="date"]:focus,
.form-field input[type="time"]:focus,
.form-field input[type="number"]:focus,
.form-field input[type="text"]:focus,
.form-field input[type="email"]:focus,
.form-field input[type="tel"]:focus {
  border-color: #3b82f6;
  outline: none;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-field input[type="date"]::placeholder,
.form-field input[type="time"]::placeholder,
.form-field input[type="number"]::placeholder {
  color: #9ca3af;
}

/* Available Slots Styling */
.slots-container {
  padding: 1rem;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.slots-container.error {
  background-color: #fef2f2;
  border-color: #fecaca;
  color: #dc2626;
}

.slots-container.empty {
  background-color: #f3f4f6;
  color: #6b7280;
}

.slots-spinner {
  text-align: center;
  color: #6b7280;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.slots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 0.75rem;
  width: 100%;
}

.slot-button {
  padding: 0.75rem;
  border: 2px solid #e5e7eb;
  border-radius: 6px;
  background-color: #ffffff;
  color: #374151;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.slot-button:hover:not(.selected) {
  border-color: #3b82f6;
  background-color: #eff6ff;
  color: #1e40af;
}

.slot-button.selected {
  background-color: #3b82f6;
  color: #ffffff;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.slot-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
