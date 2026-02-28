<template>
    <div class="main-container">
        <nav class="menu-panel">
            <div class="menu-header">
                <div class="menu-title">{{ $t("specialist.menu.title") }}</div>
                <LocaleSwitcher />
            </div>
            <ul>
                <li
                    :class="{ active: selectedMenu === 'profile' }"
                    @click="selectMenu('profile')"
                >
                    {{ $t("specialist.menu.profile") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'notifications' }"
                    @click="selectMenu('notifications')"
                >
                    {{ $t("specialist.menu.notifications") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'rates' }"
                    @click="selectMenu('rates')"
                >
                    {{ $t("specialist.menu.rates") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'availability' }"
                    @click="selectMenu('availability')"
                >
                    {{ $t("specialist.menu.availability") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'connections' }"
                    @click="selectMenu('connections')"
                >
                    {{ $t("specialist.menu.connections") }}
                </li>
                <li
                    :class="{ active: selectedMenu === 'consultations' }"
                    @click="selectMenu('consultations')"
                >
                    {{ $t("specialist.menu.consultations") }}
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
                        {{ $t("specialist.welcome.title") }}
                    </h1>
                    <p class="welcome-subtitle">
                        {{ $t("specialist.welcome.subtitle") }}
                    </p>
                </div>
            </div>

            <!-- Profile Section -->
            <ProfileSection
                v-if="selectedMenu === 'profile'"
                ref="profileSectionRef"
            />

            <!-- Rates Section -->
            <RatesSection
                v-if="selectedMenu === 'rates'"
                ref="ratesSectionRef"
            />

            <!-- Availability Section -->
            <AvailabilitySection
                v-if="selectedMenu === 'availability'"
                ref="availabilitySectionRef"
            />

            <!-- Connections Section -->
            <ConnectionsSection
                v-if="selectedMenu === 'connections'"
                ref="connectionsSectionRef"
            />

            <!-- Consultations Section -->
            <ConsultationsSection
                v-if="selectedMenu === 'consultations'"
                ref="consultationsSectionRef"
            />

            <!-- Notifications Section -->
            <NotificationsSection
                v-if="selectedMenu === 'notifications'"
                ref="notificationsSectionRef"
            />

            <!-- Idle Timeout Warning Modal -->
            <IdleTimeoutModal
                :visible="isWarningVisible"
                :format-remaining-time="formatRemainingTime"
                @stay="hideWarning"
                @logout="performLogout"
            />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "~/composables/useApi";
import { useIdleTimeout } from "~/composables/useIdleTimeout";
import ProfileSection from "~/components/ProfileSection.vue";
import RatesSection from "~/components/RatesSection.vue";
import AvailabilitySection from "~/components/AvailabilitySection.vue";
import ConnectionsSection from "~/components/ConnectionsSection.vue";
import ConsultationsSection from "~/components/ConsultationsSection.vue";
import NotificationsSection from "~/components/NotificationsSection.vue";
import IdleTimeoutModal from "~/components/IdleTimeoutModal.vue";

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
} = useIdleTimeout();

// Start idle timeout tracking
start();

const selectedMenu = ref("profile");
const profileSectionRef = ref<InstanceType<typeof ProfileSection> | null>(null);
const ratesSectionRef = ref<InstanceType<typeof RatesSection> | null>(null);
const availabilitySectionRef = ref<InstanceType<
    typeof AvailabilitySection
> | null>(null);
const connectionsSectionRef = ref<InstanceType<
    typeof ConnectionsSection
> | null>(null);
const consultationsSectionRef = ref<InstanceType<
    typeof ConsultationsSection
> | null>(null);
const notificationsSectionRef = ref<InstanceType<
    typeof NotificationsSection
> | null>(null);

const selectMenu = (menu: string) => {
    selectedMenu.value = menu;
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
        sessionStorage.removeItem("accessToken");
        sessionStorage.removeItem("sessionId");
        sessionStorage.removeItem("userId");
        sessionStorage.removeItem("specialistId");
        sessionStorage.removeItem("login");
        sessionStorage.removeItem("email");
        sessionStorage.removeItem("role");
        localStorage.removeItem("specialist_session");
        router.push("/login");
    }
};

onMounted(() => {
    // Load saved menu selection
    const savedMenu = localStorage.getItem("specialist_menu");
    if (savedMenu) {
        selectedMenu.value = savedMenu;
    }
});
</script>

<style scoped>
.main-container {
    display: flex;
    min-height: 100vh;
    background: #f8fafc;
}

.menu-panel {
    width: 280px;
    background: white;
    border-right: 1px solid #e5e7eb;
    padding: 1.5rem 0;
    display: flex;
    flex-direction: column;
    position: sticky;
    top: 0;
    height: 100vh;
    overflow-y: auto;
}

.menu-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 1.5rem 1.5rem 1.5rem;
    border-bottom: 1px solid #e5e7eb;
    margin-bottom: 1rem;
}

.menu-title {
    font-size: 1.25rem;
    font-weight: 700;
    color: #1f2937;
}

.menu-panel ul {
    list-style: none;
    padding: 0;
    margin: 0;
}

.menu-panel li {
    padding: 0.875rem 1.5rem;
    cursor: pointer;
    color: #4b5563;
    font-weight: 500;
    transition: all 0.2s ease;
    border-left: 3px solid transparent;
}

.menu-panel li:hover {
    background: #f9fafb;
    color: #1f2937;
}

.menu-panel li.active {
    background: #eff6ff;
    color: #3b82f6;
    border-left-color: #3b82f6;
}

.menu-panel li.logout {
    color: #dc2626;
    margin-top: 0.5rem;
}

.menu-panel li.logout:hover {
    background: #fef2f2;
}

.menu-divider {
    height: 1px;
    background: #e5e7eb;
    margin: 1rem 1.5rem;
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
    opacity: 0.95;
    font-size: 1rem;
}
</style>
