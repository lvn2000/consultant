# Admin App - Quick Reference Guide

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Pages                                │
│  (index.vue, login.vue, main.vue)                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Section Components                        │
│  (SpecialistsSection, ClientsSection, etc.)                 │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│   Base Components │ │     Stores       │ │    Composables   │
│  (Button, Input,  │ │ (specialists,    │ │ (useApi, useCrud,│
│   Table, etc.)    │ │  clients, etc.)  │ │  useApiError)    │
└──────────────────┘ └──────────────────┘ └──────────────────┘
                              │               │
                              └───────┬───────┘
                                      ▼
                              ┌──────────────────┐
                              │   API Backend    │
                              └──────────────────┘
```

---

## Stores Quick Reference

### Specialists Store
```typescript
import { useSpecialistsStore } from '~/stores/specialists'

const store = useSpecialistsStore()

// State
store.items              // Specialist[]
store.loading            // boolean
store.error              // string | null
store.pagination         // { currentPage, pageSize, total }
store.searchQuery        // string
store.selectedId         // string | null

// Getters
store.filteredSpecialists    // Filtered by searchQuery
store.pagedSpecialists       // Paginated results
store.totalPages             // Calculated total pages
store.isLastPage             // boolean
store.selectedSpecialist     // Currently selected specialist

// Actions
await store.fetchSpecialists()
await store.createSpecialist(data)
await store.updateSpecialist(id, data)
await store.deleteSpecialist(id)
await store.updateCategoryRates(id, rates)
await store.fetchConnections(id)
await store.addConnection(id, data)
await store.updateConnection(id, connectionId, data)
await store.deleteConnection(id, connectionId)
store.setSelectedSpecialist(id)
store.setSearchQuery(query)
store.setPageSize(size)
store.previousPage()
store.nextPage()
```

### Clients Store
```typescript
import { useClientsStore } from '~/stores/clients'

const store = useClientsStore()

// Similar structure to specialists store
// Plus notification-specific actions:
await store.fetchNotifications(clientId)
await store.updateNotification(clientId, preferenceId, type, emailEnabled)
```

### Categories Store
```typescript
import { useCategoriesStore } from '~/stores/categories'

const store = useCategoriesStore()

// Additional getters:
store.availableParentCategories  // Exclude current selection
store.rootCategories             // Categories without parent
store.childCategories(parentId)  // Children of specific parent
store.getCategoryName(id)        // Get name by ID
```

### Connection Types Store
```typescript
import { useConnectionTypesStore } from '~/stores/connectionTypes'

const store = useConnectionTypesStore()

// Simple CRUD store (no pagination)
```

---

## Composables Quick Reference

### useApi
```typescript
const { $fetch } = useApi()

// Basic GET
const data = await $fetch<User[]>('/users')

// POST with body
const result = await $fetch<User>('/users', {
  method: 'POST',
  body: userData,
})

// With query params
const items = await $fetch<Item[]>('/items', {
  query: { offset: 0, limit: 20 },
})

// Without auth
const publicData = await $fetch('/public', {
  requiresAuth: false,
})
```

### useApiError
```typescript
const { handleApiError, translateError, getErrorMessage } = useApiError()

try {
  await someApiCall()
} catch (e: any) {
  const error = handleApiError(e)
  
  // Check error type
  if (error.isUnauthorized) {
    // Handle 401
  }
  
  if (error.isForbidden) {
    // Handle 403
  }
  
  // Get localized message
  const message = translateError(error)
  
  // Or use helper
  const msg = getErrorMessage(e)
}
```

### useCrud
```typescript
import { useCrud } from '~/composables/useCrud'

const {
  items,
  loading,
  error,
  fetchItems,
  createItem,
  updateItem,
  deleteItem,
  confirmAction,
  confirmState,
} = useCrud({
  fetchUrl: '/api/items',
  createUrl: '/api/items',
  updateUrl: (id) => `/api/items/${id}`,
  deleteUrl: (id) => `/api/items/${id}`,
  onSuccess: (action) => {
    console.log(`${action} successful`)
  },
  onError: (action, error) => {
    console.error(`${action} failed:`, error)
  },
})

// Use confirmation dialog
const confirmed = await confirmAction('Delete', 'Are you sure?')
if (confirmed) {
  await deleteItem(id)
}
```

---

## Base Components Quick Reference

### BaseButton
```vue
<BaseButton
  variant="primary"          <!-- default | primary | danger | success | warning -->
  size="medium"              <!-- small | medium | large -->
  :disabled="false"
  :loading="false"
  :full-width="false"
  @click="handleClick"
>
  Click Me
</BaseButton>
```

### BaseInput
```vue
<BaseInput
  v-model="form.name"
  id="name"
  label="Full Name"
  type="text"                <!-- text | email | password | tel | number | etc. -->
  placeholder="Enter name"
  :required="true"
  :disabled="false"
  error="Error message"
  @blur="handleBlur"
/>
```

### BaseSelect
```vue
<BaseSelect
  v-model="form.role"
  id="role"
  label="Role"
  placeholder="Select role"
  :required="true"
  :disabled="false"
  error="Error message"
>
  <option value="client">Client</option>
  <option value="specialist">Specialist</option>
