<template>
  <div class="tab-panel">
    <div class="form" @click="closeDropdownsOnFormClick">
      <h4>{{ $t('consultations.book.title') }}</h4>

      <div class="form-grid">
        <!-- Specialist selection -->
        <div class="form-field specialist-field" style="position: relative;">
          <label>{{ $t('consultations.book.specialistLabel') }} *</label>
          <input 
            type="text" 
            v-model="specialistSearch" 
            :placeholder="$t('consultations.book.searchSpecialist')"
            @focus="handleSpecialistFocus"
            @click="handleSpecialistClick"
            @input="handleSpecialistInput"
          />
          <div v-if="showSpecialistDropdown" class="dropdown" @click.stop style="display: block; background: white; border: 1px solid #e5e7eb; position: absolute; top: 100%; left: 0; right: 0; z-index: 1000; width: 100%; max-height: 300px; overflow-y: auto; border-radius: 6px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
            <div v-if="specialistsLoading" style="padding: 0.75rem;">
              <div class="text-gray-500">{{ $t('consultations.book.loadingSpecialists') }}</div>
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
                {{ $t('consultations.book.noSpecialists') }}
              </div>
            </div>
          </div>
        </div>

        <!-- Category selection -->
        <div class="form-field category-field" style="position: relative;">
          <label>{{ $t('consultations.book.categoryLabel') }} *</label>
          <input 
            type="text" 
            :value="selectedCategory?.name || ''" 
            readonly 
            :placeholder="$t('consultations.book.selectCategory')"
            @focus="handleCategoryFocus"
            @click="handleCategoryClick"
          />
          <div v-if="showCategoryDropdown" class="dropdown" @click.stop style="display: block; background: white; border: 1px solid #e5e7eb; position: absolute; top: 100%; left: 0; right: 0; z-index: 1000; width: 100%; max-height: 300px; overflow-y: auto; border-radius: 6px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
            <div v-if="categoriesLoading" style="padding: 0.75rem;">
              <div class="text-gray-500">{{ $t('consultations.book.loadingCategories') }}</div>
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
                <div v-if="!selectedSpecialist">{{ $t('consultations.book.selectSpecialistFirst') }}</div>
                <div v-else>{{ $t('consultations.book.noCategories') }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Description -->
        <div class="form-field" style="grid-column: 1 / -1;">
          <label>{{ $t('consultations.book.descriptionLabel') }} *</label>
          <textarea v-model="form.description" rows="3" :placeholder="$t('consultations.book.descriptionPlaceholder')"></textarea>
        </div>

        <!-- Date -->
        <div class="form-field">
          <label>{{ $t('consultations.book.scheduledDate') }} *</label>
          <input type="date" v-model="form.scheduledDate" />
        </div>

        <!-- Time -->
        <div class="form-field">
          <label>{{ $t('consultations.book.scheduledTime') }} *</label>
          <input type="time" v-model="form.scheduledTime" />
        </div>

        <!-- Available Slots -->
        <div v-if="selectedSpecialist && form.scheduledDate" class="form-field" style="grid-column: 1 / -1;">
          <label>{{ $t('consultations.book.availableSlots') }}</label>
          <div v-if="slotsLoading" class="slots-container">
            <div class="slots-spinner">{{ $t('consultations.book.loadingSlots') }}</div>
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
                :class="{ selected: form.scheduledTime === slot.startTime }"
                @click="selectSlot(slot)"
              >
                {{ slot.startTime }} - {{ slot.endTime }}
              </button>
            </div>
          </div>
          <div v-else class="slots-container empty">
            <p>{{ $t('consultations.book.noSlots') }}</p>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button 
          class="btn btn-primary" 
          @click="createConsultation"
          :disabled="!isFormValid"
        >
          {{ creating ? $t('common.booking') : $t('consultations.book.bookButton') }}
        </button>
      </div>

      <div v-if="message" :class="['form-message', success ? 'success' : 'error']">
        {{ message }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { useApi } from '~/composables/useApi'

interface Category {
  id: string
  name: string
}

interface AvailableSlot {
  startTime: string
  endTime: string
}

interface SlotsResponse {
  slots: AvailableSlot[]
}

const emit = defineEmits(['consultation-created'])

const config = useRuntimeConfig()
const { $fetch } = useApi()
const { t } = useI18n()

const form = ref({
  specialistId: '',
  categoryId: '',
  description: '',
  scheduledDate: '',
  scheduledTime: ''
})

const creating = ref(false)
const message = ref('')
const success = ref(false)
const availableSlots = ref<AvailableSlot[]>([])
const slotsLoading = ref(false)
const slotsError = ref('')

const specialists = ref<any[]>([])
const categories = ref<Category[]>([])
const specialistsLoading = ref(false)
const categoriesLoading = ref(false)
const specialistSearch = ref('')
const selectedSpecialist = ref<any>(null)
const selectedCategory = ref<any>(null)
const showSpecialistDropdown = ref(false)
const showCategoryDropdown = ref(false)

const loadSpecialists = async () => {
  specialistsLoading.value = true
  try {
    const url = `${config.public.apiBase}/specialists/search?offset=0&limit=1000`
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
    const data = await $fetch<Category[]>(`${config.public.apiBase}/categories`)
    categories.value = data || []
  } catch (error: any) {
    console.error('Categories load error:', error)
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
  
  const specialistCategoryIds = selectedSpecialist.value.categoryRates.map((rate: any) => rate.categoryId)
  const filtered = categories.value.filter(category => specialistCategoryIds.includes(category.id))
  return filtered
})

const isFormValid = computed(() => {
  return !creating.value &&
    form.value.specialistId &&
    form.value.categoryId &&
    form.value.description &&
    form.value.scheduledDate &&
    form.value.scheduledTime
})

const selectSpecialist = (specialist: any) => {
  selectedSpecialist.value = specialist
  form.value.specialistId = specialist.id
  specialistSearch.value = specialist.name
  form.value.categoryId = ''
  selectedCategory.value = null
  showSpecialistDropdown.value = false
  
  setTimeout(() => {
    showCategoryDropdown.value = true
  }, 100)
}

const selectCategoryFromDropdown = (category: any) => {
  selectedCategory.value = category
  form.value.categoryId = category.id
  showCategoryDropdown.value = false
}

const handleSpecialistFocus = (e: Event) => {
  e.stopImmediatePropagation()
  showSpecialistDropdown.value = true
  if (specialists.value.length === 0) loadSpecialists()
}

const handleSpecialistClick = (e: Event) => {
  e.stopImmediatePropagation()
  showSpecialistDropdown.value = true
}

const handleSpecialistInput = (e: Event) => {
  e.stopImmediatePropagation()
  showSpecialistDropdown.value = true
  if (specialists.value.length === 0) loadSpecialists()
}

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

const closeDropdownsOnFormClick = (e: MouseEvent) => {
  const path = (e as any).composedPath() as HTMLElement[]
  
  const insideSpecialist = path.some(el => el.classList?.contains('specialist-field'))
  const insideCategory = path.some(el => el.classList?.contains('category-field'))
  const insideDropdown = path.some(el => el.classList?.contains('dropdown'))
  
  if (!insideSpecialist && !insideDropdown) {
    showSpecialistDropdown.value = false
  }
  if (!insideCategory && !insideDropdown) {
    showCategoryDropdown.value = false
  }
}

const handleEscapeKey = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    showSpecialistDropdown.value = false
    showCategoryDropdown.value = false
  }
}

