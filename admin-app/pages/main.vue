<template>
  <div class="main-container">
    <nav class="menu-panel">
      <div class="menu-title">Admin Menu</div>
      <ul>
        <li :class="{ active: selectedMenu === 'specialists' }" @click="selectMenu('specialists')">
          Specialists
        </li>
        <li :class="{ active: selectedMenu === 'clients' }" @click="selectMenu('clients')">
          Clients
        </li>
        <li :class="{ active: selectedMenu === 'connections' }" @click="selectMenu('connections')">
          Type Connections
        </li>
        <li :class="{ active: selectedMenu === 'categories' }" @click="selectMenu('categories')">
          Categories
        </li>
      </ul>
      <div class="menu-divider"></div>
      <ul>
        <li class="logout" @click="logout">Logout</li>
      </ul>
    </nav>
    <div class="content">
      <h1>Welcome to Admin Panel</h1>

      <section v-if="selectedMenu === 'specialists'" class="section">
        <div class="section-header">
          <h2>Specialists</h2>
          <button type="button" class="btn" @click="loadSpecialists">Refresh</button>
        </div>

        <div class="list-state" v-if="specialistsLoading">Loading specialists...</div>
        <div class="list-state error" v-else-if="specialistsError">{{ specialistsError }}</div>

        <div class="table" v-else>
          <div class="table-header">
            <span>Name</span>
            <span>Email</span>
            <span>Phone</span>
            <span>Categories</span>
            <span>Rate Items</span>
            <span>Availability</span>
            <span>Actions</span>
          </div>
          <div v-for="specialist in specialists" :key="specialist.id" class="table-row">
            <span>{{ specialist.name }}</span>
            <span>{{ specialist.email }}</span>
            <span>{{ specialist.phone }}</span>
            <span>{{ resolveCategoryRates(specialist.categoryRates) }}</span>
            <span>{{ specialist.categoryRates.length }}</span>
            <span>{{ specialist.isAvailable ? 'Available' : 'Unavailable' }}</span>
            <span class="row-actions">
              <button type="button" class="btn" @click="startEditSpecialist(specialist)">Update</button>
              <button type="button" class="btn danger" @click="removeSpecialist(specialist.id)">Delete</button>
            </span>
          </div>
        </div>

        <div v-if="!specialistsLoading && !specialistsError" class="pagination">
          <div class="pagination-info">
            Page {{ currentPage }}
          </div>
          <div class="pagination-controls">
            <button type="button" class="btn" :disabled="currentPage === 1" @click="goToPreviousPage">
              Previous
            </button>
            <button type="button" class="btn" :disabled="isLastPage" @click="goToNextPage">
              Next
            </button>
          </div>
          <div class="pagination-size">
            <label for="page-size">Page size</label>
            <select id="page-size" v-model.number="pageSize" @change="handlePageSizeChange">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </div>
        </div>

        <form class="form" @submit.prevent>
          <div class="form-grid">
            <div class="form-field">
              <label for="specialist-name">Full Name</label>
              <input id="specialist-name" v-model="specialistForm.name" type="text" placeholder="Jane Doe" />
            </div>
            <div class="form-field">
              <label for="specialist-email">Email</label>
              <input id="specialist-email" v-model="specialistForm.email" type="email" placeholder="jane@example.com" />
            </div>
            <div class="form-field">
              <label for="specialist-phone">Phone</label>
              <input id="specialist-phone" v-model="specialistForm.phone" type="tel" placeholder="+1 555 123 4567" />
            </div>
            <div class="form-field form-field--full">
              <label>Category Rates</label>
              <div class="rate-rows">
                <div v-for="(rate, index) in specialistForm.categoryRates" :key="index" class="rate-row">
                  <select v-model="rate.categoryId">
                    <option value="">Select category</option>
                    <option v-for="category in categories" :key="category.id" :value="category.id">
                      {{ category.name }}
                    </option>
                  </select>
                  <input
                    v-model.number="rate.hourlyRate"
                    type="number"
                    min="1"
                    step="0.01"
                    placeholder="Rate"
                  />
                  <input
                    v-model.number="rate.experienceYears"
                    type="number"
                    min="0"
                    step="1"
                    placeholder="Experience"
                  />
                  <button type="button" class="btn" @click="removeCategoryRate(index)">Remove</button>
                </div>
                <button type="button" class="btn" @click="addCategoryRate">Add Category Rate</button>
              </div>
            </div>
            <div class="form-field form-field--full">
              <label for="specialist-bio">Bio</label>
              <textarea id="specialist-bio" v-model="specialistForm.bio" rows="4" placeholder="Short specialist bio"></textarea>
            </div>
            <div class="form-field">
              <label for="specialist-available">Available</label>
              <select id="specialist-available" v-model="specialistForm.isAvailable">
                <option :value="true">Yes</option>
                <option :value="false">No</option>
              </select>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn primary" @click="addSpecialist">Add Specialist</button>
            <button type="button" class="btn" :disabled="!selectedSpecialistId" @click="updateSpecialist">
              Update Specialist
            </button>
            <button type="button" class="btn danger" :disabled="!selectedSpecialistId" @click="deleteSelectedSpecialist">
              Delete Specialist
            </button>
            <button type="button" class="btn" @click="resetSpecialistForm">Clear</button>
          </div>

          <p v-if="specialistActionMessage" class="form-message">{{ specialistActionMessage }}</p>
        </form>
      </section>

      <section v-else-if="selectedMenu === 'clients'" class="section">
        <h2>Clients</h2>
        <p class="muted">Client management UI will be added here.</p>
      </section>

      <section v-else-if="selectedMenu === 'connections'" class="section">
        <h2>Type Connections</h2>
        <p class="muted">Connection types management UI will be added here.</p>
      </section>

      <section v-else-if="selectedMenu === 'categories'" class="section">
        <div class="section-header">
          <h2>Categories</h2>
          <button type="button" class="btn" @click="loadCategories">Refresh</button>
        </div>

        <div class="list-state" v-if="categoriesLoading">Loading categories...</div>
        <div class="list-state error" v-else-if="categoriesError">{{ categoriesError }}</div>

        <div class="table" v-else>
          <div class="table-header categories-table">
            <span>Name</span>
            <span>Description</span>
            <span>Parent</span>
            <span>Actions</span>
          </div>
          <div v-for="category in pagedCategories" :key="category.id" class="table-row categories-table">
            <span>{{ category.name }}</span>
            <span>{{ category.description || '-' }}</span>
            <span>{{ resolveCategoryName(category.parentId) }}</span>
            <span class="row-actions">
              <button type="button" class="btn" @click="startEditCategory(category)">Update</button>
              <button type="button" class="btn danger" @click="removeCategory(category.id)">Delete</button>
            </span>
          </div>
        </div>

        <div v-if="!categoriesLoading && !categoriesError" class="pagination">
          <div class="pagination-info">
            Page {{ categoryCurrentPage }} of {{ categoryTotalPages }}
          </div>
          <div class="pagination-controls">
            <button type="button" class="btn" :disabled="categoryCurrentPage === 1" @click="goToPreviousCategoryPage">
              Previous
            </button>
            <button type="button" class="btn" :disabled="categoryCurrentPage === categoryTotalPages" @click="goToNextCategoryPage">
              Next
            </button>
          </div>
          <div class="pagination-size">
            <label for="category-page-size">Page size</label>
            <select id="category-page-size" v-model.number="categoryPageSize" @change="handleCategoryPageSizeChange">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </div>
        </div>

        <form class="form" @submit.prevent>
          <div class="form-grid">
            <div class="form-field">
              <label for="category-name">Name</label>
              <input id="category-name" v-model="categoryForm.name" type="text" placeholder="Strategy" />
            </div>
            <div class="form-field">
              <label for="category-description">Description</label>
              <input
                id="category-description"
                v-model="categoryForm.description"
                type="text"
                placeholder="Business strategy consulting"
              />
            </div>
            <div class="form-field">
              <label for="category-parent">Parent Category</label>
              <select id="category-parent" v-model="categoryForm.parentId">
                <option :value="''">No parent</option>
                <option v-for="category in categories" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn primary" @click="addCategory">Add Category</button>
            <button type="button" class="btn" :disabled="!selectedCategoryId" @click="updateCategory">
              Update Category
            </button>
            <button type="button" class="btn danger" :disabled="!selectedCategoryId" @click="deleteSelectedCategory">
              Delete Category
            </button>
            <button type="button" class="btn" @click="resetCategoryForm">Clear</button>
          </div>

          <p v-if="categoryActionMessage" class="form-message">{{ categoryActionMessage }}</p>
        </form>
      </section>

      <div v-if="confirmState.visible" class="modal-overlay">
        <div class="modal">
          <div class="modal-header">{{ confirmState.title }}</div>
          <p class="modal-message">{{ confirmState.message }}</p>
          <div class="modal-actions">
            <button type="button" class="btn" @click="cancelConfirm">Cancel</button>
            <button type="button" class="btn danger" @click="acceptConfirm">Confirm</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'

