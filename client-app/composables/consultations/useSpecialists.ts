/**
 * Composable for fetching and filtering specialists for consultations.
 * Provides methods to load specialists from the API and filter them by search or category.
 */

import { ref } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { useApi } from '../useApi'

export interface Specialist {
  id: string
  name: string
  email: string
  bio?: string
  categories?: string[]
  status?: string
}

export function useSpecialists() {
  const config = useRuntimeConfig()
  const { $fetch } = useApi()

  const specialists = ref<Specialist[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  /**
   * Load specialists from the API.
   */
  async function loadSpecialists() {
    loading.value = true
    error.value = null
    try {
      const url = `${config.public.apiBase}/specialists`
      const response = await $fetch<Specialist[]>(url)
      specialists.value = response || []
    } catch (e: any) {
      error.value = e.message || 'Failed to load specialists'
    } finally {
      loading.value = false
    }
  }

  /**
   * Filter specialists by search text.
   */
  function filterSpecialists(search: string): Specialist[] {
    if (!search) return specialists.value
    const lower = search.toLowerCase()
    return specialists.value.filter(
      s =>
        s.name?.toLowerCase().includes(lower) ||
        s.email?.toLowerCase().includes(lower) ||
        s.bio?.toLowerCase().includes(lower)
    )
  }

  /**
   * Filter specialists by category.
   */
  function filterByCategory(categoryId: string): Specialist[] {
    if (!categoryId) return specialists.value
    return specialists.value.filter(
      s => s.categories?.includes(categoryId)
    )
  }

  return {
    specialists,
    loading,
    error,
    loadSpecialists,
    filterSpecialists,
    filterByCategory,
  }
}
