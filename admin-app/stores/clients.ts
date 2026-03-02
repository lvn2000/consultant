import { defineStore } from "pinia";
import type { Client, NotificationPreference } from "~/types/api";

interface ClientsState {
  items: Client[];
  loading: boolean;
  error: string | null;
  pagination: {
    currentPage: number;
    pageSize: number;
    total: number;
  };
  searchQuery: string;
  selectedId: string | null;
  notificationsLoading: boolean;
  notificationsError: string | null;
  notifications: NotificationPreference[];
}

export const useClientsStore = defineStore("clients", {
  state: (): ClientsState => ({
    items: [],
    loading: false,
    error: null,
    pagination: {
      currentPage: 1,
      pageSize: 20,
      total: 0,
    },
    searchQuery: "",
    selectedId: null,
    notificationsLoading: false,
    notificationsError: null,
    notifications: [],
  }),

  getters: {
    filteredClients: (state) => {
      if (!state.searchQuery.trim()) return state.items;

      const query = state.searchQuery.toLowerCase().trim();
      return state.items.filter((client) => {
        return (
          client.name.toLowerCase().includes(query) ||
          client.email.toLowerCase().includes(query) ||
          (client.phone && client.phone.toLowerCase().includes(query))
        );
      });
    },

    pagedClients: (state) => {
      const start =
        (state.pagination.currentPage - 1) * state.pagination.pageSize;
      return state.filteredClients.slice(
        start,
        start + state.pagination.pageSize,
      );
    },

    totalPages: (state) => {
      return Math.max(
        1,
        Math.ceil(state.filteredClients.length / state.pagination.pageSize),
      );
    },

    isLastPage: (state, getters) => {
      return state.pagination.currentPage === getters.totalPages;
    },

    selectedClient: (state) => {
      return state.items.find((c) => c.id === state.selectedId) || null;
    },

    getClientById: (state) => {
      return (id: string) => state.items.find((c) => c.id === id) || null;
    },
  },

  actions: {
    async fetchClients() {
      this.loading = true;
      this.error = null;

      try {
        const config = useRuntimeConfig();
        const { $fetch } = useApi();

        const data = await $fetch<Client[]>(`${config.public.apiBase}/users`, {
          query: { offset: 0, limit: 1000 },
        });

        // Filter only clients
        this.items = data.filter((user) => user.role === "Client");
        this.pagination.total = this.items.length;

        if (this.pagination.currentPage > this.totalPages) {
          this.pagination.currentPage = this.totalPages;
        }
      } catch (e: any) {
        this.error = e.data?.message || "Failed to load clients";
      } finally {
        this.loading = false;
      }
    },

    async createClient(data: Partial<Client>) {
      const config = useRuntimeConfig();
      const { $fetch } = useApi();

      try {
        await $fetch<Client>(`${config.public.apiBase}/auth/register`, {
          method: "POST",
          body: {
            ...data,
            role: "Client",
            login: data.email?.split("@")[0] || "",
            password: "DefaultPassword123!",
          },
        });

        await this.fetchClients();
        return { success: true };
      } catch (e: any) {
        return {
          success: false,
          error: e.data?.message || "Failed to create client",
        };
      }
    },

    async updateClient(id: string, data: Partial<Client>) {
      const config = useRuntimeConfig();
      const { $fetch } = useApi();

      try {
        await $fetch<Client>(`${config.public.apiBase}/users/${id}`, {
          method: "PUT",
          body: data,
        });

        const index = this.items.findIndex((c) => c.id === id);
        if (index !== -1) {
          this.items[index] = { ...this.items[index], ...data };
        }

        return { success: true };
      } catch (e: any) {
        return {
          success: false,
          error: e.data?.message || "Failed to update client",
        };
      }
    },

    async deleteClient(id: string) {
      const config = useRuntimeConfig();
      const { $fetch } = useApi();

      try {
        await $fetch(`${config.public.apiBase}/users/${id}`, {
          method: "DELETE",
        });

        this.items = this.items.filter((c) => c.id !== id);

        if (this.selectedId === id) {
          this.selectedId = null;
        }

        this.pagination.total = this.items.length;

        return { success: true };
      } catch (e: any) {
        return {
          success: false,
          error: e.data?.message || "Failed to delete client",
        };
      }
    },

    async fetchNotifications(clientId: string) {
      this.notificationsLoading = true;
      this.notificationsError = null;

      try {
        const config = useRuntimeConfig();
        const { $fetch } = useApi();

        const data = await $fetch<{ preferences: NotificationPreference[] }>(
          `${config.public.apiBase}/notification-preferences/${clientId}`,
        );

        this.notifications = data.preferences || [];
      } catch (e: any) {
        this.notificationsError =
          e.data?.message || "Failed to load notifications";
      } finally {
        this.notificationsLoading = false;
      }
    },

    async updateNotification(
      clientId: string,
      preferenceId: string,
      notificationType: string,
      emailEnabled: boolean,
    ) {
      const config = useRuntimeConfig();
      const { $fetch } = useApi();

      try {
        await $fetch(
          `${config.public.apiBase}/notification-preferences/${clientId}/${notificationType}`,
          {
            method: "PUT",
            body: {
              emailEnabled,
              smsEnabled: false,
            },
          },
        );

        const index = this.notifications.findIndex(
          (n) => n.id === preferenceId,
        );
        if (index !== -1) {
          this.notifications[index].emailEnabled = emailEnabled;
        }

        return { success: true };
      } catch (e: any) {
        return {
          success: false,
          error: e.data?.message || "Failed to update notification",
        };
      }
    },

    setSelectedClient(id: string | null) {
      this.selectedId = id;
    },

    setSearchQuery(query: string) {
      this.searchQuery = query;
      this.pagination.currentPage = 1;
    },

    setPageSize(size: number) {
      this.pagination.pageSize = size;
      this.pagination.currentPage = 1;
    },

    previousPage() {
      if (this.pagination.currentPage > 1) {
        this.pagination.currentPage -= 1;
      }
    },

    nextPage() {
      if (this.pagination.currentPage < this.totalPages) {
        this.pagination.currentPage += 1;
      }
    },

    $reset() {
      this.$patch({
        items: [],
        loading: false,
        error: null,
        pagination: { currentPage: 1, pageSize: 20, total: 0 },
        searchQuery: "",
        selectedId: null,
        notificationsLoading: false,
        notificationsError: null,
        notifications: [],
      });
    },
  },
});