const router = useRouter()
const config = useRuntimeConfig()

type MenuKey = 'specialists' | 'clients' | 'connections' | 'categories'

const selectedMenu = ref<MenuKey>('specialists')
const specialistForm = ref({
  name: '',
  email: '',
  phone: '',
  bio: '',
  categoryRates: [] as Array<{
    categoryId: string
    hourlyRate: number
    experienceYears: number
    rating?: number | null
    totalConsultations?: number | null
  }>,
  isAvailable: true,
})

type Specialist = {
  id: string
  name: string
  email: string
  phone: string
  bio: string
  categoryRates: Array<{
    categoryId: string
    hourlyRate: number
    experienceYears: number
    rating?: number | null
    totalConsultations?: number | null
  }>
  isAvailable: boolean
}

type Category = {
  id: string
  name: string
  description: string
  parentId: string | null
}

const specialists = ref<Specialist[]>([])
const specialistsLoading = ref(false)
const specialistsError = ref('')
const specialistActionMessage = ref('')
const selectedSpecialistId = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = ref(20)
const isLastPage = ref(false)
const categories = ref<Category[]>([])
const categoriesLoading = ref(false)
const categoriesError = ref('')
const selectedCategoryId = ref<string | null>(null)
const categoryActionMessage = ref('')
const categoryCurrentPage = ref(1)
const categoryPageSize = ref(20)
const categoryForm = ref({
  name: '',
  description: '',
  parentId: '' as string | '',
})

