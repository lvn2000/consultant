<template>
  <section class="section">
    <div class="section-header">
      <div class="header-content">
        <h2><span class="icon">💰</span>{{ $t('rates.title') }}</h2>
        <p class="header-subtitle">{{ $t('rates.subtitle') }}</p>
      </div>
      <button type="button" class="btn" @click="loadRates">Refresh</button>
    </div>

    <div class="list-state" v-if="ratesLoading">{{ $t('rates.loading') }}</div>
    <div class="list-state error" v-else-if="ratesError">{{ ratesError }}</div>

    <div class="table" v-else-if="paginatedRates.length > 0">
      <div class="table-header rates-table">
        <span>{{ $t('rates.category') }}</span>
        <span>{{ $t('rates.hourlyRate') }}</span>
        <span>{{ $t('rates.experience') }}</span>
        <span>{{ $t('common.actions') }}</span>
      </div>
      <div v-for="rate in paginatedRates" :key="rate.categoryId" class="table-row rates-table">
        <span>{{ getCategoryName(rate.categoryId) }}</span>
        <span>${{ rate.hourlyRate }}</span>
        <span>{{ rate.experienceYears }}</span>
        <span class="row-actions">
          <button type="button" class="btn" @click="startEditRate(rate)">{{ $t('common.edit') }}</button>
          <button type="button" class="btn danger" @click="removeRate(rate.categoryId)">{{ $t('common.delete') }}</button>
        </span>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="ratePagination.totalPages > 1" class="pagination">
      <button 
        class="pagination-btn" 
        :disabled="ratePagination.currentPage === 1"
        @click="goToRatePage(ratePagination.currentPage - 1)"
      >
        Previous
      </button>
      <span class="pagination-info">
        Page {{ ratePagination.currentPage }} of {{ ratePagination.totalPages }}
        ({{ ratePagination.totalCount }} total)
      </span>
      <button 
        class="pagination-btn" 
        :disabled="ratePagination.currentPage === ratePagination.totalPages"
        @click="goToRatePage(ratePagination.currentPage + 1)"
      >
        Next
      </button>
    </div>
    <div v-else class="list-state">{{ $t('rates.noRates') }}</div>

    <!-- Rate Form -->
    <form ref="rateFormRef" class="form" @submit.prevent="saveRate">
      <h3>{{ editingRateId ? $t('rates.updateRate') : $t('rates.addCategoryRate') }}</h3>
      <div class="form-grid">
        <div class="form-field">
          <label for="rate-category">{{ $t('rates.category') }}</label>
          <select id="rate-category" v-model="rateForm.categoryId" required :disabled="!!editingRateId">
            <option value="">{{ $t('rates.selectCategory') }}</option>
            <option v-for="category in (editingRateId ? categories : availableCategories)" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
          <div v-if="isDuplicateCategory" class="form-error">
            {{ $t('rates.duplicateCategory') }}
          </div>
        </div>
        <div class="form-field">
          <label for="rate-hourly">{{ $t('rates.hourlyRateDollar') }}</label>
          <input id="rate-hourly" v-model.number="rateForm.hourlyRate" type="number" min="1" step="0.01" required />
        </div>
        <div class="form-field">
          <label for="rate-experience">{{ $t('rates.experience') }}</label>
          <input id="rate-experience" v-model.number="rateForm.experienceYears" type="number" min="0" step="1" required />
        </div>
      </div>
      <div class="form-actions">
        <button type="submit" class="btn" :disabled="rateSaving || isDuplicateCategory">
          {{ rateSaving ? $t('common.saving') : (editingRateId ? $t('rates.updateRate') : $t('rates.addRateButton')) }}
        </button>
        <button v-if="editingRateId" type="button" class="btn" @click="cancelEditRate">{{ $t('common.cancel') }}</button>
      </div>
      <div v-if="rateMessage" :class="['form-message', rateSuccess ? 'success' : 'error']">
        {{ rateMessage }}
      </div>
    </form>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { useApi } from '~/composables/useApi'

