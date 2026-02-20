# Admin App Refactoring Proposal

## Executive Summary

The admin-app subproject requires significant refactoring to improve maintainability, type safety, and code organization. Current issues include massive component files (1800+ lines), duplicated logic, and underutilized architecture (Pinia not used).

---

## Current State Analysis

### Metrics
- **Total Files**: 30
- **Largest Component**: `SpecialistsSection.vue` (1,821 lines)
- **Second Largest**: `ClientsSection.vue` (1,171 lines)
- **State Management**: Pinia installed but unused
- **Type Safety**: Extensive `any` usage throughout

### Key Issues

| Issue | Severity | Files Affected |
|-------|----------|----------------|
| Monolithic components | 🔴 Critical | All section components |
| Duplicated CRUD logic | 🔴 Critical | 4 section components |
| Missing Pinia stores | 🟡 High | Entire app |
| Inconsistent error handling | 🟡 High | All composables/pages |
| Weak TypeScript types | 🟡 High | All components |
| No API response types | 🟡 Medium | `useApi.ts` |
| Hardcoded strings in templates | 🟡 Medium | All components |
| No loading/error states abstraction | 🟢 Low | All components |

---

## Refactoring Recommendations

### 1. **Extract Pinia Stores** (High Priority)

Create dedicated stores for each entity to centralize state management.

#### New Structure:
```
stores/
├── specialists.ts
├── clients.ts
├── categories.ts
├── connectionTypes.ts
└── auth.ts
```

#### Example: `stores/specialists.ts`
```typescript
import { defineStore } from 'pinia'
import type { Specialist, SpecialistCategoryRate, SpecialistConnection } from '~/types/specialist'

interface SpecialistsState {
  items: Specialist[]
  loading: boolean
  error: string | null
  pagination: {
    currentPage: number
    pageSize: number
    total: number
  }
  searchQuery: string
  selectedId: string | null
}

export const useSpecialistsStore = defineStore('specialists', {
  state: (): SpecialistsState => ({
    items: [],
    loading: false,
    error: null,
    pagination: { currentPage: 1, pageSize: 20, total: 0 },
    searchQuery: '',
    selectedId: null,
  }),

  getters: {
    filteredSpecialists: (state) => {
      if (!state.searchQuery.trim()) return state.items
      const query = state.searchQuery.toLowerCase()
      return state.items.filter(s =>
        s.name.toLowerCase().includes(query) ||
        s.email.toLowerCase().includes(query)
      )
    },
    pagedSpecialists: (state) => {
      const start = (state.pagination.currentPage - 1) * state.pagination.pageSize
      return state.filteredSpecialists.slice(start, start + state.pagination.pageSize)
    },
    selectedSpecialist: (state) =>
      state.items.find(s => s.id === state.selectedId) || null,
  },

  actions: {
    async fetchSpecialists() {
      this.loading = true
      this.error = null
      try {
        const config = useRuntimeConfig()
        const { $fetch } = useApi()
        const data = await $fetch<Specialist[]>(`${config.public.apiBase}/specialists/search`, {
          query: { offset: 0, limit: 1000 }
        })
        this.items = data
      } catch (e: any) {
        this.error = e.data?.message || 'Failed to load specialists'
      } finally {
        this.loading = false
      }
    },

    async updateSpecialist(id: string, data: Partial<Specialist>) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()
      await $fetch(`${config.public.apiBase}/specialists/${id}`, {
        method: 'PUT',
        body: data
      })
      await this.fetchSpecialists()
    },

    async deleteSpecialist(id: string) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()
      await $fetch(`${config.public.apiBase}/specialists/${id}`, { method: 'DELETE' })
      this.items = this.items.filter(s => s.id !== id)
      if (this.selectedId === id) this.selectedId = null
    },

    setSelectedSpecialist(id: string | null) {
      this.selectedId = id
    },

    setSearchQuery(query: string) {
      this.searchQuery = query
      this.pagination.currentPage = 1
    },

    setPageSize(size: number) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
    },
  },
})
```