const categoryTotalPages = computed(() =>
  Math.max(1, Math.ceil(categories.value.length / categoryPageSize.value))
)

const pagedCategories = computed(() => {
  const start = (categoryCurrentPage.value - 1) * categoryPageSize.value
  return categories.value.slice(start, start + categoryPageSize.value)
})

const confirmState = ref({
  visible: false,
  title: '',
  message: '',
})
const confirmResolver = ref<((value: boolean) => void) | null>(null)

const selectMenu = (menu: MenuKey) => {
  selectedMenu.value = menu
}

const resolveCategoryRates = (
  rates: Array<{ categoryId: string; hourlyRate: number; experienceYears: number }>
) =>
  rates
    .map(rate => {
      const name = categories.value.find(category => category.id === rate.categoryId)?.name ?? rate.categoryId
      return `${name} (${rate.hourlyRate}, ${rate.experienceYears} yrs)`
    })
    .join(', ')

const resolveCategoryName = (id: string | null) => {
  if (!id) {
    return '-'
  }
  return categories.value.find(category => category.id === id)?.name ?? id
}

const loadCategories = async () => {
  categoriesLoading.value = true
  categoriesError.value = ''

  try {
    const data = await $fetch<Category[]>(`${config.public.apiBase}/categories`, {
      method: 'GET',
    })
    categories.value = data
    if (categoryCurrentPage.value > categoryTotalPages.value) {
      categoryCurrentPage.value = categoryTotalPages.value
    }
  } catch (error) {
    categories.value = []
    categoriesError.value = 'Failed to load categories'
  } finally {
    categoriesLoading.value = false
  }
}

