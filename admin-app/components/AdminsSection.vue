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

<template>
    <section class="section">
        <div v-if="visible">
            <div class="section-header">
                <h2>{{ $t("adminAdmins.title") }}</h2>
                <button type="button" class="btn" @click="loadAdmins">
                    🔄 Refresh
                </button>
            </div>

            <div class="search-bar">
                <input
                    v-model="adminsSearchQuery"
                    type="text"
                    :placeholder="$t('adminAdmins.searchPlaceholder')"
                    class="search-input"
                />
                <button
                    type="button"
                    class="btn"
                    @click="clearAdminsSearch"
                    v-if="adminsSearchQuery"
                >
                    ❌ Clear
                </button>
            </div>

            <div class="list-state" v-if="adminsLoading">
                {{ $t("adminAdmins.loading") }}
            </div>
            <div class="list-state error" v-else-if="adminsError">
                {{ adminsError }}
            </div>

            <div class="table" v-else>
                <div class="table-header admins-table">
                    <span>{{ $t("common.name") }}</span>
                    <span>{{ $t("common.email") }}</span>
                    <span>{{ $t("common.login") }}</span>
                    <span>{{ $t("common.actions") }}</span>
                </div>
                <div
                    v-for="admin in pagedFilteredAdmins"
                    :key="admin.id"
                    class="table-row admins-table"
                >
                    <span>{{ admin.name }}</span>
                    <span>{{ admin.email }}</span>
                    <span>{{ admin.login }}</span>
                    <span class="row-actions">
                        <button
                            type="button"
                            class="btn"
                            @click="selectAdmin(admin)"
                        >
                            ✏️ {{ $t("common.select") }}
                        </button>
                    </span>
                </div>
            </div>

            <div v-if="!adminsLoading && !adminsError" class="pagination">
                <div class="pagination-info">Page {{ adminCurrentPage }}</div>
                <div class="pagination-controls">
                    <button
                        type="button"
                        class="btn"
                        :disabled="adminCurrentPage === 1"
                        @click="goToPreviousAdminPage"
                    >
                        ⬅️ Previous
                    </button>
                    <button
                        type="button"
                        class="btn"
                        :disabled="isLastAdminPage"
                        @click="goToNextAdminPage"
                    >
                        Next ➡️
                    </button>
                </div>
                <div class="pagination-size">
                    <label for="admin-page-size">Page size</label>
                    <select
                        id="admin-page-size"
                        v-model.number="adminPageSize"
                        @change="handleAdminPageSizeChange"
                    >
                        <option :value="10">10</option>
                        <option :value="20">20</option>
                        <option :value="50">50</option>
                    </select>
                </div>
            </div>

            <form ref="adminFormRef" class="form" @submit.prevent>
                <div class="form-header" v-if="selectedAdminId">
                    <h3>{{ $t("adminAdmins.adminDetails") }}</h3>
                    <span class="form-subtitle" v-if="adminForm.name">{{
                        adminForm.name
                    }}</span>
                </div>

                <div class="form-grid" v-if="selectedAdminId">
                    <div class="form-field">
                        <label for="admin-name">{{ $t("common.name") }}</label>
                        <input
                            id="admin-name"
                            v-model="adminForm.name"
                            type="text"
                            :placeholder="$t('adminAdmins.namePlaceholder')"
                            disabled
                        />
                    </div>
                    <div class="form-field">
                        <label for="admin-email">{{
                            $t("common.email")
                        }}</label>
                        <input
                            id="admin-email"
                            v-model="adminForm.email"
                            type="email"
                            :placeholder="$t('adminAdmins.emailPlaceholder')"
                            disabled
                        />
                    </div>
                    <div class="form-field">
                        <label for="admin-login">{{
                            $t("common.login")
                        }}</label>
                        <input
                            id="admin-login"
                            v-model="adminForm.login"
                            type="text"
                            disabled
                        />
                    </div>
                </div>

                <div class="form-actions">
                    <button
                        type="button"
                        class="btn danger"
                        :disabled="isDeleteDisabled"
                        @click="deleteSelectedAdmin"
                        :title="getDeleteButtonTitle()"
                    >
                        🗑️ {{ $t("adminAdmins.deleteAdmin") }}
                    </button>
                    <button type="button" class="btn" @click="resetAdminForm">
                        ❌ Clear
                    </button>
                </div>

                <p
                    v-if="adminActionMessage"
                    :class="[
                        'form-message',
                        adminActionSuccess ? 'success' : 'error',
                    ]"
                >
                    {{ adminActionMessage }}
                </p>

                <!-- Attention message for current user -->
                <div
                    v-if="selectedAdminId === currentUserId"
                    class="attention-message"
                >
                    ⚠️ {{ $t("adminAdmins.deletingSelfWarning") }}
                </div>
            </form>

            <!-- Confirmation Dialog -->
            <div
                v-if="confirmState.visible"
                class="modal-overlay"
                @click.self="confirmResolver?.(false)"
            >
                <div class="modal-dialog">
                    <h3>{{ confirmState.title }}</h3>
                    <p>{{ confirmState.message }}</p>
                    <div class="modal-actions">
                        <button
                            type="button"
                            class="btn"
                            @click="confirmResolver?.(false)"
                        >
                            {{ $t("common.cancel") }}
                        </button>
                        <button
                            type="button"
                            class="btn danger"
                            @click="confirmResolver?.(true)"
                        >
                            {{ $t("common.confirm") }}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, nextTick, watch, watchEffect } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useRouter } from "vue-router";
