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
 * Generic CRUD operations composable for reusable data management
 */
import type { Ref } from "vue";

interface UseCrudOptions<T, C = Partial<T>, U = Partial<T>> {
  /** URL to fetch all items */
  fetchUrl: string;
  /** URL to create a new item */
  createUrl?: string;
  /** URL template to update an item (receives item ID) */
  updateUrl: (id: string) => string;
  /** URL template to delete an item (receives item ID) */
  deleteUrl: (id: string) => string;
  /** Optional success callback */
  onSuccess?: (action: "create" | "update" | "delete", data?: T) => void;
  /** Optional error callback */
  onError?: (action: "create" | "update" | "delete", error: any) => void;
}

interface UseCrudReturn<T, C = any, U = any> {
  // State
  items: Ref<T[]>;
  loading: Ref<boolean>;
  error: Ref<string | null>;

  // Actions
  fetchItems: () => Promise<void>;
  createItem: (data: C) => Promise<{ success: boolean; error?: string }>;
  updateItem: (
    id: string,
    data: U,
  ) => Promise<{ success: boolean; error?: string }>;
  deleteItem: (id: string) => Promise<{ success: boolean; error?: string }>;

  // Confirmation dialog
  confirmAction: (title: string, message: string) => Promise<boolean>;
  confirmState: Ref<{ visible: boolean; title: string; message: string }>;
}

export function useCrud<T = any, C = any, U = any>(
  options: UseCrudOptions<T, C, U>,
): UseCrudReturn<T, C, U> {
  const items = ref<T[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const { t } = useI18n();
  const { $fetch } = useApi();

  // Confirmation dialog state
  const confirmState = ref({ visible: false, title: "", message: "" });
  const confirmResolver = ref<((value: boolean) => void) | null>(null);

  /**
   * Show confirmation dialog and wait for user response
   */
  const confirmAction = (title: string, message: string): Promise<boolean> => {
    return new Promise((resolve) => {
      confirmState.value = { visible: true, title, message };
      confirmResolver.value = (confirmed: boolean) => {
        resolve(confirmed);
        confirmState.value.visible = false;
        confirmResolver.value = null;
      };
    });
  };

  /**
   * Fetch all items from API
   */
  const fetchItems = async () => {
    loading.value = true;
    error.value = null;

    try {
      const data = await $fetch<T[]>(options.fetchUrl);
      items.value = data;
    } catch (e: any) {
      const errorMsg = e.data?.message || t("common.failedToLoad");
      error.value = errorMsg;
      options.onError?.("create", e);
    } finally {
      loading.value = false;
    }
  };

  /**
   * Create a new item
   */
  const createItem = async (
    data: C,
  ): Promise<{ success: boolean; error?: string }> => {
    if (!options.createUrl) {
      return { success: false, error: "createUrl not provided" };
    }

    loading.value = true;

    try {
      const result = await $fetch<T>(options.createUrl, {
        method: "POST",
        body: data,
      });

      await fetchItems();
      options.onSuccess?.("create", result);

      return { success: true };
    } catch (e: any) {
      const errorMsg = e.data?.message || t("common.failedToCreate");
      error.value = errorMsg;
      options.onError?.("create", e);

      return { success: false, error: errorMsg };
    } finally {
      loading.value = false;
    }
  };

  /**
   * Update an existing item
   */
  const updateItem = async (
    id: string,
    data: U,
  ): Promise<{ success: boolean; error?: string }> => {
    loading.value = true;

    try {
      const result = await $fetch<T>(options.updateUrl(id), {
        method: "PUT",
        body: data,
      });

      // Update local state
      const index = items.value.findIndex((item: any) => item.id === id);
      if (index !== -1) {
        items.value[index] = { ...items.value[index], ...result };
      }

      options.onSuccess?.("update", result);

      return { success: true };
    } catch (e: any) {
      const errorMsg = e.data?.message || t("common.failedToUpdate");
      error.value = errorMsg;
      options.onError?.("update", e);

      return { success: false, error: errorMsg };
    } finally {
      loading.value = false;
    }
  };

  /**
   * Delete an item
   */
  const deleteItem = async (
    id: string,
  ): Promise<{ success: boolean; error?: string }> => {
    loading.value = true;

    try {
      await $fetch(options.deleteUrl(id), {
        method: "DELETE",
      });

      // Remove from local state
      items.value = items.value.filter((item: any) => item.id !== id);

      options.onSuccess?.("delete");

      return { success: true };
    } catch (e: any) {
      const errorMsg = e.data?.message || t("common.failedToDelete");
      error.value = errorMsg;
      options.onError?.("delete", e);

      return { success: false, error: errorMsg };
    } finally {
      loading.value = false;
    }
  };

  // Auto-fetch on mount
  onMounted(() => {
    fetchItems();
  });

  return {
    items,
    loading,
    error,
    fetchItems,
    createItem,
    updateItem,
    deleteItem,
    confirmAction,
    confirmState,
  };
}