const goToPreviousCategoryPage = () => {
  if (categoryCurrentPage.value > 1) {
    categoryCurrentPage.value -= 1
  }
}

const goToNextCategoryPage = () => {
  if (categoryCurrentPage.value < categoryTotalPages.value) {
    categoryCurrentPage.value += 1
  }
}

const handleCategoryPageSizeChange = () => {
  categoryCurrentPage.value = 1
}

const confirmAction = (title: string, message: string) =>
  new Promise<boolean>(resolve => {
    confirmState.value = {
      visible: true,
      title,
      message,
    }
    confirmResolver.value = resolve
  })

const acceptConfirm = () => {
  confirmState.value = { ...confirmState.value, visible: false }
  confirmResolver.value?.(true)
  confirmResolver.value = null
}

const cancelConfirm = () => {
  confirmState.value = { ...confirmState.value, visible: false }
  confirmResolver.value?.(false)
  confirmResolver.value = null
}

const loadSpecialists = async () => {
  specialistsLoading.value = true
  specialistsError.value = ''

  try {
    const data = await $fetch<Specialist[]>(`${config.public.apiBase}/specialists/search`, {
      method: 'GET',
      query: {
        offset: (currentPage.value - 1) * pageSize.value,
        limit: pageSize.value,
      },
    })
    specialists.value = data
    isLastPage.value = data.length < pageSize.value
  } catch (error) {
    specialistsError.value = 'Failed to load specialists'
  } finally {
    specialistsLoading.value = false
  }
}

const goToPreviousPage = async () => {
  if (currentPage.value > 1) {
    currentPage.value -= 1
    await loadSpecialists()
  }
}

const goToNextPage = async () => {
  if (!isLastPage.value) {
    currentPage.value += 1
    await loadSpecialists()
  }
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadSpecialists()
}

const resetSpecialistForm = () => {
  specialistForm.value = {
    name: '',
    email: '',
    phone: '',
    bio: '',
    categoryRates: [],
    isAvailable: true,
  }
  selectedSpecialistId.value = null
}

const resetCategoryForm = () => {
  categoryForm.value = {
    name: '',
    description: '',
    parentId: '',
  }
  selectedCategoryId.value = null
}

const startEditSpecialist = (specialist: Specialist) => {
  selectedSpecialistId.value = specialist.id
  specialistForm.value = {
    name: specialist.name,
    email: specialist.email,
    phone: specialist.phone,
    bio: specialist.bio,
    categoryRates: specialist.categoryRates.map(rate => ({ ...rate })),
    isAvailable: specialist.isAvailable,
  }
  specialistActionMessage.value = ''
}

const addCategoryRate = () => {
  specialistForm.value.categoryRates.push({
    categoryId: '',
    hourlyRate: 0,
    experienceYears: 0,
    rating: null,
    totalConsultations: null,
  })
}

const removeCategoryRate = (index: number) => {
  specialistForm.value.categoryRates.splice(index, 1)
}

const startEditCategory = (category: Category) => {
  selectedCategoryId.value = category.id
  categoryForm.value = {
    name: category.name,
    description: category.description,
    parentId: category.parentId ?? '',
  }
  categoryActionMessage.value = ''
}

const addSpecialist = async () => {
  specialistActionMessage.value = ''

  const confirmed = await confirmAction('Add Specialist', 'Add this specialist?')
  if (!confirmed) {
    return
  }

  try {
    await $fetch(`${config.public.apiBase}/specialists`, {
      method: 'POST',
      body: {
        email: specialistForm.value.email,
        name: specialistForm.value.name,
        phone: specialistForm.value.phone,
        bio: specialistForm.value.bio,
        categoryRates: specialistForm.value.categoryRates,
        isAvailable: specialistForm.value.isAvailable,
      },
    })

    specialistActionMessage.value = 'Specialist created successfully.'
    resetSpecialistForm()
    await loadSpecialists()
  } catch (error) {
    specialistActionMessage.value = 'Failed to create specialist.'
  }
}

