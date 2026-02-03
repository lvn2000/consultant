<template>
  <section class="section">
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'

const config = useRuntimeConfig()

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
  return timeString || 'N/A'
}

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

onMounted(() => {
  loadAvailability()
})

defineExpose({
  loadAvailability
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
}

.section-header h2 {
  color: #1f2937;
  margin: 0;
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

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.75rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 600;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
}

.btn-sm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-danger {
  background: #ef4444;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #dc2626;
  transform: translateY(-1px);
}

.form {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  border-top: 1px solid #e5e7eb;
  margin-top: 1rem;
}

.form h3 {
  margin-top: 0;
  margin-bottom: 1rem;
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

.form-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
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

.form-message {
  padding: 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
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
