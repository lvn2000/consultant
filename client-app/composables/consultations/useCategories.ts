/**
 * Composable for fetching and managing consultation categories.
 * Provides methods to load categories and filter them as needed.
 */

import { ref } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { useApi } from '../useApi'

export interface Category {
  id: string
  name: string
  description?: string
}

export function useCategories() {
  const categories = ref<Category[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const { $fetch } = useApi()
  const config = useRuntimeConfig()

  /**
   * Loads consultation categories from the API.
   */
  const loadCategories = async () => {
    loading.value = true
    error.value = null
    try {
      const data = await $fetch<Category[]>(`${config.public.apiBase}/categories`)
      categories.value = data || []
    } catch (e: any) {
      error.value = e.message || 'Failed to load categories'
    } finally {
      loading.value = false
    }
  }

  /**
   * Filters categories by name or description.
   * @param search Search string
   */
  const filterCategories = (search: string) => {
    if (!search) return categories.value
    const lower = search.toLowerCase()
    return categories.value.filter(
      cat =>
        cat.name.toLowerCase().includes(lower) ||
        (cat.description && cat.description.toLowerCase().includes(lower))
    )
  }

  return {
    categories,
    loading,
    error,
    loadCategories,
    filterCategories,
  }
}
