<template>
    <div class="main-container">
        <nav class="menu-panel">
            <div class="menu-header">
                <div class="menu-title">{{ $t("client.menu.title") }}</div>
                <LocaleSwitcher />
            </div>
            <ul>
                <li
                    :class="{ active: selectedMenu === 'profile' }"
                    @click="selectMenu('profile')"
                >
                    {{ $t("client.menu.profile") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'notifications' }"
                    @click="selectMenu('notifications')"
                >
                    {{ $t("client.menu.notifications") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'connections' }"
                    @click="selectMenu('connections')"
                >
                    {{ $t("client.menu.connections") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'consultations' }"
                    @click="selectMenu('consultations')"
                >
                    {{ $t("client.menu.consultations") }}
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
            <div class="welcome-header">
                <div class="welcome-content">
                    <h1 class="welcome-title">
                        <span class="wave">👋</span>
                        {{ $t("client.welcome.title") }}
                    </h1>
                    <p class="welcome-subtitle">
                        {{ $t("client.welcome.subtitle") }}
                    </p>
                </div>
            </div>

            <!-- Profile Section -->
            <ProfileSection
                v-if="selectedMenu === 'profile'"
                ref="profileSectionRef"
                @remove-account="showRemoveAccountConfirm = true"
            />

            <!-- Notifications Section -->
            <NotificationsSection
                v-if="selectedMenu === 'notifications'"
                ref="notificationsSectionRef"
            />

            <!-- Connections Section -->
            <ConnectionsSection
                v-if="selectedMenu === 'connections'"
                ref="connectionsSectionRef"
            />

            <!-- Consultations Section -->
            <div v-if="selectedMenu === 'consultations'" class="section">
                <div class="section-header">
                    <div class="header-content">
                        <h2>
                            <span class="icon">📞</span
                            >{{ $t("client.consultations.title") }}
                        </h2>
                        <p class="header-subtitle">
                            {{ $t("client.consultations.headerSubtitle") }}
                        </p>
                    </div>
                </div>

                <!-- Tabs for View and Book -->
                <div class="tabs-header">
                    <button
                        :class="[
                            'tab-btn',
                            consultationsTab === 'view' ? 'active' : '',
                        ]"
                        @click="consultationsTab = 'view'"
                    >
                        {{ $t("client.consultations.viewTab") }}
                    </button>
                    <button
                        :class="[
                            'tab-btn',
                            consultationsTab === 'book' ? 'active' : '',
                        ]"
                        @click="consultationsTab = 'book'"
                    >
                        {{ $t("client.consultations.bookTab") }}
                    </button>
                </div>

                <!-- View Consultations Tab -->
                <ConsultationsViewTab
                    v-if="consultationsTab === 'view'"
                    :loading="consultationsLoading"
                    :error="consultationsError"
                    :consultations="consultations"
                    :filters="consultationFilters"
                    :pagination="consultationPagination"
                    @load-consultations="loadConsultations"
                    @go-to-page="goToPage"
                    @clear-filters="clearConsultationFilters"
                />

                <!-- Book Consultation Tab -->
                <ConsultationsBookTab
                    v-if="consultationsTab === 'book'"
                    @consultation-created="loadConsultations"
                />
            </div>
        </div>

        <!-- Remove Account Confirmation Modal -->
        <RemoveAccountModal
            :show="showRemoveAccountConfirm"
            @close="showRemoveAccountConfirm = false"
            @removed="handleAccountRemoved"
        />

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
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useRuntimeConfig } from "nuxt/app";
import { $fetch } from "ofetch";

const { t } = useI18n();
import ProfileSection from "~/components/ProfileSection.vue";
import NotificationsSection from "~/components/NotificationsSection.vue";
import ConnectionsSection from "~/components/ConnectionsSection.vue";
import ConsultationsViewTab from "~/components/ConsultationsViewTab.vue";
import ConsultationsBookTab from "~/components/ConsultationsBookTab.vue";
import RemoveAccountModal from "~/components/RemoveAccountModal.vue";
import IdleTimeoutModal from "~/components/IdleTimeoutModal.vue";
import { useIdleTimeout } from "~/composables/useIdleTimeout";

const router = useRouter();
const config = useRuntimeConfig();

// Idle timeout
const {
    isWarningVisible,
    formatRemainingTime,
    hideWarning,
    performLogout,
    start,
} = useIdleTimeout();

// Start idle timeout tracking
start();

// Menu state
const selectedMenu = ref("profile");
const showRemoveAccountConfirm = ref(false);

// Component refs
const profileSectionRef = ref<any>(null);
const notificationsSectionRef = ref<any>(null);
const connectionsSectionRef = ref<any>(null);

// Handle account removal
const handleAccountRemoved = () => {
    // Session is already cleared in the modal
    // Just redirect to login
    router.push("/login");
};

// Consultations state
const consultationsTab = ref("view");
const consultationsLoading = ref(false);
const consultationsError = ref("");
const consultations = ref<any[]>([]);
const consultationFilters = ref({
    status: "",
    fromDate: "",
    toDate: "",
    search: "",
});
const consultationPagination = ref({
    currentPage: 1,
    pageSize: 10,
    totalCount: 0,
    totalPages: 0,
});