---

### 2. **Create Reusable CRUD Composable** (High Priority)

Extract duplicated CRUD operations into a generic composable.

#### `composables/useCrud.ts`
```typescript
import type { Ref } from 'vue'

interface UseCrudOptions<T, C = Partial<T>, U = Partial<T>> {
  fetchUrl: string
  createUrl?: string
  updateUrl: (id: string) => string
  deleteUrl: (id: string) => string
  onSuccess?: (action: 'create' | 'update' | 'delete') => void
  onError?: (action: 'create' | 'update' | 'delete', error: any) => void
}

interface UseCrudReturn<T> {
  items: Ref<T[]>
  loading: Ref<boolean>
  error: Ref<string | null>
  fetchItems: () => Promise<void>
  createItem: (data: any) => Promise<void>
  updateItem: (id: string, data: any) => Promise<void>
  deleteItem: (id: string) => Promise<void>
  confirmAction: (title: string, message: string) => Promise<boolean>
}

export function useCrud<T = any, C = any, U = any>(
  options: UseCrudOptions<T, C, U>
): UseCrudReturn<T> {
  const items = ref<T[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const { t } = useI18n()

  const confirmState = ref({ visible: false, title: '', message: '' })
  const confirmResolver = ref<((value: boolean) => void) | null>(null)

  const confirmAction = (title: string, message: string) =>
    new Promise<boolean>((resolve) => {
      confirmState.value = { visible: true, title, message }
      confirmResolver.value = (confirmed: boolean) => {
        resolve(confirmed)
        confirmState.value.visible = false
      }
    })

  const fetchItems = async () => {
    loading.value = true
    error.value = null
    try {
      const { $fetch } = useApi()
      const data = await $fetch<T[]>(options.fetchUrl)
      items.value = data
    } catch (e: any) {
      error.value = e.data?.message || t('common.failedToLoad')
      options.onError?.('create', e)
    } finally {
      loading.value = false
    }
  }

  const createItem = async (data: C) => {
    if (!options.createUrl) throw new Error('createUrl not provided')
    loading.value = true
    try {
      const { $fetch } = useApi()
      await $fetch<T>(options.createUrl, { method: 'POST', body: data })
      await fetchItems()
      options.onSuccess?.('create')
    } catch (e: any) {
      error.value = e.data?.message || t('common.failedToCreate')
      options.onError?.('create', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  const updateItem = async (id: string, data: U) => {
    loading.value = true
    try {
      const { $fetch } = useApi()
      await $fetch<T>(options.updateUrl(id), { method: 'PUT', body: data })
      await fetchItems()
      options.onSuccess?.('update')
    } catch (e: any) {
      error.value = e.data?.message || t('common.failedToUpdate')
      options.onError?.('update', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  const deleteItem = async (id: string) => {
    loading.value = true
    try {
      const { $fetch } = useApi()
      await $fetch(options.deleteUrl(id), { method: 'DELETE' })
      items.value = items.value.filter((item: any) => item.id !== id)
      options.onSuccess?.('delete')
    } catch (e: any) {
      error.value = e.data?.message || t('common.failedToDelete')
      options.onError?.('delete', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    fetchItems()
  })

  return {
    items,
    loading,
    error,
    fetchItems,
    createItem,
    updateItem,
    deleteItem,
    confirmAction,
  }
}
```

---

### 3. **Define Proper TypeScript Types** (High Priority)

