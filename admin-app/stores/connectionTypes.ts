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
