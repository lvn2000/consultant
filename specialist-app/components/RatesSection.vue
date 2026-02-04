<template>
  <section class="section">
    <div class="section-header">
      <div class="header-content">
        <h2><span class="icon">💰</span>My Rates</h2>
        <p class="header-subtitle">Set your hourly rates for different expertise categories</p>
      </div>
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
            <option v-for="category in (editingRateId ? categories : availableCategories)" :key="category.id" :value="category.id">
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'

const config = useRuntimeConfig()

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
  editingRateId.value = rate.categoryId
  rateForm.value = {
    categoryId: rate.categoryId,
    hourlyRate: rate.hourlyRate,
    experienceYears: rate.experienceYears
  }
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

onMounted(() => {
  loadRates()
})

defineExpose({
  loadRates
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

.btn.danger {
  background: #dc2626;
}

.btn.danger:hover:not(:disabled) {
  background: #b91c1c;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

.form-error {
  color: #dc2626;
  font-size: 0.875rem;
  margin-top: 0.25rem;
}

.form-actions {
  display: flex;
  gap: 1rem;
}

.form-message {
  padding: 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
  margin-top: 1rem;
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
