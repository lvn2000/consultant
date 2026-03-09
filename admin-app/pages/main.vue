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
    <div class="main-container">
        <nav class="menu-panel">
            <div class="menu-header">
                <div class="menu-title">{{ $t("admin.menu.title") }}</div>
                <LocaleSwitcher />
            </div>
            <ul>
                <li
                    :class="{ active: selectedMenu === 'accounts' }"
                    @click.prevent="selectMenu('accounts')"
                >
                    {{ $t("admin.menu.createAccounts") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'admins' }"
                    @click.prevent="selectMenu('admins')"
                >
                    {{ $t("admin.menu.admins") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'specialists' }"
                    @click.prevent="selectMenu('specialists')"
                >
                    {{ $t("admin.menu.specialists") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'clients' }"
                    @click.prevent="selectMenu('clients')"
                >
                    {{ $t("admin.menu.clients") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'connections' }"
                    @click.prevent="selectMenu('connections')"
                >
                    {{ $t("admin.menu.connectionTypes") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'categories' }"
                    @click.prevent="selectMenu('categories')"
                >
                    {{ $t("admin.menu.categories") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'settings' }"
                    @click.prevent="selectMenu('settings')"
                >
                    ⚙️ {{ $t("admin.menu.settings") }}
                </li>
            </ul>
            <div class="menu-divider"></div>
            <ul>
                <li class="logout" @click="logout">
                    {{ $t("common.logout") }}
                </li>
            </ul>
        </nav>
        <div class="content">
            <!-- Statistics Dashboard (show on accounts view) -->
            <div v-if="selectedMenu === 'accounts'" class="stats-grid">
                <h1>{{ $t("admin.welcome.title") }}</h1>
                <div class="stat-card">
                    <div class="stat-icon">👥</div>
                    <div class="stat-content">
                        <div class="stat-value">{{ specialistsCount }}</div>
                        <div class="stat-label">
                            {{ $t("admin.stats.specialists") }}
                        </div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">📂</div>
                    <div class="stat-content">
                        <div class="stat-value">{{ categoriesCount }}</div>
                        <div class="stat-label">
                            {{ $t("admin.stats.categories") }}
                        </div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon">🔗</div>
                    <div class="stat-content">
                        <div class="stat-value">{{ connectionTypesCount }}</div>
                        <div class="stat-label">
                            {{ $t("admin.stats.connectionTypes") }}
                        </div>
                    </div>
                </div>
                <div class="stat-card available">
                    <div class="stat-icon">✅</div>
                    <div class="stat-content">
                        <div class="stat-value">
                            {{ availableSpecialistsCount }}
                        </div>
                        <div class="stat-label">
                            {{ $t("admin.stats.available") }}
                        </div>
                    </div>
                </div>
            </div>

            <!-- Admin account/user creation -->
            <section
                v-if="selectedMenu === 'accounts'"
                class="create-account-section"
            >
                <h2>{{ $t("admin.createAccount.title") }}</h2>
                <p class="section-subtitle">
                    {{ $t("admin.createAccount.subtitle") }}
                </p>

                <form
                    class="create-account-form"
                    @submit.prevent="createAccountByAdmin"
                >
                    <div class="form-grid">
                        <div class="form-field">
                            <label for="acc-login">{{
                                $t("admin.createAccount.labels.login")
                            }}</label>
                            <input
                                id="acc-login"
                                v-model="createForm.login"
                                type="text"
                                :placeholder="
                                    $t('admin.createAccount.placeholders.login')
                                "
                                required
                            />
                        </div>
                        <div class="form-field">
                            <label for="acc-email">{{
                                $t("admin.createAccount.labels.email")
                            }}</label>
                            <input
                                id="acc-email"
                                v-model="createForm.email"
                                type="email"
                                :placeholder="
                                    $t('admin.createAccount.placeholders.email')
                                "
                                required
                            />
                        </div>
                        <div class="form-field">
                            <label for="acc-name">{{
                                $t("admin.createAccount.labels.name")
                            }}</label>
                            <input
                                id="acc-name"
                                v-model="createForm.name"
                                type="text"
                                :placeholder="
                                    $t('admin.createAccount.placeholders.name')
                                "
                                required
                            />
                        </div>
                        <div class="form-field">
                            <label for="acc-role">{{
                                $t("admin.createAccount.labels.role")
                            }}</label>
                            <select
                                id="acc-role"
                                v-model="createForm.role"
                                required
                            >
                                <option value="client">{{
                                    $t("admin.createAccount.roles.client")
                                }}</option>
                                <option value="specialist">{{
                                    $t("admin.createAccount.roles.specialist")
                                }}</option>
                                <option value="admin">{{
                                    $t("admin.createAccount.roles.admin")
                                }}</option>
                            </select>
                        </div>
                        <div class="form-field">
                            <label for="acc-phone">{{
                                $t("admin.createAccount.labels.phone")
                            }}</label>
                            <input
                                id="acc-phone"
                                v-model="createForm.phone"
                                type="tel"
                                :placeholder="
                                    $t('admin.createAccount.placeholders.phone')
                                "
                            />
                        </div>
                        <div class="form-field">
                            <label for="acc-password">{{
                                $t("admin.createAccount.labels.password")
                            }}</label>
                            <input
                                id="acc-password"
                                v-model="createForm.password"
                                type="password"
                                required
                            />
                        </div>
                        <div class="form-field">
                            <label for="acc-confirm">{{
                                $t("admin.createAccount.labels.confirmPassword")
                            }}</label>
                            <input
                                id="acc-confirm"
                                v-model="createForm.confirmPassword"
                                type="password"
                                required
                            />
                        </div>
                    </div>

                    <div class="form-actions">
                        <button
                            type="submit"
                            class="btn primary"
                            :disabled="creatingAccount"
                        >
                            {{
                                creatingAccount
                                    ? $t("admin.createAccount.buttons.creating")
                                    : $t("admin.createAccount.buttons.create")
                            }}
                        </button>
                    </div>

                    <div v-if="createError" class="form-message error">
                        {{ createError }}
                    </div>
                    <div v-if="createSuccess" class="form-message success">
                        {{ createSuccess }}
                    </div>
                </form>
            </section>

            <!-- Administrators Section Component -->
            <AdminsSection
                :key="'admins-' + selectedMenu"
                :visible="selectedMenu === 'admins'"
            />

            <!-- Specialists Section Component -->
            <SpecialistsSection :visible="selectedMenu === 'specialists'" />

            <!-- Clients Section Component -->
            <ClientsSection :visible="selectedMenu === 'clients'" />

            <!-- Connection Types Section Component -->
            <ConnectionTypesSection :visible="selectedMenu === 'connections'" />

            <!-- Categories Section Component -->
            <CategoriesSection :visible="selectedMenu === 'categories'" />

            <!-- Settings Section Component -->
            <SettingsSection :visible="selectedMenu === 'settings'" />
        </div>

        <!-- Confirmation Modal -->
        <div v-if="confirmState.visible" class="modal-overlay">
            <div class="modal">
                <div class="modal-header">{{ confirmState.title }}</div>
                <p class="modal-message">{{ confirmState.message }}</p>
                <div class="modal-actions">
                    <button type="button" class="btn" @click="cancelConfirm">
                        {{ $t("common.cancel") }}
                    </button>
                    <button
                        type="button"
                        class="btn danger"
                        @click="acceptConfirm"
                    >
                        {{ $t("common.confirm") }}
                    </button>
                </div>
            </div>
        </div>

        <!-- Idle Timeout Warning Modal -->
        <IdleTimeoutModal
            :visible="isWarningVisible"
            :format-remaining-time="formatRemainingTime"
            @stay="hideWarning"
            @logout="performLogout"
        />
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { useRuntimeConfig } from "nuxt/app";
import { useState } from "nuxt/app";
import { useApi } from "../composables/useApi";
import { adminRegisterRequest } from "../composables/useRegister";
import { useIdleTimeout } from "~/composables/useIdleTimeout";
import IdleTimeoutModal from "~/components/IdleTimeoutModal.vue";
import AdminsSection from "~/components/AdminsSection.vue";
import SpecialistsSection from "~/components/SpecialistsSection.vue";
import ClientsSection from "~/components/ClientsSection.vue";
import ConnectionTypesSection from "~/components/ConnectionTypesSection.vue";
import CategoriesSection from "~/components/CategoriesSection.vue";
import SettingsSection from "~/components/SettingsSection.vue";

const { t } = useI18n();
const router = useRouter();
const config = useRuntimeConfig();
const { $fetch } = useApi();

// Idle timeout
const {
    isWarningVisible,
    formatRemainingTime,
    hideWarning,
    performLogout,
    start,
    idleTimeoutMinutes,
    idleWarningMinutes,
} = useIdleTimeout();

// Start idle timeout tracking explicitly
onMounted(async () => {
    await start();
});

type MenuKey =
    | "accounts"
    | "admins"
    | "specialists"
    | "clients"
    | "connections"
    | "categories"
    | "settings";

const selectedMenu = useState<MenuKey>("selectedMenu", () => "accounts");
const specialistsCount = ref(0);
const categoriesCount = ref(0);
const connectionTypesCount = ref(0);
const availableSpecialistsCount = ref(0);

const createForm = ref({
    login: "",
    email: "",
    name: "",
    phone: "",
    role: "client",
    password: "",
    confirmPassword: "",
});
const creatingAccount = ref(false);
const createError = ref("");
const createSuccess = ref("");

const confirmState = ref({
    visible: false,
    title: "",
    message: "",
});
const confirmResolver = ref<((value: boolean) => void) | null>(null);

const selectMenu = (menu: MenuKey) => {
    selectedMenu.value = menu;
};

// Debug watcher - only run on client
onMounted(() => {
    watch(selectedMenu, (newVal, oldVal) => {});
});

const loadStats = async () => {
    try {
        // Load specialists
        const specialists = await $fetch<Array<{ isAvailable?: boolean }>>(
            `${config.public.apiBase}/specialists/search?offset=0&limit=1000`,
        );
        specialistsCount.value = specialists.length;
        availableSpecialistsCount.value = specialists.filter(
            (specialist: { isAvailable?: boolean }) => specialist.isAvailable,
        ).length;

        // Load categories
        const categories = await $fetch<any[]>(
            `${config.public.apiBase}/categories`,
        );
        categoriesCount.value = categories.length;

        // Load connection types
        const connectionTypes = await $fetch<any[]>(
            `${config.public.apiBase}/connection-types`,
        );
        connectionTypesCount.value = connectionTypes.length;
    } catch (error) {
        // Error handling can be added here if needed
    }
};

const createAccountByAdmin = async () => {
    createError.value = "";
    createSuccess.value = "";

    if (
        !createForm.value.login ||
        !createForm.value.email ||
        !createForm.value.name ||
        !createForm.value.password
    ) {
        createError.value = t("admin.createAccount.messages.fillRequired");
        return;
    }

    if (createForm.value.password !== createForm.value.confirmPassword) {
        createError.value = t(
            "admin.createAccount.messages.passwordsDoNotMatch",
        );
        return;
    }

    creatingAccount.value = true;
    try {
        const result = await adminRegisterRequest({
            login: createForm.value.login,
            email: createForm.value.email,
            password: createForm.value.password,
            name: createForm.value.name,
            phone: createForm.value.phone || undefined,
            role: createForm.value.role,
        });

        if (!result.success) {
            createError.value =
                result.error || t("admin.createAccount.messages.createFailed");
            return;
        }

        createSuccess.value = t("admin.createAccount.messages.success", {
            role: t(
                `admin.createAccount.roles.${result.user?.role || createForm.value.role}`,
            ),
            login: result.user?.login || createForm.value.login,
        });
        createForm.value = {
            login: "",
            email: "",
            name: "",
            phone: "",
            role: "client",
            password: "",
            confirmPassword: "",
        };
        await loadStats();
    } finally {
        creatingAccount.value = false;
    }
};

const logout = async () => {
    try {
        const sessionId = sessionStorage.getItem("sessionId");
        if (sessionId) {
            await $fetch(`${config.public.apiBase}/users/logout`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: { sessionId },
            });
        }
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

const acceptConfirm = () => {
    confirmState.value = { ...confirmState.value, visible: false };
    confirmResolver.value?.(true);
    confirmResolver.value = null;
};

const cancelConfirm = () => {
    confirmState.value = { ...confirmState.value, visible: false };
    confirmResolver.value?.(false);
    confirmResolver.value = null;
};

onMounted(() => {
    loadStats();
});
</script>

<style scoped>
.main-container {
    display: flex;
    min-height: 100vh;
    width: 100%;
    justify-content: center;
    background: #f8fafc;
}

.menu-panel {
    width: 200px;
    background: #f5f5f5;
    padding: 1.5rem 0.75rem;
    border-right: 1px solid #ddd;
    flex-shrink: 0;
    position: relative;
    z-index: 10;
}

.menu-title {
    font-weight: 700;
    color: #1f2937;
    font-size: 0.95rem;
}

.menu-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.75rem;
}

.menu-divider {
    height: 1px;
    background: #e5e7eb;
    margin: 1rem 0;
}

.menu-panel ul {
    list-style: none;
    padding: 0;
}

.menu-panel li {
    padding: 0.55rem 0.75rem;
    cursor: pointer;
    color: #007bff;
    border-radius: 4px;
    transition: background 0.2s;
    font-size: 0.9rem;
}

.menu-panel li.active {
    background: #e0e7ff;
    color: #1d4ed8;
    font-weight: 600;
}

.menu-panel li.logout {
    color: #dc2626;
}

.menu-panel li:hover {
    background: #e6e6e6;
}

.content {
    flex: 1;
    padding: 1.25rem;
    max-width: 90vw;
    min-width: 0;
    overflow-x: auto;
    position: relative;
    z-index: 1;
}

.content h1 {
    margin-bottom: 1.5rem;
    font-size: 1.875rem;
    color: #111827;
}

.create-account-section {
    margin-bottom: 2rem;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    padding: 1rem;
}

.create-account-section h2 {
    margin: 0 0 0.5rem;
    font-size: 1.25rem;
    color: #111827;
}

.section-subtitle {
    margin: 0 0 1rem;
    color: #6b7280;
}

.create-account-form {
    display: block;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 0.75rem;
}

.form-field {
    display: flex;
    flex-direction: column;
}

.form-field label {
    font-size: 0.85rem;
    color: #374151;
    margin-bottom: 0.25rem;
}

.form-field input,
.form-field select {
    padding: 0.5rem 0.6rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
}

.form-actions {
    margin-top: 1rem;
}

.btn.primary {
    background: #2563eb;
    color: #fff;
    border-color: #1d4ed8;
}

.form-message {
    margin-top: 0.75rem;
    padding: 0.65rem 0.75rem;
    border-radius: 6px;
    font-size: 0.9rem;
}

.form-message.error {
    background: #fee2e2;
    color: #b91c1c;
}

.form-message.success {
    background: #dcfce7;
    color: #166534;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 1rem;
    margin-bottom: 2rem;
}

.stat-card {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 1.25rem;
    display: flex;
    align-items: center;
    gap: 1rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    transition: all 0.3s ease;
}

.stat-card:hover {
    border-color: #d1d5db;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.08);
    transform: translateY(-2px);
}

.stat-card.available {
    border-left: 4px solid #10b981;
}

.stat-icon {
    font-size: 2.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 60px;
}

.stat-content {
    display: flex;
    flex-direction: column;
}

.stat-value {
    font-size: 1.875rem;
    font-weight: 700;
    color: #111827;
    line-height: 1;
    margin-bottom: 0.25rem;
}

.stat-label {
    font-size: 0.875rem;
    color: #6b7280;
    font-weight: 500;
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

.modal {
    background: #ffffff;
    padding: 1.5rem;
    border-radius: 12px;
    width: min(420px, 90vw);
    box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
}

.modal-header {
    font-size: 1.2rem;
    font-weight: 700;
    color: #111827;
    margin-bottom: 0.75rem;
}

.modal-message {
    color: #374151;
    margin-bottom: 1.25rem;
}

.modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
}

.btn {
    padding: 0.45rem 0.9rem;
    border-radius: 6px;
    border: 1px solid #cbd5f5;
    background: #ffffff;
    color: #1f2937;
    cursor: pointer;
    font-weight: 600;
    font-size: 0.85rem;
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
}

.btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.debug-box {
    border: 3px solid red;
    padding: 20px;
    background: yellow;
    font-weight: bold;
    font-size: 20px;
    margin: 20px 0;
    text-align: center;
}
</style>