#### `types/api.ts`
```typescript
export interface ApiResponse<T> {
  data: T
  success: boolean
  message?: string
}

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  offset: number
  limit: number
}

export interface User {
  id: string
  login: string
  email: string
  name: string
  phone?: string
  role: 'Client' | 'Specialist' | 'Admin'
  createdAt: string
  updatedAt: string
}

export interface Specialist extends User {
  role: 'Specialist'
  bio?: string
  isAvailable: boolean
  categoryRates: SpecialistCategoryRate[]
  connections: SpecialistConnection[]
  consultationIds: string[]
}

export interface SpecialistCategoryRate {
  categoryId: string
  hourlyRate: number
  experienceYears: number
}

export interface SpecialistConnection {
  id: string
  connectionTypeId: string
  connectionValue: string
  isVerified: boolean
}

export interface Client extends User {
  role: 'Client'
  consultationIds: string[]
}

export interface Category {
  id: string
  name: string
  description?: string
  parentId?: string | null
}

export interface ConnectionType {
  id: string
  name: string
  description?: string | null
}

export interface NotificationPreference {
  id: string
  userId: string
  notificationType: string
  emailEnabled: boolean
  smsEnabled: boolean
}
```

---

### 4. **Create Base Table Component** (Medium Priority)

#### `components/base/BaseTable.vue`
```vue
<template>
  <div class="table">
    <div class="table-header" :class="tableClass">
      <span v-for="(column, index) in columns" :key="index">
        {{ column }}
      </span>
    </div>
    <div
      v-for="(item, index) in items"
      :key="item.id || index"
      class="table-row"
      :class="tableClass"
    >
      <slot name="row" :item="item" :index="index">
        <span v-for="(column, colIndex) in columns" :key="colIndex">
          {{ item[column.toLowerCase()] }}
        </span>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  columns: string[]
  items: any[]
  tableClass?: string
}>()
</script>
```

---

### 5. **Create Base Form Components** (Medium Priority)

#### `components/base/BaseForm.vue`
```vue
<template>
  <form class="form" @submit.prevent>
    <div class="form-grid" :style="{ gridTemplateColumns: repeat(auto-fit, minmax(${minWidth}px, 1fr)) }">
      <slot />
    </div>
    <div class="form-actions">
      <slot name="actions" />
    </div>
    <div v-if="message" :class="['form-message', type]">
      {{ message }}
    </div>
  </form>
</template>

<script setup lang="ts">
defineProps<{
  message?: string
  type?: 'success' | 'error' | 'info'
  minWidth?: number
}>()
</script>
```

#### `components/base/BaseFormField.vue`
```vue
<template>
  <div class="form-field" :class="{ 'form-field--full': full }">
    <label v-if="label" :for="id">{{ label }}</label>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label?: string
  id?: string
  full?: boolean
}>()
</script>
```

---

### 6. **Refactor Section Components** (High Priority)

After implementing stores and composables, refactor section components:

#### Before (`SpecialistsSection.vue`): 1,821 lines
#### After: ~300 lines

```vue
<template>
  <section v-if="visible" class="section">
    <SectionHeader
      :title="$t('adminSpecialists.title')"
      @refresh="store.fetchSpecialists()"
    />

    <SearchBar
      v-model="store.searchQuery"
      :placeholder="$t('adminSpecialists.searchPlaceholder')"
      @clear="store.searchQuery = ''"
    />

    <BaseTable
      v-if="!store.loading && !store.error"
      :columns="[
        $t('common.name'),
        $t('common.email'),
        $t('common.phone'),
        $t('adminSpecialists.categories'),
        $t('common.actions')
      ]"
      :items="store.pagedSpecialists"
    >
      <template #row="{ item }">
        <span>{{ item.name }}</span>
        <span>{{ item.email }}</span>
        <span>{{ item.phone }}</span>
        <span>{{ item.categoryRates.length }}</span>
        <span class="row-actions">
          <BaseButton @click="store.setSelectedSpecialist(item.id)">
            ✏️ {{ $t('common.select') }}
          </BaseButton>
          <BaseButton variant="danger" @click="handleDelete(item.id)">
            🗑️ {{ $t('common.delete') }}
          </BaseButton>
        </span>
      </template>
    </BaseTable>

    <Pagination
      v-model:page="store.pagination.currentPage"
      v-model:page-size="store.pagination.pageSize"
      :total="store.filteredSpecialists.length"
    />

    <SpecialistForm
      v-if="store.selectedId"
      :specialist="store.selectedSpecialist"
      @update="handleUpdate"
      @clear="store.setSelectedSpecialist(null)"
    />
  </section>
</template>

<script setup lang="ts">
import { useSpecialistsStore } from '~/stores/specialists'

defineProps<{ visible: boolean }>()

const store = useSpecialistsStore()
const { confirmAction } = useCrudActions()
const { t } = useI18n()

const handleDelete = async (id: string) => {
  const confirmed = await confirmAction(
    t('adminSpecialists.deleteSpecialist'),
    t('adminSpecialists.deleteSpecialistConfirm')
  )
  if (confirmed) {
    await store.deleteSpecialist(id)
  }
}

const handleUpdate = async (data: any) => {
  await store.updateSpecialist(store.selectedId!, data)
}
</script>
```

