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
        <li :class="{ active: selectedMenu === 'availability' }" @click="selectMenu('availability')">
          Availability
        </li>
        <li :class="{ active: selectedMenu === 'connections' }" @click="selectMenu('connections')">
          My Connections
        </li>
        <li :class="{ active: selectedMenu === 'consultations' }" @click="selectMenu('consultations')">
          My Consultations
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
                <option v-for="category in availableCategories" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
              <div v-if="isDuplicateCategory" class="form-error">
                This category has already been added
              </div>
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
            <button type="submit" class="btn" :disabled="rateSaving || isDuplicateCategory">
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

      <!-- Availability Section -->
      <section v-if="selectedMenu === 'availability'" class="section">
        <div class="section-header">
          <h2>My Availability</h2>
          <button type="button" class="btn" @click="loadAvailability">Refresh</button>
        </div>

        <div class="list-state" v-if="availabilityLoading">Loading availability...</div>
        <div class="list-state error" v-else-if="availabilityError">{{ availabilityError }}</div>

        <div v-else class="availability-section">
          <!-- Current Availability -->
          <div v-if="availability.length > 0" class="availability-list">
            <h3>Current Time Slots</h3>
            <div class="availability-grid">
              <div v-for="slot in sortedAvailability" :key="slot.id" class="availability-slot">
                <div class="slot-day">{{ getDayName(slot.dayOfWeek) }}</div>
                <div class="slot-time">{{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}</div>
                <div class="slot-actions">
                  <button type="button" class="btn btn-sm btn-danger" @click="deleteAvailability(slot.id)" :disabled="deletingAvailabilityId === slot.id">
                    🗑️ Delete
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Add New Availability -->
          <div class="form">
            <h3>{{ editingAvailabilityId ? 'Edit Time Slot' : 'Add New Time Slot' }}</h3>
            <div class="form-grid">
              <div class="form-field">
                <label for="day-of-week">Day of Week *</label>
                <select id="day-of-week" v-model.number="availabilityForm.dayOfWeek" required>
                  <option value="">Select day</option>
                  <option value="0">Monday</option>
                  <option value="1">Tuesday</option>
                  <option value="2">Wednesday</option>
                  <option value="3">Thursday</option>
                  <option value="4">Friday</option>
                  <option value="5">Saturday</option>
                  <option value="6">Sunday</option>
                </select>
              </div>
              <div class="form-field">
                <label for="start-time">Start Time *</label>
                <input id="start-time" v-model="availabilityForm.startTime" type="time" required />
              </div>
              <div class="form-field">
                <label for="end-time">End Time *</label>
                <input id="end-time" v-model="availabilityForm.endTime" type="time" required />
              </div>
            </div>
            <div class="form-actions">
              <button type="button" class="btn" @click="saveAvailability" :disabled="availabilitySaving || !isAvailabilityFormValid">
                {{ availabilitySaving ? 'Saving...' : (editingAvailabilityId ? 'Update Slot' : 'Add Slot') }}
              </button>
              <button v-if="editingAvailabilityId" type="button" class="btn" @click="cancelEditAvailability">Cancel</button>
            </div>
            <div v-if="availabilityMessage" :class="['form-message', availabilitySuccess ? 'success' : 'error']">
              {{ availabilityMessage }}
            </div>
          </div>
        </div>
      </section>

      <!-- Consultations Section -->
      <section v-if="selectedMenu === 'consultations'" class="section">
        <div class="section-header">
          <h2>My Consultations</h2>
          <button type="button" class="btn" @click="loadConsultations">Refresh</button>
        </div>

        <div class="list-state" v-if="consultationsLoading">Loading consultations...</div>
        <div class="list-state error" v-else-if="consultationsError">{{ consultationsError }}</div>
        <div v-else-if="consultations.length === 0" class="empty-state">
          <div class="empty-icon">📋</div>
          <h3>No consultations yet</h3>
          <p>You haven't received any consultation requests yet.</p>
        </div>
        <div v-else class="consultations-container">
          <!-- Consultations Table -->
          <div class="table" v-if="paginatedConsultations.length > 0">
            <div class="table-header consultations-table">
              <span>Client</span>
              <span>Category</span>
              <span>Date & Time</span>
              <span>Duration (minutes)</span>
              <span>Status</span>
              <span>Price</span>
              <span>Actions</span>
            </div>
            <div v-for="consultation in paginatedConsultations" :key="consultation.id" class="table-row consultations-table">
              <span>{{ consultation.clientName || consultation.userId }}</span>
              <span>{{ consultation.categoryName || consultation.categoryId }}</span>
              <span>{{ formatDateTime(consultation.scheduledAt) }}</span>
              <span>{{ consultation.duration }} min</span>
              <span :class="['status-badge', consultation.status.toLowerCase()]">
                {{ consultation.status }}
              </span>
              <span>{{ consultation.price === 0 ? 'Free' : `$${consultation.price}` }}</span>
              <span class="actions-cell">
                <template v-if="isConsultationActionable(consultation)">
                  <template v-if="consultation.status === 'Requested'">
                    <button 
                      type="button" 
                      class="btn btn-sm btn-success" 
                      @click="approveConsultation(consultation.id)"
                      :disabled="updatingConsultationId === consultation.id"
                    >
                      {{ updatingConsultationId === consultation.id ? '...' : '✓ Approve' }}
                    </button>
                    <button 
                      type="button" 
                      class="btn btn-sm btn-danger" 
                      @click="declineConsultation(consultation.id)"
                      :disabled="updatingConsultationId === consultation.id"
                    >
                      {{ updatingConsultationId === consultation.id ? '...' : '✗ Decline' }}
                    </button>
                  </template>
                  <template v-else-if="consultation.status === 'Scheduled'">
                    <template v-if="isConsultationInFuture(consultation)">
                      <button 
                        type="button" 
                        class="btn btn-sm btn-danger" 
                        @click="declineConsultation(consultation.id)"
                        :disabled="updatingConsultationId === consultation.id"
                      >
                        {{ updatingConsultationId === consultation.id ? '...' : '✗ Cancel' }}
                      </button>
                    </template>
                    <template v-else>
                      <button 
                        type="button" 
                        class="btn btn-sm btn-warning" 
                        @click="markAsMissed(consultation.id)"
                        :disabled="updatingConsultationId === consultation.id"
                      >
                        {{ updatingConsultationId === consultation.id ? '...' : '⏭ Mark Missed' }}
                      </button>
                    </template>
                  </template>
                  <template v-else>
                    <span class="text-gray">-</span>
                  </template>
                </template>
                <template v-else>
                  <span class="text-gray">Expired</span>
                </template>
              </span>
            </div>
          </div>

          <!-- Pagination -->
          <div v-if="consultationPagination.totalPages > 1" class="pagination">
            <button 
              class="pagination-btn" 
              :disabled="consultationPagination.currentPage === 1"
              @click="goToConsultationPage(consultationPagination.currentPage - 1)"
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
              @click="goToConsultationPage(consultationPagination.currentPage + 1)"
            >
              Next
            </button>
          </div>
        </div>

        <!-- Approve Consultation Dialog -->
        <div v-if="showApprovalDialog" class="modal-overlay" @click="showApprovalDialog = false">
          <div class="modal" @click.stop>
            <h3>Approve Consultation</h3>
            <p>Estimate the duration for this consultation (in minutes):</p>
            <div class="form-field">
              <label>Duration (minutes) *</label>
              <input 
                v-model.number="approvingConsultationDuration" 
                type="number" 
                min="15" 
                step="15" 
                placeholder="60"
                @keyup.enter="confirmApprove"
              />
            </div>
            <div v-if="approvingConsultationDurationError" class="error-message">
              {{ approvingConsultationDurationError }}
            </div>
            <div class="modal-actions">
              <button type="button" class="btn success" @click="confirmApprove" :disabled="updatingConsultationId !== null">
                {{ updatingConsultationId !== null ? 'Approving...' : 'Approve' }}
              </button>
              <button type="button" class="btn" @click="showApprovalDialog = false" :disabled="updatingConsultationId !== null">Cancel</button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
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
const editingConnectionId = ref<string | null>(null)
const connectionSaving = ref(false)
const connectionMessage = ref('')
const connectionSuccess = ref(false)

// Consultations state
const consultationsLoading = ref(false)
const consultationsError = ref('')
const consultations = ref<any[]>([])
const currentConsultationPage = ref(1)
const itemsPerPage = 10
const updatingConsultationId = ref<string | null>(null)

// Approval dialog state
const showApprovalDialog = ref(false)
const approvingConsultationId = ref<string | null>(null)
const approvingConsultationDuration = ref<number | null>(null)
const approvingConsultationDurationError = ref('')

const consultationPagination = computed(() => {
  const total = consultations.value.length
  const totalPages = Math.ceil(total / itemsPerPage)
  return {
    currentPage: currentConsultationPage.value,
    totalPages: totalPages || 1,
    totalCount: total
  }
})

const paginatedConsultations = computed(() => {
  const start = (currentConsultationPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return consultations.value.slice(start, end)
})

const selectMenu = (menu: string) => {
  selectedMenu.value = menu
  if (menu === 'profile') loadProfile()
  if (menu === 'rates') loadRates()
  if (menu === 'connections') loadConnections()
  if (menu === 'consultations') loadConsultations()
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
    if (!userId) {
      profileUpdateMessage.value = 'User ID not found'
      profileUpdateSuccess.value = false
      return
    }
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
    if (!userId) {
      alert('User ID not found')
      return
    }
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
    if (!userId) {
      ratesError.value = 'User ID not found'
      return
    }
    const specialist = await $fetch(`${config.public.apiBase}/specialists/${userId}`)
    rates.value = specialist.categoryRates || []
    // Load categories
    const categoriesData = await $fetch(`${config.public.apiBase}/categories?page=1&pageSize=100`)
    categories.value = Array.isArray(categoriesData) ? categoriesData : []
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
    if (!userId) {
      rateMessage.value = 'User ID not found'
      rateSuccess.value = false
      return
    }
    const specialist = await $fetch(`${config.public.apiBase}/specialists/${userId}`)
    
    let updatedRates = [...(specialist.categoryRates || [])]
    
    if (editingRateId.value) {
      // Update existing rate
      const index = updatedRates.findIndex(r => r.categoryId === editingRateId.value)
      if (index !== -1) {
        updatedRates[index] = { 
          ...rateForm.value,
          rating: updatedRates[index].rating || null,
          totalConsultations: updatedRates[index].totalConsultations || 0
        }
      }
    } else {
      // Add new rate
      updatedRates.push({ 
        ...rateForm.value,
        rating: null,
        totalConsultations: 0
      })
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
    if (!userId) {
      alert('User ID not found')
      return
    }
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

// Filter out categories that are already added
const availableCategories = computed(() => {
  const addedCategoryIds = rates.value.map(r => r.categoryId)
  return categories.value.filter(c => !addedCategoryIds.includes(c.id))
})

// Check if current form selection is a duplicate
const isDuplicateCategory = computed(() => {
  if (!rateForm.value.categoryId || editingRateId.value) return false
  return rates.value.some(r => r.categoryId === rateForm.value.categoryId)
})

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

// Availability state
const availabilityLoading = ref(false)
const availabilityError = ref('')
const availability = ref<any[]>([])
const availabilityForm = ref({
  dayOfWeek: '',
  startTime: '',
  endTime: ''
})
const editingAvailabilityId = ref<string | null>(null)
const availabilitySaving = ref(false)
const availabilityMessage = ref('')
const availabilitySuccess = ref(false)
const deletingAvailabilityId = ref<string | null>(null)

const sortedAvailability = computed(() => {
  return [...availability.value].sort((a, b) => a.dayOfWeek - b.dayOfWeek)
})

const isAvailabilityFormValid = computed(() => {
  return availabilityForm.value.dayOfWeek !== '' && 
         availabilityForm.value.startTime && 
         availabilityForm.value.endTime &&
         availabilityForm.value.startTime < availabilityForm.value.endTime
})

const getDayName = (dayOfWeek: number): string => {
  const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']
  return days[dayOfWeek] || 'Unknown'
}

const formatTime = (timeString: string): string => {
  // Assuming timeString is in HH:mm format
  return timeString || 'N/A'
}

// Availability operations
const loadAvailability = async () => {
  availabilityLoading.value = true
  availabilityError.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      availabilityError.value = 'User ID not found'
      return
    }
    const data = await $fetch(`${config.public.apiBase}/specialists/${userId}/availability`)
    availability.value = data || []
  } catch (error: any) {
    availabilityError.value = error.data?.message || error.message || 'Failed to load availability'
  } finally {
    availabilityLoading.value = false
  }
}

const saveAvailability = async () => {
  availabilitySaving.value = true
  availabilityMessage.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      availabilityMessage.value = 'User ID not found'
      availabilitySuccess.value = false
      return
    }
    
    if (editingAvailabilityId.value) {
      // Update existing availability
      await $fetch(`${config.public.apiBase}/specialists/${userId}/availability/${editingAvailabilityId.value}`, {
        method: 'PUT',
        body: {
          dayOfWeek: parseInt(availabilityForm.value.dayOfWeek),
          startTime: availabilityForm.value.startTime,
          endTime: availabilityForm.value.endTime
        }
      })
      availabilityMessage.value = 'Availability updated successfully'
    } else {
      // Add new availability
      await $fetch(`${config.public.apiBase}/specialists/${userId}/availability`, {
        method: 'POST',
        body: {
          dayOfWeek: parseInt(availabilityForm.value.dayOfWeek),
          startTime: availabilityForm.value.startTime,
          endTime: availabilityForm.value.endTime
        }
      })
      availabilityMessage.value = 'Availability added successfully'
    }
    
    availabilitySuccess.value = true
    setTimeout(() => {
      cancelEditAvailability()
      loadAvailability()
    }, 1500)
  } catch (error: any) {
    availabilityMessage.value = error.data?.message || error.message || 'Failed to save availability'
    availabilitySuccess.value = false
  } finally {
    availabilitySaving.value = false
  }
}