import { useApi } from "../composables/useApi";

const props = defineProps<{ visible: boolean }>();

const config = useRuntimeConfig();
const { $fetch } = useApi();
const { t } = useI18n();
const router = useRouter();

type Admin = {
    id: string;
    name: string;
    email: string;
    login: string;
};

const admins = ref<Admin[]>([]);
const adminsLoading = ref(false);
const adminsError = ref("");
const selectedAdminId = ref<string | null>(null);
const adminActionMessage = ref("");
const adminActionSuccess = ref(false);
const adminCurrentPage = ref(1);
const adminPageSize = ref(20);
const adminsSearchQuery = ref("");
const adminFormRef = ref<HTMLFormElement | null>(null);
const adminCount = ref(1);
const currentUserId = ref<string | null>(null);

const adminForm = ref({
    name: "",
    email: "",
    login: "",
});

const confirmState = ref({
    visible: false,
    title: "",
    message: "",
});
const confirmResolver = ref<((value: boolean) => void) | null>(null);

const adminTotalPages = computed(() =>
    Math.max(1, Math.ceil(filteredAdmins.value.length / adminPageSize.value)),
);
const isLastAdminPage = computed(
    () => adminCurrentPage.value === adminTotalPages.value,
);

const isDeleteDisabled = computed(() => {
    const disabled = !selectedAdminId.value || adminCount.value <= 1;
    return disabled;
});

const filteredAdmins = computed(() => {
    if (!adminsSearchQuery.value.trim()) {
        return admins.value;
    }

    const query = adminsSearchQuery.value.toLowerCase().trim();
    return admins.value.filter((admin) => {
        return (
            admin.name.toLowerCase().includes(query) ||
            admin.email.toLowerCase().includes(query) ||
            admin.login.toLowerCase().includes(query)
        );
    });
});

const pagedFilteredAdmins = computed(() => {
    const start = (adminCurrentPage.value - 1) * adminPageSize.value;
    return filteredAdmins.value.slice(start, start + adminPageSize.value);
});

const confirmAction = (title: string, message: string) =>
    new Promise<boolean>((resolve) => {
        confirmState.value = { visible: true, title, message };
        confirmResolver.value = (confirmed: boolean) => {
            resolve(confirmed);
            confirmState.value.visible = false;
        };
    });

const loadAdminCount = async () => {
    try {
        const endpoint = `${config.public.apiBase}/admin-count`;
        const rawData = await $fetch(endpoint, { method: "GET" });
        const data = rawData as { count: number };
        adminCount.value = data?.count ?? 1;
    } catch (error) {
        console.error("Failed to load admin count:", error);
        adminCount.value = 1;
    }
};

