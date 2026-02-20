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
