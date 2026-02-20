import { defineStore } from 'pinia'
import type { Category } from '~/types/api'

interface CategoriesState {
  items: Category[]
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

export const useCategoriesStore = defineStore('categories', {
  state: (): CategoriesState => ({
    items: [],
    loading: false,
    error: null,
    pagination: {
      currentPage: 1,
      pageSize: 20,
      total: 0,
    },
    searchQuery: '',
    selectedId: null,
  }),

  getters: {
    filteredCategories: (state) => {
      if (!state.searchQuery.trim()) return state.items

      const query = state.searchQuery.toLowerCase().trim()
      return state.items.filter((category) => {
        return (
          category.name.toLowerCase().includes(query) ||
          (category.description && category.description.toLowerCase().includes(query))
        )
      })
    },

    pagedCategories: (state) => {
      const start = (state.pagination.currentPage - 1) * state.pagination.pageSize
      return state.filteredCategories.slice(start, start + state.pagination.pageSize)
    },

    totalPages: (state) => {
      return Math.max(1, Math.ceil(state.filteredCategories.length / state.pagination.pageSize))
    },

    isLastPage: (state, getters) => {
      return state.pagination.currentPage === getters.totalPages
    },

    selectedCategory: (state) => {
      return state.items.find((c) => c.id === state.selectedId) || null
    },

    availableParentCategories: (state) => {
      // Filter out current category to prevent self-reference
      return state.items.filter((cat) => cat.id !== state.selectedId)
    },

    getCategoryById: (state) => {
      return (id: string) => state.items.find((c) => c.id === id) || null
    },

    getCategoryName: (state) => {
      return (id: string | null) => {
        if (!id) return '-'
        return state.items.find((c) => c.id === id)?.name ?? id
      }
    },

    rootCategories: (state) => {
      return state.items.filter((c) => !c.parentId)
    },

    childCategories: (state) => {
      return (parentId: string) => state.items.filter((c) => c.parentId === parentId)
    },
  },

  actions: {
    async fetchCategories() {
      this.loading = true
      this.error = null

      try {
        const config = useRuntimeConfig()
        const { $fetch } = useApi()

        const data = await $fetch<Category[]>(`${config.public.apiBase}/categories`)

        this.items = data
        this.pagination.total = data.length

        if (this.pagination.currentPage > this.totalPages) {
          this.pagination.currentPage = this.totalPages
        }
      } catch (e: any) {
        this.error = e.data?.message || 'Failed to load categories'
        console.error('[CategoriesStore] Fetch error:', e)
      } finally {
        this.loading = false
      }
    },

    async createCategory(data: Omit<Category, 'id'>) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()

      try {
        await $fetch<Category>(`${config.public.apiBase}/categories`, {
          method: 'POST',
          body: data,
        })

        await this.fetchCategories()
        return { success: true }
      } catch (e: any) {
        console.error('[CategoriesStore] Create error:', e)
        return {
          success: false,
          error: e.data?.message || 'Failed to create category',
        }
      }
    },

    async updateCategory(id: string, data: Partial<Category>) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()

      try {
        await $fetch<Category>(`${config.public.apiBase}/categories/${id}`, {
          method: 'PUT',
          body: data,
        })

        const index = this.items.findIndex((c) => c.id === id)
        if (index !== -1) {
          this.items[index] = { ...this.items[index], ...data }
        }

        return { success: true }
      } catch (e: any) {
        console.error('[CategoriesStore] Update error:', e)
        return {
          success: false,
          error: e.data?.message || 'Failed to update category',
        }
      }
    },

    async deleteCategory(id: string) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()

      try {
        await $fetch(`${config.public.apiBase}/categories/${id}`, {
          method: 'DELETE',
        })

        this.items = this.items.filter((c) => c.id !== id)

        if (this.selectedId === id) {
          this.selectedId = null
        }

        this.pagination.total = this.items.length

        return { success: true }
      } catch (e: any) {
        console.error('[CategoriesStore] Delete error:', e)
        return {
          success: false,
          error: e.data?.message || 'Failed to delete category',
        }
      }
    },

    setSelectedCategory(id: string | null) {
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

    previousPage() {
      if (this.pagination.currentPage > 1) {
        this.pagination.currentPage -= 1
      }
    },

    nextPage() {
      if (this.pagination.currentPage < this.totalPages) {
        this.pagination.currentPage += 1
      }
    },

    $reset() {
      this.$patch({
        items: [],
        loading: false,
        error: null,
        pagination: { currentPage: 1, pageSize: 20, total: 0 },
        searchQuery: '',
        selectedId: null,
      })
    },
  },
})
