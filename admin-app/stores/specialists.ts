import { defineStore } from 'pinia'
import type { Specialist, SpecialistConnection, SpecialistCategoryRate } from '~/types/api'

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
  connectionsLoading: boolean
  connectionsError: string | null
  connections: SpecialistConnection[]
}

export const useSpecialistsStore = defineStore('specialists', {
  state: (): SpecialistsState => ({
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
    connectionsLoading: false,
    connectionsError: null,
    connections: [],
  }),

  getters: {
    /**
     * Filter specialists by search query
     */
    filteredSpecialists: (state) => {
      if (!state.searchQuery.trim()) return state.items

      const query = state.searchQuery.toLowerCase().trim()
      return state.items.filter((specialist) => {
        return (
          specialist.name.toLowerCase().includes(query) ||
          specialist.email.toLowerCase().includes(query) ||
          (specialist.phone && specialist.phone.toLowerCase().includes(query))
        )
      })
    },

    /**
     * Get paginated specialists
     */
    pagedSpecialists: (state) => {
      const start = (state.pagination.currentPage - 1) * state.pagination.pageSize
      return state.filteredSpecialists.slice(start, start + state.pagination.pageSize)
    },

    /**
     * Get total pages for pagination
     */
    totalPages: (state) => {
      return Math.max(1, Math.ceil(state.filteredSpecialists.length / state.pagination.pageSize))
    },

    /**
     * Check if current page is the last page
     */
    isLastPage: (state, getters) => {
      return state.pagination.currentPage === getters.totalPages
    },

    /**
     * Get currently selected specialist
     */
    selectedSpecialist: (state) => {
      return state.items.find((s) => s.id === state.selectedId) || null
    },

    /**
     * Get specialist by ID
     */
    getSpecialistById: (state) => {
      return (id: string) => state.items.find((s) => s.id === id) || null
    },
  },

  actions: {
    /**
     * Fetch all specialists from API
     */
    async fetchSpecialists() {
      this.loading = true
      this.error = null

      try {
        const config = useRuntimeConfig()
        const { $fetch } = useApi()

        const data = await $fetch<Specialist[]>(`${config.public.apiBase}/specialists/search`, {
          query: { offset: 0, limit: 1000 },
        })

        this.items = data
        this.pagination.total = data.length

        // Reset page if out of bounds
        if (this.pagination.currentPage > this.totalPages) {
          this.pagination.currentPage = this.totalPages
        }
      } catch (e: any) {
        this.error = e.data?.message || 'Failed to load specialists'
        console.error('[SpecialistsStore] Fetch error:', e)
      } finally {
        this.loading = false
      }
    },

    /**
     * Create a new specialist
     */
    async createSpecialist(data: Partial<Specialist>) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()

      try {
        await $fetch<Specialist>(`${config.public.apiBase}/specialists`, {
          method: 'POST',
          body: data,
        })

        await this.fetchSpecialists()
        return { success: true }
      } catch (e: any) {
        console.error('[SpecialistsStore] Create error:', e)
        return {
          success: false,
          error: e.data?.message || 'Failed to create specialist',
        }
      }
    },

    /**
     * Update an existing specialist
     */
    async updateSpecialist(id: string, data: Partial<Specialist>) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()

      try {
        await $fetch<Specialist>(`${config.public.apiBase}/specialists/${id}`, {
          method: 'PUT',
          body: data,
        })

        // Update local state
        const index = this.items.findIndex((s) => s.id === id)
        if (index !== -1) {
          this.items[index] = { ...this.items[index], ...data }
        }

        return { success: true }
      } catch (e: any) {
        console.error('[SpecialistsStore] Update error:', e)
        return {
          success: false,
          error: e.data?.message || 'Failed to update specialist',
        }
      }
    },

    /**
     * Delete a specialist
     */
    async deleteSpecialist(id: string) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()

      try {
        await $fetch(`${config.public.apiBase}/specialists/${id}`, {
          method: 'DELETE',
        })

        // Remove from local state
        this.items = this.items.filter((s) => s.id !== id)

        // Clear selection if deleted
        if (this.selectedId === id) {
          this.selectedId = null
        }

        this.pagination.total = this.items.length

        return { success: true }
      } catch (e: any) {
        console.error('[SpecialistsStore] Delete error:', e)
        return {
          success: false,
          error: e.data?.message || 'Failed to delete specialist',
        }
      }
    },

    /**
     * Update specialist category rates
     */
    async updateCategoryRates(id: string, rates: SpecialistCategoryRate[]) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()

      try {
        await $fetch(`${config.public.apiBase}/specialists/${id}/category-rates`, {
          method: 'PUT',
          body: { categoryRates: rates },
        })

        // Update local state
        const index = this.items.findIndex((s) => s.id === id)
        if (index !== -1) {
          this.items[index].categoryRates = rates
        }

        return { success: true }
      } catch (e: any) {
        console.error('[SpecialistsStore] Update rates error:', e)
        return {
          success: false,
          error: e.data?.message || 'Failed to update category rates',
        }
      }
    },

    /**
     * Fetch specialist connections
     */
    async fetchConnections(specialistId: string) {
      this.connectionsLoading = true
      this.connectionsError = null

      try {
        const config = useRuntimeConfig()
        const { $fetch } = useApi()

        const data = await $fetch<SpecialistConnection[]>(
          `${config.public.apiBase}/specialists/${specialistId}/connections`
        )

        this.connections = data
      } catch (e: any) {
        this.connectionsError = e.data?.message || 'Failed to load connections'
        console.error('[SpecialistsStore] Fetch connections error:', e)
      } finally {
        this.connectionsLoading = false
      }
    },

    /**
     * Add a connection to a specialist
     */
    async addConnection(specialistId: string, data: Omit<SpecialistConnection, 'id' | 'specialistId'>) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()

      try {
        const newConnection = await $fetch<SpecialistConnection>(
          `${config.public.apiBase}/specialists/${specialistId}/connections`,
          {
            method: 'POST',
            body: data,
          }
        )

        this.connections.push(newConnection)

        // Update specialist in main list
        const specialist = this.items.find((s) => s.id === specialistId)
        if (specialist && !specialist.connections) {
          specialist.connections = []
        }
        if (specialist?.connections) {
          specialist.connections.push(newConnection)
        }

        return { success: true, data: newConnection }
      } catch (e: any) {
        console.error('[SpecialistsStore] Add connection error:', e)
        return {
          success: false,
          error: e.data?.message || 'Failed to add connection',
        }
      }
    },

    /**
     * Update a specialist connection
     */
    async updateConnection(
      specialistId: string,
      connectionId: string,
      data: Partial<SpecialistConnection>
    ) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()

      try {
        await $fetch(
          `${config.public.apiBase}/specialists/${specialistId}/connections/${connectionId}`,
          {
            method: 'PUT',
            body: data,
          }
        )

        // Update local state
        const index = this.connections.findIndex((c) => c.id === connectionId)
        if (index !== -1) {
          this.connections[index] = { ...this.connections[index], ...data }
        }

        return { success: true }
      } catch (e: any) {
        console.error('[SpecialistsStore] Update connection error:', e)
        return {
          success: false,
          error: e.data?.message || 'Failed to update connection',
        }
      }
    },

    /**
     * Delete a specialist connection
     */
    async deleteConnection(specialistId: string, connectionId: string) {
      const config = useRuntimeConfig()
      const { $fetch } = useApi()

      try {
        await $fetch(
          `${config.public.apiBase}/specialists/${specialistId}/connections/${connectionId}`,
          {
            method: 'DELETE',
          }
        )

        // Remove from local state
        this.connections = this.connections.filter((c) => c.id !== connectionId)

        // Update specialist in main list
        const specialist = this.items.find((s) => s.id === specialistId)
        if (specialist?.connections) {
          specialist.connections = specialist.connections.filter((c) => c.id !== connectionId)
        }

        return { success: true }
      } catch (e: any) {
        console.error('[SpecialistsStore] Delete connection error:', e)
        return {
          success: false,
          error: e.data?.message || 'Failed to delete connection',
        }
      }
    },

    /**
     * Set the currently selected specialist
     */
    setSelectedSpecialist(id: string | null) {
      this.selectedId = id
    },

    /**
     * Set search query and reset pagination
     */
    setSearchQuery(query: string) {
      this.searchQuery = query
      this.pagination.currentPage = 1
    },

    /**
     * Set page size and reset to first page
     */
    setPageSize(size: number) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
    },

    /**
     * Go to previous page
     */
    previousPage() {
      if (this.pagination.currentPage > 1) {
        this.pagination.currentPage -= 1
      }
    },

    /**
     * Go to next page
     */
    nextPage() {
      if (this.pagination.currentPage < this.totalPages) {
        this.pagination.currentPage += 1
      }
    },

    /**
     * Clear all state
     */
    $reset() {
      this.$patch({
        items: [],
        loading: false,
        error: null,
        pagination: { currentPage: 1, pageSize: 20, total: 0 },
        searchQuery: '',
        selectedId: null,
        connectionsLoading: false,
        connectionsError: null,
        connections: [],
      })
    },
  },
})