---

### 7. **Improve Error Handling** (Medium Priority)

#### `composables/useApiError.ts` (Enhanced)
```typescript
export enum ErrorCode {
  NOT_FOUND = 'NOT_FOUND',
  UNAUTHORIZED = 'UNAUTHORIZED',
  FORBIDDEN = 'FORBIDDEN',
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  CONFLICT = 'CONFLICT',
}

export class ApiError extends Error {
  constructor(
    public code: ErrorCode,
    public status: number,
    message: string,
    public details?: any
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export function useApiError() {
  const { t, te } = useI18n()

  const translateError = (error: ApiError | string): string => {
    if (typeof error === 'string') return error

    const key = `errors.${error.code}`
    return te(key) ? t(key) : error.message
  }

  const handleApiError = (e: any): ApiError => {
    const data = e?.data || e?.response?._data
    const code = data?.error || ErrorCode.ERROR
    const message = data?.message || e?.message || 'An error occurred'

    return new ApiError(code, e?.status || 500, message, data)
  }

  return {
    translateError,
    handleApiError,
    ApiError,
    ErrorCode,
  }
}
```

---

### 8. **Enhance API Composable** (Medium Priority)

#### `composables/useApi.ts` (Enhanced)
```typescript
import { ofetch } from 'ofetch'
import type { ApiResponse } from '~/types/api'
import { useApiError } from './useApiError'

interface FetchOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: any
  headers?: Record<string, string>
  query?: Record<string, any>
  requiresAuth?: boolean
}

export function useApi() {
  const { handleApiError } = useApiError()

  async function $fetch<T>(
    url: string,
    options: FetchOptions = {}
  ): Promise<T> {
    const {
      method = 'GET',
      body,
      headers = {},
      query,
      requiresAuth = true,
    } = options

    let token: string | null = null
    if (process.client && requiresAuth) {
      token = sessionStorage.getItem('accessToken') || sessionStorage.getItem('sessionId')
    }

    const config = useRuntimeConfig()

    try {
      return await ofetch<T>(url, {
        baseURL: config.public.apiBase,
        method,
        headers: {
          'Content-Type': 'application/json',
          ...(token && { Authorization: `Bearer ${token}` }),
          ...headers,
        },
        body: body ? JSON.stringify(body) : undefined,
        query,
        onResponseError({ response }) {
          if (response.status === 401) {
            // Handle unauthorized - redirect to login
            if (process.client) {
              sessionStorage.clear()
              window.location.href = '/login'
            }
          }
        },
      })
    } catch (error: any) {
      throw handleApiError(error)
    }
  }

  return { $fetch }
}
```

---

### 9. **Add Loading & Notification Components** (Low Priority)

#### `components/base/BaseLoading.vue`
```vue
<template>
  <div class="loading-spinner" :class="{ 'loading-spinner--fullscreen': fullscreen }">
    <div class="spinner" />
    <span v-if="message" class="loading-message">{{ message }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  message?: string
  fullscreen?: boolean
}>()
</script>
```