const loadAvailableSlots = async () => {
  if (!form.value.specialistId || !form.value.scheduledDate) {
    availableSlots.value = []
    return
  }

  slotsLoading.value = true
  slotsError.value = ''
  availableSlots.value = []

  try {
    const response = await $fetch<SlotsResponse>(`${config.public.apiBase}/specialists/${form.value.specialistId}/availability/slots`, {
      query: {
        date: form.value.scheduledDate,
        durationMinutes: 60
      }
    })
    
    if (response.slots && Array.isArray(response.slots)) {
      availableSlots.value = response.slots
      if (response.slots.length === 0) {
        slotsError.value = t('consultations.book.noSlotsForDate')
      }
    }
  } catch (error: any) {
    console.error('Slots load error:', error)
    slotsError.value = error.data?.message || error.message || t('consultations.book.failedToLoadSlots')
    availableSlots.value = []
  } finally {
    slotsLoading.value = false
  }
}

const selectSlot = (slot: any) => {
  form.value.scheduledTime = slot.startTime
}

const createConsultation = async () => {
  creating.value = true
  message.value = ''
  success.value = false
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      message.value = t('auth.userIdNotFound')
      return
    }
    
    const scheduledDateTime = form.value.scheduledDate && form.value.scheduledTime 
      ? new Date(`${form.value.scheduledDate}T${form.value.scheduledTime}`).toISOString()
      : null
    
    const body = {
      userId,
      specialistId: form.value.specialistId,
      categoryId: form.value.categoryId,
      description: form.value.description,
      scheduledAt: scheduledDateTime,
      duration: 60
    }
    
    await $fetch(`${config.public.apiBase}/consultations`, {
      method: 'POST',
      body
    })
    
    message.value = t('consultations.book.created')
    success.value = true
    form.value = {
      specialistId: '',
      categoryId: '',
      description: '',
      scheduledDate: '',
      scheduledTime: ''
    }
    selectedSpecialist.value = null
    selectedCategory.value = null
    specialistSearch.value = ''
    
    // Emit event to reload consultations in parent
    emit('consultation-created')
    
  } catch (error: any) {
    console.error('Consultation create error:', error)
    message.value = error.data?.message || error.message || t('consultations.book.failedToCreate')
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleEscapeKey)
  loadSpecialists()
  loadCategories()
  
  watch([() => form.value.specialistId, () => form.value.scheduledDate], () => {
    loadAvailableSlots()
  })
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscapeKey)
})

defineExpose({
  form,
  selectedSpecialist,
  selectedCategory
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
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  position: relative;
}

.form-field label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
  font-size: 0.9rem;
}

.form-field input,
.form-field textarea,
.form-field select {
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.95rem;
  font-family: inherit;
}

.form-field input:focus,
.form-field textarea:focus,
.form-field select:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-field textarea {
  resize: vertical;
}

.dropdown {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  z-index: 1000;
}

.form-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
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

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.slots-container {
  padding: 1rem;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.slots-container.error {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #991b1b;
}

.slots-container.empty {
  background: #f3f4f6;
  border-color: #d1d5db;
  color: #666;
}

.slots-spinner {
  text-align: center;
  color: #666;
}

.slots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 0.75rem;
}

.slot-button {
  padding: 0.75rem;
  border: 2px solid #d1d5db;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.15s;
}

.slot-button:hover {
  border-color: #4f46e5;
  color: #4f46e5;
}

.slot-button.selected {
  background: #4f46e5;
  color: white;
  border-color: #4f46e5;
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

.text-gray-500 {
  color: #6b7280;
}
</style>