const addCategory = async () => {
  categoryActionMessage.value = ''

  const confirmed = await confirmAction('Add Category', 'Add this category?')
  if (!confirmed) {
    return
  }

  try {
    await $fetch(`${config.public.apiBase}/categories`, {
      method: 'POST',
      body: {
        name: categoryForm.value.name,
        description: categoryForm.value.description,
        parentId: categoryForm.value.parentId || null,
      },
    })
    categoryActionMessage.value = 'Category created successfully.'
    resetCategoryForm()
    await loadCategories()
  } catch (error) {
    categoryActionMessage.value = 'Failed to create category.'
  }
}

const updateSpecialist = async () => {
  if (!selectedSpecialistId.value) {
    specialistActionMessage.value = 'Select a specialist to update.'
    return
  }

  const confirmed = await confirmAction('Update Specialist', 'Update this specialist?')
  if (!confirmed) {
    return
  }

  specialistActionMessage.value = ''

  try {
    await $fetch(`${config.public.apiBase}/specialists/${selectedSpecialistId.value}`, {
      method: 'PUT',
      body: {
        email: specialistForm.value.email,
        name: specialistForm.value.name,
        phone: specialistForm.value.phone,
        bio: specialistForm.value.bio,
        categoryRates: specialistForm.value.categoryRates,
        isAvailable: specialistForm.value.isAvailable,
      },
    })

    specialistActionMessage.value = 'Specialist updated successfully.'
    await loadSpecialists()
  } catch (error) {
    specialistActionMessage.value = 'Failed to update specialist.'
  }
}

const updateCategory = async () => {
  if (!selectedCategoryId.value) {
    categoryActionMessage.value = 'Select a category to update.'
    return
  }

  const confirmed = await confirmAction('Update Category', 'Update this category?')
  if (!confirmed) {
    return
  }

  categoryActionMessage.value = ''

  try {
    await $fetch(`${config.public.apiBase}/categories/${selectedCategoryId.value}`, {
      method: 'PUT',
      body: {
        name: categoryForm.value.name,
        description: categoryForm.value.description,
        parentId: categoryForm.value.parentId || null,
      },
    })
    categoryActionMessage.value = 'Category updated successfully.'
    await loadCategories()
  } catch (error) {
    categoryActionMessage.value = 'Failed to update category.'
  }
}

const removeSpecialist = async (specialistId: string) => {
  specialistActionMessage.value = ''

  const confirmed = await confirmAction('Delete Specialist', 'Delete this specialist?')
  if (!confirmed) {
    return
  }

  try {
    await $fetch(`${config.public.apiBase}/specialists/${specialistId}`, {
      method: 'DELETE',
    })
    specialistActionMessage.value = 'Specialist deleted successfully.'
    if (selectedSpecialistId.value === specialistId) {
      resetSpecialistForm()
    }
    if (currentPage.value > 1 && specialists.value.length === 1) {
      currentPage.value -= 1
    }
    await loadSpecialists()
  } catch (error) {
    specialistActionMessage.value = 'Failed to delete specialist.'
  }
}

const removeCategory = async (categoryId: string) => {
  categoryActionMessage.value = ''

  const confirmed = await confirmAction('Delete Category', 'Delete this category?')
  if (!confirmed) {
    return
  }

  try {
    await $fetch(`${config.public.apiBase}/categories/${categoryId}`, {
      method: 'DELETE',
    })
    categoryActionMessage.value = 'Category deleted successfully.'
    if (selectedCategoryId.value === categoryId) {
      resetCategoryForm()
    }
    await loadCategories()
  } catch (error) {
    categoryActionMessage.value = 'Failed to delete category.'
  }
}

const deleteSelectedCategory = async () => {
  if (!selectedCategoryId.value) {
    categoryActionMessage.value = 'Select a category to delete.'
    return
  }
  await removeCategory(selectedCategoryId.value)
}

