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

import { defineStore } from "pinia";
import type { ConnectionType } from "~/types/api";

interface ConnectionTypesState {
  items: ConnectionType[];
  loading: boolean;
  error: string | null;
  selectedId: string | null;
}

export const useConnectionTypesStore = defineStore("connectionTypes", {
  state: (): ConnectionTypesState => ({
    items: [],
    loading: false,
    error: null,
    selectedId: null,
  }),

  getters: {
    connectionTypeById: (state) => {
      return (id: string) => state.items.find((t) => t.id === id) || null;
    },

    connectionTypeName: (state) => {
      return (id: string) => state.items.find((t) => t.id === id)?.name ?? id;
    },

    selectedConnectionType: (state) => {
      return state.items.find((t) => t.id === state.selectedId) || null;
    },
  },

  actions: {
    async fetchConnectionTypes() {
      this.loading = true;
      this.error = null;

      try {
        const config = useRuntimeConfig();
        const { $fetch } = useApi();

        const data = await $fetch<ConnectionType[]>(
          `${config.public.apiBase}/connection-types`,
        );

        this.items = data;
      } catch (e: any) {
        this.error = e.data?.message || "Failed to load connection types";
      } finally {
        this.loading = false;
      }
    },

    async createConnectionType(data: Omit<ConnectionType, "id">) {
      const config = useRuntimeConfig();
      const { $fetch } = useApi();

      try {
        await $fetch<ConnectionType>(
          `${config.public.apiBase}/connection-types`,
          {
            method: "POST",
            body: data,
          },
        );

        await this.fetchConnectionTypes();
        return { success: true };
      } catch (e: any) {
        return {
          success: false,
          error: e.data?.message || "Failed to create connection type",
        };
      }
    },

    async updateConnectionType(id: string, data: Partial<ConnectionType>) {
      const config = useRuntimeConfig();
      const { $fetch } = useApi();

      try {
        await $fetch<ConnectionType>(
          `${config.public.apiBase}/connection-types/${id}`,
          {
            method: "PUT",
            body: data,
          },
        );

        const index = this.items.findIndex((t) => t.id === id);
        if (index !== -1) {
          this.items[index] = { ...this.items[index], ...data };
        }

        return { success: true };
      } catch (e: any) {
        return {
          success: false,
          error: e.data?.message || "Failed to update connection type",
        };
      }
    },

    async deleteConnectionType(id: string) {
      const config = useRuntimeConfig();
      const { $fetch } = useApi();

      try {
        await $fetch(`${config.public.apiBase}/connection-types/${id}`, {
          method: "DELETE",
        });

        this.items = this.items.filter((t) => t.id !== id);

        if (this.selectedId === id) {
          this.selectedId = null;
        }

        return { success: true };
      } catch (e: any) {
        return {
          success: false,
          error: e.data?.message || "Failed to delete connection type",
        };
      }
    },

    setSelectedConnectionType(id: string | null) {
      this.selectedId = id;
    },

    $reset() {
      this.$patch({
        items: [],
        loading: false,
        error: null,
        selectedId: null,
      });
    },
  },
});
