<template>
    <section class="section">
        <SectionHeader
            icon="🔔"
            :title="$t('notifications.title')"
            :subtitle="$t('notifications.subtitle')"
        />

        <LoadingState
            v-if="notificationsLoading"
            :message="$t('notifications.loading')"
        />
        <ErrorState
            v-else-if="notificationsError"
            :message="notificationsError"
        />

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
                            :checked="pref.emailEnabled"
                            @change="handleToggleChange(pref, $event)"
                            :disabled="updatingNotificationId === pref.id"
                        />
                        <span class="toggle-slider"></span>
                    </label>
                </div>
            </div>

            <div
                v-if="notificationUpdateMessage"
                :class="[
                    'update-message',
                    notificationUpdateSuccess ? 'success' : 'error',
                ]"
            >
                <span>{{ notificationUpdateMessage }}</span>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "~/composables/useApi";

// Component imports
import SectionHeader from "~/components/base/SectionHeader.vue";
import LoadingState from "~/components/base/LoadingState.vue";
import ErrorState from "~/components/base/ErrorState.vue";
import FormMessage from "~/components/form/FormMessage.vue";

const { t } = useI18n();
const config = useRuntimeConfig();
const { $fetch } = useApi();

type NotificationPreference = {
    id: string;
    notificationType: string;
    emailEnabled: boolean;
    smsEnabled?: boolean;
};

type NotificationPreferencesResponse = {
    preferences?: NotificationPreference[];
};

const notificationPreferences = ref<NotificationPreference[]>([]);
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
        const token =
            sessionStorage.getItem("accessToken") ||
            sessionStorage.getItem("sessionId");
        if (!userId) {
            notificationsError.value = t("auth.userIdNotFound");
            return;
        }
        if (!token) {
            notificationsError.value = t("auth.sessionExpired");
            return;
        }

        const response = await $fetch<NotificationPreferencesResponse>(
            `${config.public.apiBase}/notification-preferences/${userId}`,
        );

        notificationPreferences.value = response.preferences || [];
    } catch (error: any) {
        notificationsError.value =
            error.data?.message ||
            error.message ||
            t("notifications.failedToLoad");
    } finally {
        notificationsLoading.value = false;
    }
};

const updateNotificationPreference = async (
    preference: NotificationPreference,
    newValue: boolean,
) => {
    updatingNotificationId.value = preference.id;
    notificationUpdateMessage.value = "";
    notificationUpdateSuccess.value = false;

    try {
        const userId = sessionStorage.getItem("userId");
        const token =
            sessionStorage.getItem("accessToken") ||
            sessionStorage.getItem("sessionId");
        if (!userId) {
            notificationUpdateMessage.value = t("auth.userIdNotFound");
            notificationUpdateSuccess.value = false;
            return;
        }
        if (!token) {
            notificationUpdateMessage.value = t("auth.sessionExpired");
            notificationUpdateSuccess.value = false;
            return;
        }

        console.log("Sending API request...");
        await $fetch(
            `${config.public.apiBase}/notification-preferences/${userId}/${preference.notificationType}`,
            {
                method: "PUT",
                body: {
                    emailEnabled: newValue,
                    smsEnabled: preference.smsEnabled,
                },
            },
        );

        console.log("API request successful");
        notificationUpdateMessage.value = t("notifications.preferenceUpdated");
        notificationUpdateSuccess.value = true;

        setTimeout(() => {
            notificationUpdateMessage.value = "";
        }, 3000);
    } catch (error: any) {
        console.error("Error updating notification preference:", error);
        notificationUpdateMessage.value =
            error.data?.message ||
            error.message ||
            t("notifications.failedToUpdate");
        notificationUpdateSuccess.value = false;
        // Revert the change
        await loadNotificationPreferences();
    } finally {
        updatingNotificationId.value = null;
    }
};

const handleToggleChange = async (
    preference: NotificationPreference,
    event: Event,
) => {
    const target = event.target as HTMLInputElement;
    const newValue = target.checked;

    console.log(
        "Toggle clicked:",
        preference.notificationType,
        "new value:",
        newValue,
    );

    // Update the preference immediately
    preference.emailEnabled = newValue;

    // Save to API
    await updateNotificationPreference(preference, newValue);
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
        ConsultationApproved: t(
            "notifications.descriptionsSpecialist.approved",
        ),
        ConsultationDeclined: t(
            "notifications.descriptionsSpecialist.declined",
        ),
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

.notifications-container {
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.notifications-list {
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
    margin-bottom: 1rem;
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
    background-color: #667eea;
}

input:checked + .toggle-slider:before {
    transform: translateX(22px);
}

input:disabled + .toggle-slider {
    opacity: 0.5;
    cursor: not-allowed;
}

.update-message {
    margin-top: 1rem;
    padding: 1rem;
    border-radius: 6px;
    font-weight: 600;
    font-size: 0.9rem;
}

.update-message.success {
    background: #d1fae5;
    color: #065f46;
    border: 2px solid #059669;
}

.update-message.error {
    background: #fee2e2;
    color: #dc2626;
    border: 2px solid #dc2626;
}
</style>