const cancelEditAvailability = () => {
  availabilityForm.value = { dayOfWeek: '', startTime: '', endTime: '' }
  editingAvailabilityId.value = null
  availabilityMessage.value = ''
}

const deleteAvailability = async (availabilityId: string) => {
  if (!confirm('Are you sure you want to delete this time slot?')) return
  
  deletingAvailabilityId.value = availabilityId
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      alert('User ID not found')
      return
    }
    await $fetch(`${config.public.apiBase}/specialists/${userId}/availability/${availabilityId}`, {
      method: 'DELETE'
    })
    await loadAvailability()
  } catch (error: any) {
    alert(error.data?.message || error.message || 'Failed to delete availability')
  } finally {
    deletingAvailabilityId.value = null
  }
}

// Consultations operations
const loadConsultations = async () => {
  consultationsLoading.value = true
  consultationsError.value = ''
  currentConsultationPage.value = 1
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      consultationsError.value = 'User ID not found'
      return
    }
    const data = await $fetch(`${config.public.apiBase}/consultations/specialist/${userId}`)
    const consultationsData = data || []
    
    console.log('Raw consultations data:', consultationsData)
    
    // Enrich consultations with client names and category names
    const enrichedConsultations = await Promise.all(
      consultationsData.map(async (consultation: any) => {
        try {
          console.log('Processing consultation:', consultation)
          
          // Fetch client name
          let clientName = 'Unknown Client'
          if (consultation.userId) {
            try {
              console.log('Fetching client:', consultation.userId)
              const clientData = await $fetch(`${config.public.apiBase}/users/${consultation.userId}`)
              console.log('Client data:', clientData)
              clientName = clientData?.name || consultation.userId
            } catch (e: any) {
              console.error('Failed to fetch client:', consultation.userId, e)
              clientName = `Client (${consultation.userId})`
            }
          } else {
            console.warn('No userId in consultation:', consultation)
          }
          
          // Fetch category name
          let categoryName = 'Unknown Category'
          if (consultation.categoryId) {
            try {
              console.log('Fetching category:', consultation.categoryId)
              const categoryData = await $fetch(`${config.public.apiBase}/categories/${consultation.categoryId}`)
              console.log('Category data:', categoryData)
              categoryName = categoryData?.name || consultation.categoryId
            } catch (e: any) {
              console.error('Failed to fetch category:', consultation.categoryId, e)
              categoryName = `Category (${consultation.categoryId})`
            }
          } else {
            console.warn('No categoryId in consultation:', consultation)
          }
          
          const enriched = {
            ...consultation,
            clientName,
            categoryName
          }
          console.log('Enriched consultation:', enriched)
          return enriched
        } catch (e) {
          console.error('Error enriching consultation:', e)
          return consultation
        }
      })
    )
    
    consultations.value = enrichedConsultations
    console.log('Final consultations:', enrichedConsultations)
  } catch (error: any) {
    console.error('Error loading consultations:', error)
    consultationsError.value = error.data?.message || error.message || 'Failed to load consultations'
  } finally {
    consultationsLoading.value = false
  }
}