const loadAdmins = async () => {
    adminsLoading.value = true;
    adminsError.value = "";
    currentUserId.value = sessionStorage.getItem("userId");

    try {
        const data = await $fetch<Admin[]>(
            `${config.public.apiBase}/users?offset=0&limit=1000`,
            {
                method: "GET",
            },
        );

        // Filter only admins (case-insensitive)
        admins.value = data
            .filter((user: any) => user.role?.toUpperCase() === "ADMIN")
            .map((user: any) => ({
                id: user.id,
                name: user.name || "",
                email: user.email || "",
                login: user.login || "",
            }));

        await loadAdminCount();

        if (adminCurrentPage.value > adminTotalPages.value) {
            adminCurrentPage.value = adminTotalPages.value;
        }
    } catch (error) {
        admins.value = [];
        adminsError.value = t("adminAdmins.failedToLoad");
    } finally {
        adminsLoading.value = false;
    }
};

const clearAdminsSearch = () => {
    adminsSearchQuery.value = "";
    adminCurrentPage.value = 1;
};

const goToPreviousAdminPage = () => {
    if (adminCurrentPage.value > 1) {
        adminCurrentPage.value -= 1;
    }
};

const goToNextAdminPage = () => {
    if (adminCurrentPage.value < adminTotalPages.value) {
        adminCurrentPage.value += 1;
    }
};

const handleAdminPageSizeChange = () => {
    adminCurrentPage.value = 1;
};

const selectAdmin = (admin: Admin) => {
    selectedAdminId.value = admin.id;
    adminForm.value = {
        name: admin.name,
        email: admin.email,
        login: admin.login,
    };
    adminActionMessage.value = "";
    adminActionSuccess.value = false;

    nextTick(() => {
        if (adminFormRef.value) {
            adminFormRef.value.scrollIntoView({
                behavior: "smooth",
                block: "start",
            });
        }
    });
};

const resetAdminForm = () => {
    selectedAdminId.value = null;
    adminForm.value = {
        name: "",
        email: "",
        login: "",
    };
    adminActionMessage.value = "";
    adminActionSuccess.value = false;
};

const getDeleteButtonTitle = () => {
    if (adminCount.value <= 1) {
        return t("adminAdmins.cannotDeleteLast");
    }
    return "";
};

const isDeletingSelf = computed(() => {
    return selectedAdminId.value === currentUserId.value;
});

const removeAdmin = async (admin: Admin) => {
    if (adminCount.value <= 1) {
        adminActionMessage.value = t("adminAdmins.cannotDeleteLast");
        adminActionSuccess.value = false;
        return;
    }

    const isSelf = admin.id === currentUserId.value;
    const confirmed = await confirmAction(
        t("adminAdmins.deleteAdmin"),
        isSelf
            ? t("adminAdmins.deleteSelfConfirm")
            : t("adminAdmins.deleteConfirm", { name: admin.name }),
    );
    if (!confirmed) return;

    try {
        adminActionMessage.value = "";
        adminActionSuccess.value = false;

        await $fetch(`${config.public.apiBase}/users/${admin.id}`, {
            method: "DELETE",
        });

        adminActionMessage.value = t("adminAdmins.deleted");
        adminActionSuccess.value = true;

        if (selectedAdminId.value === admin.id) {
            resetAdminForm();
        }

        // If deleting own account, perform logout
        if (isSelf) {
            await performLogout();
        } else {
            await loadAdmins();
        }
    } catch (error: any) {
        adminActionMessage.value =
            error?.data?.message || t("adminAdmins.failedToDelete");
        adminActionSuccess.value = false;
    }
};

const deleteSelectedAdmin = async () => {
    if (!selectedAdminId.value) return;

    const admin = admins.value.find((a) => a.id === selectedAdminId.value);
    if (admin) {
        await removeAdmin(admin);
    }
};

const performLogout = async () => {
    try {
        const sessionId = sessionStorage.getItem("sessionId");
        if (sessionId) {
            await $fetch(`${config.public.apiBase}/users/logout`, {
                method: "POST",
                body: { sessionId },
            });
        }
    } catch (error) {
        console.error("Logout error:", error);
    } finally {
        sessionStorage.removeItem("accessToken");
        sessionStorage.removeItem("sessionId");
        sessionStorage.removeItem("userId");
        sessionStorage.removeItem("login");
        sessionStorage.removeItem("email");
        sessionStorage.removeItem("role");
        localStorage.removeItem("admin_session");
        router.push("/login");
    }
};

// Watch for visibility changes and reload data
watchEffect(() => {
    if (props.visible) {
        loadAdmins();
    }
});

onMounted(() => {
    if (props.visible) {
        loadAdmins();
    }
});
</script>

