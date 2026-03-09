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

import { ref, computed, type Ref } from "vue";

export interface PaginationOptions {
  itemsPerPage?: number;
  initialPage?: number;
}

export interface PaginationResult<T> {
  currentPage: Ref<number>;
  totalPages: Ref<number>;
  totalCount: Ref<number>;
  paginatedItems: Ref<T[]>;
  goToPage: (page: number) => void;
  nextPage: () => void;
  prevPage: () => void;
  setPage: (page: number) => void;
}

export function usePagination<T>(
  items: Ref<T[]>,
  options: PaginationOptions = {},
): PaginationResult<T> {
  const { itemsPerPage = 10, initialPage = 1 } = options;
  const currentPage = ref(initialPage);

  const totalPages = computed(() => {
    const total = Math.ceil(items.value.length / itemsPerPage);
    return total || 1;
  });

  const totalCount = computed(() => items.value.length);

  const paginatedItems = computed(() => {
    const start = (currentPage.value - 1) * itemsPerPage;
    const end = start + itemsPerPage;
    return items.value.slice(start, end);
  });

  const goToPage = (page: number) => {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page;
    }
  };

  const nextPage = () => {
    goToPage(currentPage.value + 1);
  };

  const prevPage = () => {
    goToPage(currentPage.value - 1);
  };

  const setPage = (page: number) => {
    currentPage.value = page;
  };

  return {
    currentPage,
    totalPages,
    totalCount,
    paginatedItems,
    goToPage,
    nextPage,
    prevPage,
    setPage,
  };
}