const selectMenu = (menu: string) => {
    selectedMenu.value = menu;
    if (menu === "profile") {
        profileSectionRef.value?.loadProfile();
    }
    if (menu === "notifications") {
        notificationsSectionRef.value?.loadNotificationPreferences();
    }
    if (menu === "connections") {
        connectionsSectionRef.value?.loadConnections();
    }
    if (menu === "consultations") {
        loadConsultations();
    }
};

// Consultations operations
const loadConsultations = async () => {
    consultationsLoading.value = true;
    consultationsError.value = "";
    try {
        const userId = sessionStorage.getItem("userId");
        const accessToken = sessionStorage.getItem("accessToken");
        if (!userId) {
            consultationsError.value = t("auth.userIdNotFound");
            return;
        }
        if (!accessToken) {
            consultationsError.value = t("auth.tokenNotFound");
            return;
        }
        const data = await $fetch(
            `${config.public.apiBase}/consultations/user/${userId}`,
            {
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
            },
        );
        consultations.value = data;
        consultationPagination.value.totalCount = data.length;
        consultationPagination.value.totalPages = Math.ceil(
            data.length / consultationPagination.value.pageSize,
        );
        consultationPagination.value.currentPage = 1;
    } catch (error: any) {
        console.error("Consultations load error:", error);
        consultationsError.value =
            error.data?.message ||
            error.message ||
            t("consultations.failedToLoad");
    } finally {
        consultationsLoading.value = false;
    }
};

const clearConsultationFilters = () => {
    consultationFilters.value = {
        status: "",
        fromDate: "",
        toDate: "",
        search: "",
    };
    consultationPagination.value.currentPage = 1;
};

const goToPage = (page: number) => {
    if (page >= 1 && page <= consultationPagination.value.totalPages) {
        consultationPagination.value.currentPage = page;
    }
};

const logout = async () => {
    const sessionId = sessionStorage.getItem("sessionId");

    try {
        if (sessionId) {
            await $fetch(`${config.public.apiBase}/users/logout`, {
                method: "POST",
                body: { sessionId },
            });
        }
    } finally {
        // Clear all stored credentials
        sessionStorage.removeItem("accessToken");
        sessionStorage.removeItem("refreshToken");
        sessionStorage.removeItem("expiresAt");
        sessionStorage.removeItem("userId");
        sessionStorage.removeItem("login");
        sessionStorage.removeItem("email");
        sessionStorage.removeItem("name");
        sessionStorage.removeItem("role");
        sessionStorage.removeItem("sessionId");
        localStorage.removeItem("client_session");
        router.push("/login");
    }
};

onMounted(() => {
    selectMenu("profile");
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
    margin: 0.75rem 0;
}

.menu-panel ul {
    list-style: none;
    padding: 0;
}

.menu-panel li {
    padding: 0.55rem 0.75rem;
    cursor: pointer;
    color: #4f46e5;
    border-radius: 4px;
    transition:
        background 0.15s,
        color 0.15s;
    font-size: 0.9rem;
}

.menu-panel li:hover {
    background: #e0e7ff;
}

.menu-panel li.active {
    background: #4f46e5;
    color: white;
}

.menu-panel li.logout {
    color: #dc2626;
}

.menu-panel li.logout:hover {
    background: #fee2e2;
}

.content {
    flex: 1;
    padding: 2rem;
    max-width: 1200px;
    margin: 0 auto;
}

.welcome-header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 2.5rem;
    border-radius: 12px;
    margin-bottom: 2rem;
    box-shadow: 0 10px 30px rgba(102, 126, 234, 0.15);
}

.welcome-content {
    color: white;
}

.welcome-title {
    margin: 0 0 0.5rem 0;
    font-size: 2rem;
    font-weight: 700;
    display: flex;
    align-items: center;
    gap: 0.75rem;
}

.wave {
    display: inline-block;
    animation: wave 2.5s ease-in-out infinite;
}

@keyframes wave {
    0%,
    100% {
        transform: rotate(0deg);
    }
    25% {
        transform: rotate(14deg);
    }
    75% {
        transform: rotate(-14deg);
    }
}

.welcome-subtitle {
    margin: 0;
    font-size: 1rem;
    opacity: 0.95;
    font-weight: 400;
}

.section {
    margin-top: 2rem;
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
    gap: 1.5rem;
}

.header-content {
    flex: 1;
}

.section-header h2 {
    color: #1f2937;
    margin: 0 0 0.35rem 0;
    font-size: 1.5rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.header-subtitle {
    color: #6b7280;
    margin: 0;
    font-size: 0.875rem;
    font-weight: 400;
}

.icon {
    font-size: 1.5rem;
}

.section h3 {
    color: #1f2937;
    margin-bottom: 1.5rem;
}

.tabs-header {
    display: flex;
    gap: 0.5rem;
    border-bottom: 2px solid #e5e7eb;
    margin-bottom: 1.5rem;
}

.tab-btn {
    padding: 0.75rem 1.5rem;
    background: transparent;
    border: none;
    border-bottom: 2px solid transparent;
    color: #6b7280;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
    position: relative;
    bottom: -2px;
}

.tab-btn:hover {
    color: #374151;
    background: #f9fafb;
}

.tab-btn.active {
    color: #4f46e5;
    border-bottom-color: #4f46e5;
    background: white;
}
</style>