const goToConsultationPage = (page: number) => {
  const totalPages = consultationPagination.value.totalPages
  if (page >= 1 && page <= totalPages) {
    currentConsultationPage.value = page
  }
}

const approveConsultation = async (consultationId: string) => {
  // Open dialog to set duration
  console.log('Opening approval dialog for consultation:', consultationId)
  approvingConsultationId.value = consultationId
  approvingConsultationDuration.value = null
  approvingConsultationDurationError.value = ''
  showApprovalDialog.value = true
  console.log('Approval dialog opened, showApprovalDialog:', showApprovalDialog.value)
}

const confirmApprove = async () => {
  if (!approvingConsultationDuration.value || approvingConsultationDuration.value < 15) {
    approvingConsultationDurationError.value = 'Duration must be at least 15 minutes'
    return
  }

  updatingConsultationId.value = approvingConsultationId.value
  try {
    const result = await $fetch(`${config.public.apiBase}/consultations/${approvingConsultationId.value}/approve`, {
      method: 'PUT',
      body: { 
        status: 'Scheduled',
        duration: approvingConsultationDuration.value
      }
    })
    
    console.log('Consultation approved:', result)
    // Update the consultation in the list
    const consultation = consultations.value.find(c => c.id === approvingConsultationId.value)
    if (consultation) {
      consultation.status = 'Scheduled'
      consultation.duration = approvingConsultationDuration.value
    }
    showApprovalDialog.value = false
  } catch (error: any) {
    console.error('Error approving consultation:', error)
    approvingConsultationDurationError.value = error.data?.message || error.message || 'Failed to approve consultation'
  } finally {
    updatingConsultationId.value = null
  }
}