const deleteSelectedSpecialist = async () => {
  if (!selectedSpecialistId.value) {
    specialistActionMessage.value = 'Select a specialist to delete.'
    return
  }
  await removeSpecialist(selectedSpecialistId.value)
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
    localStorage.removeItem('admin_session')
    router.push('/login')
  }
}

onMounted(() => {
  loadSpecialists()
  loadCategories()
})
</script>

<style scoped>
.main-container {
  display: flex;
  min-height: 100vh;
}
.menu-panel {
  width: 200px;
  background: #f5f5f5;
  padding: 2rem 1rem;
  border-right: 1px solid #ddd;
}
.menu-title {
  font-weight: 700;
  margin-bottom: 1rem;
  color: #1f2937;
}
.menu-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 1rem 0;
}
.menu-panel ul {
  list-style: none;
  padding: 0;
}
.menu-panel li {
  padding: 0.75rem 1rem;
  cursor: pointer;
  color: #007bff;
  border-radius: 4px;
  transition: background 0.2s;
}
.menu-panel li.active {
  background: #e0e7ff;
  color: #1d4ed8;
  font-weight: 600;
}
.menu-panel li.logout {
  color: #dc2626;
}
.menu-panel li:hover {
  background: #e6e6e6;
}
.content {
  flex: 1;
  padding: 2rem;
}
.section {
  margin-top: 2rem;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}
.list-state {
  padding: 0.75rem 1rem;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  margin-bottom: 1rem;
}
.list-state.error {
  background: #fee2e2;
  color: #b91c1c;
}
.table {
  display: flex;
  flex-direction: column;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 1.5rem;
}
.table-header,
.table-row {
  display: grid;
  grid-template-columns: 1.2fr 1.5fr 1fr 1.4fr 0.8fr 0.8fr 1.3fr;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  align-items: center;
}
.table-header.categories-table,
.table-row.categories-table {
  grid-template-columns: 1fr 2fr 1fr 1.2fr;
}
.table-header {
  background: #f1f5f9;
  font-weight: 600;
  color: #1f2937;
}
.table-row {
  border-top: 1px solid #e5e7eb;
  background: #ffffff;
}
.row-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
}
.pagination-info {
  font-weight: 600;
  color: #1f2937;
}
.pagination-controls {
  display: flex;
  gap: 0.5rem;
}
.pagination-size {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #374151;
}
.section h2 {
  margin-bottom: 1rem;
  font-size: 1.5rem;
  color: #111827;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.form-field--full {
  grid-column: 1 / -1;
}
.form-field label {
  font-weight: 600;
  color: #374151;
}
.form-field input,
.form-field textarea,
.form-field select {
  padding: 0.6rem 0.8rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.95rem;
  outline: none;
  transition: border 0.2s ease;
}
.form-field select[multiple] {
  min-height: 120px;
}
.rate-rows {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.rate-row {
  display: grid;
  grid-template-columns: 1.2fr 0.6fr 0.6fr auto;
  gap: 0.75rem;
  align-items: center;
}
.form-field input:focus,
.form-field textarea:focus,
.form-field select:focus {
  border-color: #6366f1;
}
.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}
.form-message {
  margin-top: 0.5rem;
  color: #1f2937;
}
.btn {
  padding: 0.65rem 1.2rem;
  border-radius: 6px;
  border: 1px solid #cbd5f5;
  background: #ffffff;
  color: #1f2937;
  cursor: pointer;
  font-weight: 600;
}
.btn.primary {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
}
.btn.danger {
  background: #fee2e2;
  color: #b91c1c;
  border-color: #fecaca;
}
.muted {
  color: #6b7280;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}
.modal {
  background: #ffffff;
  padding: 1.5rem;
  border-radius: 12px;
  width: min(420px, 90vw);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
}
.modal-header {
  font-size: 1.2rem;
  font-weight: 700;
  color: #111827;
  margin-bottom: 0.75rem;
}
.modal-message {
  color: #374151;
  margin-bottom: 1.25rem;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
</style>
