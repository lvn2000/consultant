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
 * Composable for fetching and filtering specialists for consultations.
 * Provides methods to load specialists from the API and filter them by search or category.
 */

import { ref } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "../useApi";

export interface Specialist {
  id: string;
  name: string;
  email: string;
  bio?: string;
  categories?: string[];
  status?: string;
}

export function useSpecialists() {
  const config = useRuntimeConfig();
  const { $fetch } = useApi();

  const specialists = ref<Specialist[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  /**
   * Load specialists from the API.
   */
  async function loadSpecialists() {
    loading.value = true;
    error.value = null;
    try {
      const url = `${config.public.apiBase}/specialists/search?offset=0&limit=1000`;
      const response = await $fetch<Specialist[]>(url);
      specialists.value = response || [];
    } catch (e: any) {
      error.value = e.message || "Failed to load specialists";
    } finally {
      loading.value = false;
    }
  }

  /**
   * Filter specialists by search text.
   */
  function filterSpecialists(search: string): Specialist[] {
    if (!search) return specialists.value;
    const lower = search.toLowerCase();
    return specialists.value.filter(
      (s) =>
        s.name?.toLowerCase().includes(lower) ||
        s.email?.toLowerCase().includes(lower) ||
        s.bio?.toLowerCase().includes(lower),
    );
  }

  /**
   * Filter specialists by category.
   */
  function filterByCategory(categoryId: string): Specialist[] {
    if (!categoryId) return specialists.value;
    return specialists.value.filter((s) => s.categories?.includes(categoryId));
  }

  return {
    specialists,
    loading,
    error,
    loadSpecialists,
    filterSpecialists,
    filterByCategory,
  };
}