const declineConsultation = async (consultationId: string) => {
  updatingConsultationId.value = consultationId
  try {
    const result = await $fetch(`${config.public.apiBase}/consultations/${consultationId}/status`, {
      method: 'PUT',
      body: { status: 'Cancelled' }
    })
    
    console.log('Consultation declined:', result)
    // Update the consultation in the list
    const consultation = consultations.value.find(c => c.id === consultationId)
    if (consultation) {
      consultation.status = 'Cancelled'
    }
  } catch (error: any) {
    console.error('Error declining consultation:', error)
    alert(error.data?.message || error.message || 'Failed to decline consultation')
  } finally {
    updatingConsultationId.value = null
  }
}

const markAsMissed = async (consultationId: string) => {
  updatingConsultationId.value = consultationId
  try {
    const result = await $fetch(`${config.public.apiBase}/consultations/${consultationId}/status`, {
      method: 'PUT',
      body: { status: 'Missed' }
    })
    
    console.log('Consultation marked as missed:', result)
    // Update the consultation in the list
    const consultation = consultations.value.find(c => c.id === consultationId)
    if (consultation) {
      consultation.status = 'Missed'
    }
  } catch (error: any) {
    console.error('Error marking consultation as missed:', error)
    alert(error.data?.message || error.message || 'Failed to mark consultation as missed')
  } finally {
    updatingConsultationId.value = null
  }
}

