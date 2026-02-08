<template>
  <section class="section">
    <div class="section-header">
      <div class="header-content">
        <h2><span class="icon">📞</span>My Consultations</h2>
        <p class="header-subtitle">Track and manage your consultation sessions and client interactions</p>
      </div>
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
          <span>{{ formatDateTime(consultation.scheduledAt || '') }}</span>
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { useApi } from '~/composables/useApi'

const config = useRuntimeConfig()
const { $fetch } = useApi()

type Consultation = {
  id: string
  userId?: string
  categoryId?: string
  scheduledAt?: string
  duration: number
  status: string
  price: number
}

type EnrichedConsultation = Consultation & {
  clientName?: string
  categoryName?: string
}

type NamedResource = {
  name?: string
}

const consultationsLoading = ref(false)
const consultationsError = ref('')
const consultations = ref<EnrichedConsultation[]>([])
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
    const data = await $fetch<Consultation[]>(`${config.public.apiBase}/consultations/specialist/${userId}`)
    const consultationsData = data || []
    
    // Enrich consultations with client names and category names
    const enrichedConsultations = await Promise.all(
      consultationsData.map(async (consultation: any) => {
        try {
          // Fetch client name
          let clientName = 'Unknown Client'
          if (consultation.userId) {
            try {
              const clientData = await $fetch<NamedResource>(`${config.public.apiBase}/users/${consultation.userId}`)
              clientName = clientData?.name || consultation.userId
            } catch (e: any) {
              clientName = `Client (${consultation.userId})`
            }
          }
          
          // Fetch category name
          let categoryName = 'Unknown Category'
          if (consultation.categoryId) {
            try {
              const categoryData = await $fetch<NamedResource>(`${config.public.apiBase}/categories/${consultation.categoryId}`)
              categoryName = categoryData?.name || consultation.categoryId
            } catch (e: any) {
              categoryName = `Category (${consultation.categoryId})`
            }
          }
          
          return {
            ...consultation,
            clientName,
            categoryName
          }
        } catch (e) {
          return consultation
        }
      })
    )
    
    consultations.value = enrichedConsultations
  } catch (error: any) {
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
  approvingConsultationId.value = consultationId
  approvingConsultationDuration.value = null
  approvingConsultationDurationError.value = ''
  showApprovalDialog.value = true
}

const confirmApprove = async () => {
  if (!approvingConsultationDuration.value || approvingConsultationDuration.value < 15) {
    approvingConsultationDurationError.value = 'Duration must be at least 15 minutes'
    return
  }

  updatingConsultationId.value = approvingConsultationId.value
  try {
    await $fetch(`${config.public.apiBase}/consultations/${approvingConsultationId.value}/approve`, {
      method: 'PUT',
      body: { 
        status: 'Scheduled',
        duration: approvingConsultationDuration.value
      }
    })
    
    // Update the consultation in the list
    const consultation = consultations.value.find(c => c.id === approvingConsultationId.value)
    if (consultation) {
      consultation.status = 'Scheduled'
      consultation.duration = approvingConsultationDuration.value
    }
    showApprovalDialog.value = false
  } catch (error: any) {
    approvingConsultationDurationError.value = error.data?.message || error.message || 'Failed to approve consultation'
  } finally {
    updatingConsultationId.value = null
  }
}

const declineConsultation = async (consultationId: string) => {
  updatingConsultationId.value = consultationId
  try {
    await $fetch(`${config.public.apiBase}/consultations/${consultationId}/status`, {
      method: 'PUT',
      body: { status: 'Cancelled' }
    })
    
    // Update the consultation in the list
    const consultation = consultations.value.find(c => c.id === consultationId)
    if (consultation) {
      consultation.status = 'Cancelled'
    }
  } catch (error: any) {
    alert(error.data?.message || error.message || 'Failed to decline consultation')
  } finally {
    updatingConsultationId.value = null
  }
}

const markAsMissed = async (consultationId: string) => {
  updatingConsultationId.value = consultationId
  try {
    await $fetch(`${config.public.apiBase}/consultations/${consultationId}/status`, {
      method: 'PUT',
      body: { status: 'Missed' }
    })
    
    // Update the consultation in the list
    const consultation = consultations.value.find(c => c.id === consultationId)
    if (consultation) {
      consultation.status = 'Missed'
    }
  } catch (error: any) {
    alert(error.data?.message || error.message || 'Failed to mark consultation as missed')
  } finally {
    updatingConsultationId.value = null
  }
}

const isConsultationActionable = (consultation: EnrichedConsultation): boolean => {
  if (!consultation.scheduledAt) return false
  
  try {
    const scheduledDate = new Date(consultation.scheduledAt)
    const now = new Date()
    return scheduledDate > now
  } catch (e) {
    return false
  }
}

const isConsultationInFuture = (consultation: EnrichedConsultation): boolean => {
  if (!consultation.scheduledAt) return false
  
  try {
    const scheduledDate = new Date(consultation.scheduledAt)
    const now = new Date()
    return scheduledDate > now
  } catch (e) {
    return false
  }
}

const formatDateTime = (dateString: string) => {
  if (!dateString) return 'N/A'
  try {
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
  } catch (e: any) {
    return 'N/A'
  }
}

onMounted(() => {
  loadConsultations()
})

defineExpose({
  loadConsultations
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
}

.consultations-container {
  background: white;
  padding: 1rem;
  border-radius: 8px;
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

.consultations-table {
  grid-template-columns: 1.5fr 1.5fr 2fr 1fr 1fr 1fr 1.5fr;
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

.actions-cell {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
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
}

.btn:hover:not(:disabled) {
  background: #4338ca;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

.form-field input {
  padding: 0.625rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.form-field input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.error-message {
  color: #dc2626;
  font-size: 0.875rem;
  margin: 0.5rem 0 1rem 0;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 1.5rem;
}

.btn.success {
  background: #10b981;
}

.btn.success:hover:not(:disabled) {
  background: #059669;
}
</style>