</BaseSelect>
```

### BaseTable
```vue
<BaseTable
  :columns="['Name', 'Email', 'Actions']"
  :items="users"
  table-class="users-table"
  :selected-id="selectedUserId"
  empty-message="No users found"
>
  <template #row="{ item }">
    <span>{{ item.name }}</span>
    <span>{{ item.email }}</span>
    <span>
      <BaseButton @click="edit(item)">Edit</BaseButton>
    </span>
  </template>
</BaseTable>
```

### Pagination
```vue
<Pagination
  v-model:current-page="currentPage"
  v-model:page-size="pageSize"
  :total="totalItems"
/>
```

### SectionHeader
```vue
<SectionHeader
  title="Specialists"
  :show-refresh="true"
  @refresh="loadData"
>
  <template #actions>
    <BaseButton @click="addAction">Add</BaseButton>
  </template>
</SectionHeader>
```

### SearchBar
```vue
<SearchBar
  v-model="searchQuery"
  placeholder="Search..."
  @search="handleSearch"
  @clear="handleClear"
/>
```

---

## Types Quick Reference

### Specialist
```typescript
interface Specialist {
  id: string
  login: string
  email: string
  name: string
  phone?: string | null
  role: 'Specialist'
  bio?: string | null
  isAvailable: boolean
  categoryRates: SpecialistCategoryRate[]
  connections?: SpecialistConnection[]
  consultationIds?: string[]
  createdAt: string
  updatedAt: string
}
```

### Category Rate
```typescript
interface SpecialistCategoryRate {
  id?: string
  categoryId: string
  categoryName?: string
  hourlyRate: number
  experienceYears: number
}
```

### Connection
```typescript
interface SpecialistConnection {
  id: string
  specialistId: string
  connectionTypeId: string
  connectionTypeName?: string
  connectionValue: string
  isVerified: boolean
  createdAt?: string
  updatedAt?: string
}
```

---

## Common Patterns

### Fetch Data on Mount
```typescript
const store = useSpecialistsStore()

onMounted(async () => {
  await store.fetchSpecialists()
})
```

### Watch for Selection Changes
```typescript
watch(
  () => store.selectedId,
  async (newId) => {
    if (newId) {
      await store.fetchConnections(newId)
    }
  }
)
```

### Confirmation Dialog
```typescript
const handleDelete = async (id: string) => {
  const confirmed = await confirmAction(
    t('common.delete'),
    t('common.deleteConfirm')
  )
  
  if (confirmed) {
    await store.deleteSpecialist(id)
  }
}
```

### Form Submission
```typescript
const handleSubmit = async () => {
  if (!form.name.trim()) {
    showMessage('Name is required', 'error')
    return
  }
  
  const result = selectedId.value
    ? await store.updateSpecialist(selectedId.value, form.value)
    : await store.createSpecialist(form.value)
  
  if (result.success) {
    showMessage('Success!', 'success')
    resetForm()
  } else {
    showMessage(result.error || 'Failed', 'error')
  }
}
```

### Computed Properties with Store
```typescript
const filteredItems = computed(() => {
  if (!searchQuery.value.trim()) return store.items
  return store.items.filter(item =>
    item.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const pagedItems = computed(() => {
  const start = (store.pagination.currentPage - 1) * store.pagination.pageSize
  return filteredItems.value.slice(start, start + store.pagination.pageSize)
})
```

---

## Error Handling

### Try-Catch Pattern
```typescript
try {
  await store.fetchSpecialists()
} catch (e: any) {
  console.error('[Component] Fetch error:', e)
  // Error is already stored in store.error
}
```

### Result Pattern
```typescript
const result = await store.updateSpecialist(id, data)

if (result.success) {
  // Handle success
} else {
  // Handle error (result.error contains message)
  showMessage(result.error, 'error')
}
```

---

## Testing

### Store Test Example
```typescript
import { setActivePinia, createPinia } from 'pinia'
import { useSpecialistsStore } from '~/stores/specialists'

describe('Specialists Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should fetch specialists', async () => {
    const store = useSpecialistsStore()
    // Mock API call
    vi.mocked($fetch).mockResolvedValue(mockData)
    
    await store.fetchSpecialists()
    
    expect(store.items).toHaveLength(2)
    expect(store.loading).toBe(false)
  })
})
```

---

## Best Practices

1. **Always use stores** for state management, not local state
2. **Use base components** for consistent UI
3. **Type everything** - avoid `any`
4. **Handle errors** in both composables and components
5. **Show loading states** for better UX
6. **Use confirmation dialogs** for destructive actions
7. **Write tests** for stores and composables
8. **Keep components small** - extract logic to composables
9. **Use computed properties** for derived state
10. **Document complex logic** with comments

---

## Troubleshooting

### Store state not reactive
✅ Make sure to use `computed()` or `storeToRefs()`
```typescript
// Wrong
const items = store.items

// Right
const items = computed(() => store.items)
// or
const { items } = storeToRefs(store)
```

### API calls not authenticated
✅ Check if `requiresAuth` is set to false accidentally
✅ Verify session storage has token

### Types not working
✅ Ensure you're importing from `~/types/api`
✅ Check TypeScript config extends `.nuxt/tsconfig.json`

### Pinia store not initialized
✅ Make sure Pinia plugin is registered in `nuxt.config.ts`
```typescript
export default defineNuxtConfig({
  modules: ['@pinia/nuxt'],
})
```