const isConsultationActionable = (consultation: any): boolean => {
  // Check if the consultation date/time is still in the future
  if (!consultation.scheduledAt) return false
  
  try {
    const scheduledDate = new Date(consultation.scheduledAt)
    const now = new Date()
    
    // Consultation is actionable if it's still in the future
    return scheduledDate > now
  } catch (e) {
    console.error('Error checking consultation date:', e)
    return false
  }
}

const isConsultationInFuture = (consultation: any): boolean => {
  // Check if the consultation date/time is still in the future
  if (!consultation.scheduledAt) return false
  
  try {
    const scheduledDate = new Date(consultation.scheduledAt)
    const now = new Date()
    
    return scheduledDate > now
  } catch (e) {
    console.error('Error checking consultation date:', e)
    return false
  }
}

const formatDateTime = (dateString: string) => {
  if (!dateString) return 'N/A'
  try {
    // Handle ISO 8601 format with timezone
    const date = new Date(dateString)
    if (isNaN(date.getTime())) return 'Invalid Date'
    
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    })
  } catch (e) {
    console.error('Error formatting date:', dateString, e)
    return dateString || 'N/A'
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
    localStorage.removeItem('specialist_session')
    router.push('/login')
  }
}

onMounted(() => {
  // Check if user is properly logged in
  const userId = sessionStorage.getItem('userId')
  const role = sessionStorage.getItem('role')?.toLowerCase()
  
  if (!userId || role !== 'specialist') {
    // Not logged in or not a specialist, redirect to login
    localStorage.removeItem('specialist_session')
    router.push('/login')
    return
  }
  
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

/* Loading Spinner */
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

/* Empty State */
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

/* Connections List */
.connections-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.connection-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
  border-left: 4px solid #667eea;
}

.connection-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
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
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: #dc2626;
  font-weight: 500;
}

