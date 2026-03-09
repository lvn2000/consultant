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
    <div class="section">
        <div class="section-header">
            <div class="header-content">
                <h2>
                    <span class="icon">🔔</span>{{ $t("notifications.title") }}
                </h2>
                <p class="header-subtitle">
                    {{ $t("notifications.subtitle") }}
                </p>
            </div>
        </div>
        <div v-if="notificationsLoading" class="list-state">
            {{ $t("notifications.loading") }}
        </div>
        <div v-else-if="notificationsError" class="list-state error">
            {{ notificationsError }}
        </div>
        <div v-else class="notifications-container">
            <div class="notifications-list">
                <div
                    v-for="pref in notificationPreferences"
                    :key="pref.id"
                    class="notification-item"
                >
                    <div class="notification-label">
                        <span class="notification-name">{{
                            formatNotificationType(pref.notificationType)
                        }}</span>
                        <span class="notification-description">{{
                            getNotificationDescription(pref.notificationType)
                        }}</span>
                    </div>
                    <label class="toggle-switch">
                        <input
                            type="checkbox"
                            v-model="pref.emailEnabled"
                            @change="updateNotificationPreference(pref)"
                            :disabled="updatingNotificationId === pref.id"
                            :aria-label="
                                formatNotificationType(pref.notificationType) +
                                ' ' +
                                $t('notifications.emailNotifications')
                            "
                        />
                        <span class="toggle-slider"></span>
                    </label>
                </div>
            </div>

            <div
                v-if="notificationUpdateMessage"
                :class="[
                    'form-message',
                    notificationUpdateSuccess ? 'success' : 'error',
                ]"
            >
                {{ notificationUpdateMessage }}
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "../composables/useApi";

const { t } = useI18n();
const config = useRuntimeConfig();
const { $fetch } = useApi();

const notificationPreferences = ref<any[]>([]);
const notificationsLoading = ref(false);
const notificationsError = ref("");
const notificationUpdateMessage = ref("");
const notificationUpdateSuccess = ref(false);
const updatingNotificationId = ref<string | null>(null);

const loadNotificationPreferences = async () => {
    notificationsLoading.value = true;
    notificationsError.value = "";
    try {
        const userId = sessionStorage.getItem("userId");
        const accessToken = sessionStorage.getItem("accessToken");

        if (!userId) {
            notificationsError.value = t("auth.userIdNotFound");
            return;
        }

        if (!accessToken) {
            notificationsError.value = t("auth.tokenNotFound");
            return;
        }

        const response = (await $fetch(
            `${config.public.apiBase}/notification-preferences/${userId}`,
        )) as any;

        notificationPreferences.value = response.preferences || [];
    } catch (error: any) {
        if (process.dev)
            console.error("Notification preferences load error:", error);
        // Check if error is due to expired token
        if (
            error?.status === 401 ||
            error?.data?.message?.includes("token") ||
            error?.data?.message?.includes("authentication")
        ) {
            notificationsError.value = t("auth.sessionExpired");
        } else {
            notificationsError.value =
                error.data?.message ||
                error.message ||
                t("notifications.failedToLoad");
        }
    } finally {
        notificationsLoading.value = false;
    }
};

