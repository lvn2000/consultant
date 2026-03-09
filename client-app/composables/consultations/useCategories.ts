/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