.connection-status.verified {
  color: #059669;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #dc2626;
}

.connection-status.verified .status-dot {
  background: #059669;
}

.connection-value {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 1rem;
  word-break: break-word;
}

.connection-actions {
  display: flex;
  gap: 0.5rem;
}

/* Modal */
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

/* Buttons */
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

.btn-icon {
  font-size: 1.125rem;
  margin-right: 0.25rem;
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

/* Form Fields */
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

.form-error {
  color: #dc2626;
  font-size: 0.875rem;
  margin-top: 0.25rem;
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

/* Rates Table (keep for rates section) */
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

.consultations-table {
  grid-template-columns: 1.5fr 1.5fr 2fr 1fr 1fr 1fr 1.5fr;
}

.status-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  white-space: nowrap;
}

.status-badge.requested {
  background: #dbeafe;
  color: #1e40af;
}

.status-badge.confirmed {
  background: #dcfce7;
  color: #15803d;
}

.status-badge.scheduled {
  background: #dcfce7;
  color: #15803d;
}

.status-badge.inprogress {
  background: #fef3c7;
  color: #92400e;
}

.status-badge.completed {
  background: #d1fae5;
  color: #065f46;
}

.status-badge.missed {
  background: #fed7aa;
  color: #92400e;
}

.status-badge.cancelled {
  background: #fee2e2;
  color: #991b1b;
}

.status-badge.cancelled {
  background: #fee2e2;
  color: #991b1b;
}

.consultations-container {
  background: white;
  padding: 1rem;
  border-radius: 8px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 1rem;
}

.pagination-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  color: #1f2937;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  color: #6b7280;
  font-size: 0.875rem;
  font-weight: 500;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: #6b7280;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  color: #1f2937;
  margin: 1rem 0 0.5rem;
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

.form-actions {
  display: flex;
  gap: 1rem;
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

.actions-cell {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.75rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 600;
  white-space: nowrap;
}

.btn-sm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-success {
  background: #10b981;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #059669;
  transform: translateY(-1px);
}

.btn-danger {
  background: #ef4444;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #dc2626;
  transform: translateY(-1px);
}

.btn-warning {
  background: #f59e0b;
  color: white;
}

.btn-warning:hover:not(:disabled) {
  background: #d97706;
  transform: translateY(-1px);
}

.text-gray {
  color: #9ca3af;
  font-size: 0.875rem;
}

/* Availability Section Styles */
.availability-section {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.availability-list {
  margin-bottom: 2rem;
}

.availability-list h3 {
  margin-top: 0;
  margin-bottom: 1rem;
  color: #1f2937;
}

.availability-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1rem;
}

.availability-slot {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.slot-day {
  font-weight: 600;
  color: #1f2937;
  font-size: 0.95rem;
}

.slot-time {
  color: #6b7280;
  font-size: 0.875rem;
}

.slot-actions {
  margin-top: 0.5rem;
}

.btn-warning {
  background: #f59e0b;
  color: white;
}

.btn-warning:hover:not(:disabled) {
  background: #d97706;
  transform: translateY(-1px);
}

.text-gray {
  color: #9ca3af;
  font-size: 0.875rem;
}
</style>