const updateNotificationPreference = async (preference: any) => {
    updatingNotificationId.value = preference.id;
    notificationUpdateMessage.value = "";
    notificationUpdateSuccess.value = false;

    try {
        const userId = sessionStorage.getItem("userId");
        const accessToken = sessionStorage.getItem("accessToken");

        if (!userId) {
            notificationUpdateMessage.value = t("auth.userIdNotFound");
            return;
        }

        if (!accessToken) {
            notificationUpdateMessage.value = t("auth.tokenNotFound");
            return;
        }

        await $fetch(
            `${config.public.apiBase}/notification-preferences/${userId}/${preference.notificationType}`,
            {
                method: "PUT",
                headers: {
                    "X-User-Id": userId,
                    "X-User-Role": sessionStorage.getItem("role") || "Client",
                },
                body: {
                    emailEnabled: preference.emailEnabled,
                    smsEnabled: preference.smsEnabled,
                },
            },
        );

        notificationUpdateMessage.value = t("notifications.preferenceUpdated");
        notificationUpdateSuccess.value = true;

        setTimeout(() => {
            notificationUpdateMessage.value = "";
        }, 2000);
    } catch (error: any) {
        if (process.dev)
            console.error("Error updating notification preference:", error);
        // Check if error is due to expired token
        if (
            error?.status === 401 ||
            error?.data?.message?.includes("token") ||
            error?.data?.message?.includes("authentication")
        ) {
            notificationUpdateMessage.value = t("auth.sessionExpired");
        } else {
            notificationUpdateMessage.value =
                error.data?.message ||
                error.message ||
                t("notifications.failedToUpdate");
        }
        notificationUpdateSuccess.value = false;
        // Revert the change
        await loadNotificationPreferences();
    } finally {
        updatingNotificationId.value = null;
    }
};

const formatNotificationType = (type: string): string => {
    // Convert ConsultationApproved -> Consultation Approved
    return type
        .replace(/([A-Z])/g, " $1")
        .replace(/^Consultation/, "Consultation")
        .trim();
};

const getNotificationDescription = (type: string): string => {
    const descriptions: { [key: string]: string } = {
        ConsultationApproved: t("notifications.descriptions.approved"),
        ConsultationDeclined: t("notifications.descriptions.declined"),
        ConsultationCompleted: t("notifications.descriptions.completed"),
        ConsultationMissed: t("notifications.descriptions.missed"),
        ConsultationCancelled: t("notifications.descriptions.cancelled"),
    };
    return descriptions[type] || t("notifications.fallbackDescription");
};

onMounted(() => {
    loadNotificationPreferences();
});

defineExpose({
    loadNotificationPreferences,
});
</script>

<style scoped>
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

.list-state {
    padding: 2rem;
    text-align: center;
    color: #666;
    background: white;
    border-radius: 8px;
}

.list-state.error {
    color: #dc2626;
    background: #fee2e2;
}

.notifications-container {
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 2rem;
}

.notifications-list {
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

.notification-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem;
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    transition: background-color 0.2s;
}

.notification-item:hover {
    background: #f3f4f6;
}

.notification-label {
    display: flex;
    flex-direction: column;
    flex: 1;
}

.notification-name {
    font-weight: 600;
    color: #1f2937;
    font-size: 0.95rem;
    margin-bottom: 0.25rem;
}

.notification-description {
    color: #6b7280;
    font-size: 0.85rem;
}

/* Toggle Switch Styles */
.toggle-switch {
    position: relative;
    display: inline-block;
    width: 50px;
    height: 28px;
    margin-left: 1rem;
    flex-shrink: 0;
}

.toggle-switch input {
    opacity: 0;
    width: 0;
    height: 0;
}

.toggle-slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #ccc;
    transition: 0.3s;
    border-radius: 28px;
}

.toggle-slider:before {
    position: absolute;
    content: "";
    height: 22px;
    width: 22px;
    left: 3px;
    bottom: 3px;
    background-color: white;
    transition: 0.3s;
    border-radius: 50%;
}

input:checked + .toggle-slider {
    background-color: #4f46e5;
}

input:checked + .toggle-slider:before {
    transform: translateX(22px);
}

input:disabled + .toggle-slider {
    opacity: 0.5;
    cursor: not-allowed;
}

.form-message {
    padding: 1rem;
    border-radius: 6px;
    margin-top: 1rem;
}

.form-message.success {
    background: #dcfce7;
    color: #166534;
    border: 1px solid #86efac;
}

.form-message.error {
    background: #fee2e2;
    color: #991b1b;
    border: 1px solid #fca5a5;
}
</style>