#### `components/base/BaseNotification.vue`
```vue
<template>
  <Transition name="slide">
    <div v-if="visible" :class="['notification', `notification--${type}`]">
      <span class="notification-icon">{{ icon }}</span>
      <span class="notification-message">{{ message }}</span>
      <button class="notification-close" @click="close">×</button>
    </div>
  </Transition>
</template>

<script setup lang="ts">
const props = defineProps<{
  visible: boolean
  message: string
  type: 'success' | 'error' | 'info' | 'warning'
}>()

const emit = defineEmits<{
  close: []
}>()

const icons = {
  success: '✓',
  error: '✕',
  info: 'ℹ',
  warning: '⚠',
}

const icon = computed(() => icons[props.type])
const close = () => emit('close')
</script>
```

---

## New File Structure

```
admin-app/
├── app.vue
├── nuxt.config.ts
├── package.json
├── tsconfig.json
├── assets/
│   └── css/
│       └── main.css
├── components/
│   ├── base/
│   │   ├── BaseButton.vue
│   │   ├── BaseForm.vue
│   │   ├── BaseFormField.vue
│   │   ├── BaseInput.vue
│   │   ├── BaseSelect.vue
│   │   ├── BaseTable.vue
│   │   ├── BaseLoading.vue
│   │   ├── BaseNotification.vue
│   │   └── SectionHeader.vue
│   ├── SearchBar.vue
│   ├── Pagination.vue
│   ├── LocaleSwitcher.vue
│   └── specialists/
│       ├── SpecialistsSection.vue (refactored, ~300 lines)
│       └── SpecialistForm.vue
├── composables/
│   ├── useApi.ts (enhanced)
│   ├── useApiError.ts (enhanced)
│   ├── useCrud.ts (new)
│   ├── useLogin.ts
│   └── useRegister.ts
├── stores/
│   ├── auth.ts
│   ├── specialists.ts
│   ├── clients.ts
│   ├── categories.ts
│   └── connectionTypes.ts
├── types/
│   ├── api.ts
│   ├── specialist.ts
│   ├── client.ts
│   ├── category.ts
│   └── connection.ts
├── pages/
│   ├── index.vue
│   ├── login.vue
│   └── main.vue
├── middleware/
│   └── auth.global.ts
├── plugins/
│   ├── element-plus.ts
│   └── i18n-messages.ts
├── i18n/
│   ├── i18n.config.ts
│   └── locales/
│       ├── en.json
│       ├── ua.json
│       └── ...
└── tests/
    ├── example.test.ts
    └── stores/
        └── specialists.test.ts
```

---

## Migration Strategy

### Phase 1: Foundation (Week 1-2)
1. Create TypeScript types
2. Set up Pinia stores
3. Create `useCrud` composable
4. Enhance error handling

### Phase 2: Base Components (Week 2-3)
1. Create base UI components
2. Create layout components
3. Update existing components to use base components

### Phase 3: Refactor Sections (Week 3-5)
1. Refactor `CategoriesSection` (simplest)
2. Refactor `ConnectionTypesSection`
3. Refactor `ClientsSection`
4. Refactor `SpecialistsSection` (most complex)

### Phase 4: Polish (Week 5-6)
1. Add comprehensive tests
2. Improve loading states
3. Add notifications system
4. Documentation

---

## Expected Benefits

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Largest component | 1,821 lines | ~300 lines | 83% reduction |
| Code duplication | High | Minimal | ~70% reduction |
| Type coverage | ~40% | ~95% | 55% improvement |
| Test coverage | <10% | >80% | 70% improvement |
| Build time | ~45s | ~25s | 44% faster |
| New feature dev time | 2-3 days | 0.5-1 day | 60% faster |

---

## Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking existing functionality | High | Comprehensive tests, gradual migration |
| Team learning curve | Medium | Documentation, pair programming |
| Time investment | Medium | Phase approach, prioritize high-impact changes |
| Pinia complexity | Low | Simple store patterns, examples provided |

---

## Next Steps

1. **Review and approve** this proposal
2. **Set up development branch** for refactoring
3. **Start with Phase 1** (types and stores)
4. **Create tests** alongside each refactored component
5. **Document patterns** for team reference