<style scoped>
.section {
    margin-top: 1.25rem;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: 1rem;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
}

.section-header h2 {
    margin: 0;
    font-size: 1.25rem;
    color: #111827;
}

.search-bar {
    display: flex;
    gap: 0.5rem;
    margin-bottom: 1rem;
}

.search-input {
    flex: 1;
    padding: 0.5rem 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.9rem;
}

.list-state {
    padding: 2rem;
    text-align: center;
    color: #6b7280;
}

.list-state.error {
    color: #dc2626;
    background: #fee2e2;
    border-radius: 6px;
}

.table {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.table-header {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr auto;
    gap: 1rem;
    padding: 0.75rem;
    background: #f9fafb;
    border-radius: 6px;
    font-weight: 600;
    font-size: 0.85rem;
    color: #374151;
}

.table-row {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr auto;
    gap: 1rem;
    padding: 0.75rem;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    align-items: center;
    font-size: 0.9rem;
}

.row-actions {
    display: flex;
    gap: 0.5rem;
}

.pagination {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 1rem;
    padding-top: 1rem;
    border-top: 1px solid #e5e7eb;
    flex-wrap: wrap;
    gap: 1rem;
}

.pagination-info {
    font-size: 0.85rem;
    color: #6b7280;
}

.pagination-controls {
    display: flex;
    gap: 0.5rem;
}

.pagination-size {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.85rem;
    color: #6b7280;
}

.pagination-size select {
    padding: 0.25rem 0.5rem;
    border: 1px solid #d1d5db;
    border-radius: 4px;
}

.form {
    margin-top: 1.5rem;
    padding: 1rem;
    background: #f9fafb;
    border-radius: 8px;
}

.form-header {
    margin-bottom: 1rem;
    padding-bottom: 0.5rem;
    border-bottom: 1px solid #e5e7eb;
}

.form-header h3 {
    margin: 0 0 0.25rem;
    font-size: 1rem;
    color: #111827;
}

.form-subtitle {
    font-size: 0.85rem;
    color: #6b7280;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 0.75rem;
    margin-bottom: 1rem;
}

.form-field {
    display: flex;
    flex-direction: column;
}

.form-field label {
    font-size: 0.85rem;
    color: #374151;
    margin-bottom: 0.25rem;
    font-weight: 500;
}

.form-field input {
    padding: 0.5rem 0.6rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.9rem;
}

.form-field input:disabled {
    background: #f3f4f6;
    cursor: not-allowed;
    color: #9ca3af;
}

.form-actions {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.btn {
    padding: 0.5rem 1rem;
    border-radius: 6px;
    border: 1px solid #d1d5db;
    background: #ffffff;
    color: #1f2937;
    cursor: pointer;
    font-weight: 500;
    font-size: 0.9rem;
    transition: all 0.2s;
}

.btn:hover:not(:disabled) {
    background: #f3f4f6;
    border-color: #9ca3af;
}

.btn.danger {
    background: #fee2e2;
    color: #b91c1c;
    border-color: #fecaca;
}

.btn.danger:hover:not(:disabled) {
    background: #fca5a5;
    border-color: #f87171;
}

.btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.form-message {
    margin-top: 1rem;
    padding: 0.75rem;
    border-radius: 6px;
    font-size: 0.9rem;
    font-weight: 500;
}

.form-message.success {
    background: #dcfce7;
    color: #166534;
}

.form-message.error {
    background: #fee2e2;
    color: #b91c1c;
}

.attention-message {
    margin-top: 1rem;
    padding: 0.75rem;
    border-radius: 6px;
    background: #fef3c7;
    color: #92400e;
    border: 1px solid #fcd34d;
    font-size: 0.9rem;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.45);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 50;
}

.modal-dialog {
    background: #ffffff;
    padding: 1.5rem;
    border-radius: 12px;
    width: min(420px, 90vw);
    box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
}

.modal-dialog h3 {
    margin: 0 0 0.75rem;
    font-size: 1.1rem;
    color: #111827;
}

.modal-dialog p {
    margin: 0 0 1.25rem;
    color: #374151;
}

.modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
}

.btn.primary {
    background: #2563eb;
    color: #ffffff;
    border-color: #1d4ed8;
}

.btn.primary:hover:not(:disabled) {
    background: #1d4ed8;
    border-color: #1e40af;
}
</style>
