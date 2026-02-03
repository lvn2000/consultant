<template>
  <div class="tab-panel">
    <div class="form" style="margin-bottom: 2rem;">
      <h4>Filter Consultations</h4>
      <div class="form-grid">
        <div class="form-field">
          <label>Status</label>
          <select v-model="filters.status">
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
          <input type="date" v-model="filters.fromDate">
        </div>
        <div class="form-field">
          <label>To Date</label>
          <input type="date" v-model="filters.toDate">
        </div>
        <div class="form-field">
          <label>Search</label>
          <input type="text" v-model="filters.search" placeholder="Search by specialist or description">
        </div>
      </div>
      <div class="form-actions">
        <button class="btn btn-secondary" @click="clearFilters">Clear Filters</button>
        <button class="btn btn-primary" @click="emit('load-consultations')">Apply Filters</button>
      </div>
    </div>

    <div v-if="loading" class="list-state">Loading consultations...</div>
    <div v-else-if="error" class="list-state error">{{ error }}</div>
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
      <div v-if="pagination.totalPages > 1" class="pagination">
        <button 
          class="pagination-btn" 
          :disabled="pagination.currentPage === 1"
          @click="emit('go-to-page', pagination.currentPage - 1)"
        >
          Previous
        </button>
        <span class="pagination-info">
          Page {{ pagination.currentPage }} of {{ pagination.totalPages }}
          ({{ pagination.totalCount }} total)
        </span>
        <button 
          class="pagination-btn" 
          :disabled="pagination.currentPage === pagination.totalPages"
          @click="emit('go-to-page', pagination.currentPage + 1)"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  loading: boolean
  error: string
  consultations: any[]
  filters: {
    status: string
    fromDate: string
    toDate: string
    search: string
  }
  pagination: {
    currentPage: number
    pageSize: number
    totalCount: number
    totalPages: number
  }
}

const props = defineProps<Props>()
const emit = defineEmits(['load-consultations', 'go-to-page', 'clear-filters'])

const filters = ref(props.filters)

const clearFilters = () => {
  filters.value = {
    status: '',
    fromDate: '',
    toDate: '',
    search: ''
  }
  emit('clear-filters')
}

const filteredConsultations = computed(() => {
  let filtered = props.consultations

  // Filter by status
  if (filters.value.status) {
    filtered = filtered.filter(c => c.status === filters.value.status)
  }

  // Filter by date range
  if (filters.value.fromDate) {
    const fromDate = new Date(filters.value.fromDate)
    filtered = filtered.filter(c => new Date(c.createdAt || c.date) >= fromDate)
  }

  if (filters.value.toDate) {
    const toDate = new Date(filters.value.toDate)
    toDate.setHours(23, 59, 59, 999)
    filtered = filtered.filter(c => new Date(c.createdAt || c.date) <= toDate)
  }

  // Filter by search text
  if (filters.value.search) {
    const search = filters.value.search.toLowerCase()
    filtered = filtered.filter(c => 
      c.description?.toLowerCase().includes(search) ||
      c.specialistId?.toLowerCase().includes(search)
    )
  }

  // Update pagination info based on filtered results
  const totalFiltered = filtered.length
  const totalPages = Math.ceil(totalFiltered / props.pagination.pageSize)

  // Apply pagination
  const startIndex = (props.pagination.currentPage - 1) * props.pagination.pageSize
  const endIndex = startIndex + props.pagination.pageSize

  return filtered.slice(startIndex, endIndex)
})

defineExpose({
  filters
})
</script>

<style scoped>
.tab-panel {
  padding: 1rem;
}

.form {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1.5rem;
}

.form h4 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  color: #1f2937;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
}

.form-field label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
  font-size: 0.9rem;
}

.form-field input,
.form-field select {
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.95rem;
  font-family: inherit;
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

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
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

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

.consultation-list {
  background: white;
  border-radius: 8px;
}

.consultation-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1rem;
}

.consultation-item:last-child {
  margin-bottom: 0;
}

.consultation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.consultation-title {
  font-weight: 500;
  color: #1f2937;
  flex: 1;
}

.consultation-status {
  padding: 0.375rem 0.75rem;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 500;
  text-transform: uppercase;
  color: white;
}

.consultation-status.requested {
  background: #fbbf24;
}

.consultation-status.scheduled {
  background: #60a5fa;
}

.consultation-status.inprogress {
  background: #34d399;
}

.consultation-status.completed {
  background: #10b981;
}

.consultation-status.missed {
  background: #ef4444;
}

.consultation-status.cancelled {
  background: #6b7280;
}

.consultation-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  color: #666;
  font-size: 0.9rem;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.pagination-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.15s;
}

.pagination-btn:hover:not(:disabled) {
  border-color: #4f46e5;
  color: #4f46e5;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  color: #666;
  font-size: 0.9rem;
}
</style>