const config = useRuntimeConfig()
const { $fetch } = useApi()
const { t } = useI18n()

type Category = {
  id: string
  name: string
}

type CategoryRate = {
  categoryId: string
  hourlyRate: number
  experienceYears: number
  rating?: number | null
  totalConsultations?: number | null
}

type SpecialistProfile = {
  categoryRates?: CategoryRate[]
}

const ratesLoading = ref(false)
const ratesError = ref('')
const rates = ref<CategoryRate[]>([])
const currentRatePage = ref(1)
const itemsPerPage = 5
const categories = ref<Category[]>([])
const rateFormRef = ref<HTMLFormElement | null>(null)
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
  currentRatePage.value = 1
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      ratesError.value = t('auth.userIdNotFound')
      return
    }
    const specialist = await $fetch<SpecialistProfile>(`${config.public.apiBase}/specialists/${userId}`)
    rates.value = specialist.categoryRates || []
    // Load categories
    const categoriesData = await $fetch<Category[]>(`${config.public.apiBase}/categories?page=1&pageSize=100`)
    categories.value = Array.isArray(categoriesData) ? categoriesData : []
  } catch (error: any) {
    ratesError.value = error.message || t('rates.failedToLoad')
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
  // Scroll to form and focus
  nextTick(() => {
    if (rateFormRef.value) {
      rateFormRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
      const firstInput = rateFormRef.value.querySelector('input, select') as HTMLInputElement | HTMLSelectElement
      if (firstInput) {
        setTimeout(() => firstInput.focus(), 300)
      }
    }
  })
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
      rateMessage.value = t('auth.userIdNotFound')
      rateSuccess.value = false
      return
    }
    const specialist = await $fetch<SpecialistProfile>(`${config.public.apiBase}/specialists/${userId}`)
    
    let updatedRates = [...(specialist.categoryRates || [])]
    
    if (editingRateId.value) {
      // Update existing rate
      const index = updatedRates.findIndex((r: CategoryRate) => r.categoryId === editingRateId.value)
      if (index !== -1) {
        updatedRates[index] = { 
          ...rateForm.value,
          rating: updatedRates[index]!.rating || null,
          totalConsultations: updatedRates[index]!.totalConsultations || 0
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
    
    rateMessage.value = editingRateId.value ? t('rates.rateUpdated') : t('rates.rateAdded')
    rateSuccess.value = true
    cancelEditRate()
    await loadRates()
  } catch (error: any) {
    rateMessage.value = error.message || t('rates.failedToSave')
    rateSuccess.value = false
  } finally {
    rateSaving.value = false
  }
}

const removeRate = async (categoryId: string) => {
  if (!confirm(t('rates.confirmRemove'))) return
  
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      alert(t('auth.userIdNotFound'))
      return
    }
    const specialist = await $fetch<SpecialistProfile>(`${config.public.apiBase}/specialists/${userId}`)
    const updatedRates = (specialist.categoryRates || []).filter((r: CategoryRate) => r.categoryId !== categoryId)
    
    await $fetch(`${config.public.apiBase}/specialists/${userId}`, {
      method: 'PUT',
      body: {
        ...specialist,
        categoryRates: updatedRates
      }
    })
    
    await loadRates()
  } catch (error: any) {
    alert(error.message || t('rates.failedToRemove'))
  }
}

const getCategoryName = (categoryId: string) => {
  const category = categories.value.find((c: Category) => c.id === categoryId)
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

const ratePagination = computed(() => {
  const total = rates.value.length
  const totalPages = Math.ceil(total / itemsPerPage)
  return {
    currentPage: currentRatePage.value,
    totalPages: totalPages || 1,
    totalCount: total
  }
})

const paginatedRates = computed(() => {
  const start = (currentRatePage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return rates.value.slice(start, end)
})

const goToRatePage = (page: number) => {
  const totalPages = ratePagination.value.totalPages
  if (page >= 1 && page <= totalPages) {
    currentRatePage.value = page
  }
}

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

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 2rem;
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
</style>
