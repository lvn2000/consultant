<template>
    <section class="section">
        <SectionHeader
            icon="👤"
            :title="$t('profile.title')"
            :subtitle="$t('profile.subtitle')"
            :show-refresh="true"
            @refresh="loadProfile"
        >
            <template #actions>
                <AppButton @click="loadProfile">{{
                    $t("common.refresh")
                }}</AppButton>
            </template>
        </SectionHeader>

        <LoadingState v-if="profileLoading" :message="$t('profile.loading')" />
        <ErrorState v-else-if="profileError" :message="profileError" />

        <form v-else class="form" @submit.prevent="updateProfile">
            <FormGrid>
                <FormField
                    :label="$t('profile.fullName')"
                    input-id="name"
                    type="text"
                    :placeholder="$t('profile.namePlaceholder')"
                    :required="true"
                    :model-value="profileForm.name"
                    @update:model-value="profileForm.name = $event"
                />
                <FormField
                    :label="$t('common.email')"
                    input-id="email"
                    type="email"
                    :placeholder="$t('profile.emailPlaceholder')"
                    :required="true"
                    :model-value="profileForm.email"
                    @update:model-value="profileForm.email = $event"
                />
                <FormField
                    :label="$t('common.phone')"
                    input-id="phone"
                    type="tel"
                    :placeholder="$t('profile.phonePlaceholder')"
                    :model-value="profileForm.phone"
                    @update:model-value="profileForm.phone = $event"
                />
                <FormField
                    :label="$t('profile.availabilityStatus')"
                    input-id="availability"
                    type="select"
                    :model-value="profileForm.isAvailable?.toString()"
                    @update:model-value="
                        profileForm.isAvailable = $event === 'true'
                    "
                >
                    <template #options>
                        <option value="true">
                            {{ $t("common.available") }}
                        </option>
                        <option value="false">
                            {{ $t("common.unavailable") }}
                        </option>
                    </template>
                </FormField>
            </FormGrid>

            <FormActions>
                <AppButton type="submit" :loading="profileUpdating">
                    {{
                        profileUpdating
                            ? $t("common.saving")
                            : $t("profile.updateProfile")
                    }}
                </AppButton>
                <AppButton
                    type="button"
                    variant="danger"
                    @click="showRemoveAccountConfirm = true"
                >
                    Remove Account
                </AppButton>
            </FormActions>

            <div
                v-if="profileUpdateMessage"
                :class="[
                    'update-message',
                    profileUpdateSuccess ? 'success' : 'error',
                ]"
            >
                <span v-if="profileUpdateSuccess">✓</span>
                <span v-else>✗</span>
                {{ profileUpdateMessage }}
            </div>
        </form>

        <!-- Remove Account Confirmation -->
        <Modal
            v-model="showRemoveAccountConfirm"
            title="Remove Account"
            size="md"
            :show-close="true"
        >
            <p>
                Are you sure you want to remove your account? This action cannot
                be undone.
            </p>
            <template #footer>
                <AppButton
                    variant="danger"
                    :loading="accountRemoving"
                    @click="removeAccount"
                >
                    {{
                        accountRemoving ? "Removing..." : "Yes, Remove Account"
                    }}
                </AppButton>
                <AppButton
                    variant="secondary"
                    @click="showRemoveAccountConfirm = false"
                >
                    Cancel
                </AppButton>
            </template>
        </Modal>
    </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "~/composables/useApi";

// Component imports
import SectionHeader from "~/components/base/SectionHeader.vue";
import LoadingState from "~/components/base/LoadingState.vue";
import ErrorState from "~/components/base/ErrorState.vue";
import FormGrid from "~/components/form/FormGrid.vue";
import FormField from "~/components/form/FormField.vue";
import FormActions from "~/components/form/FormActions.vue";
import FormMessage from "~/components/form/FormMessage.vue";
import Modal from "~/components/ui/Modal.vue";
import AppButton from "~/components/ui/AppButton.vue";

const router = useRouter();
const config = useRuntimeConfig();
const { $fetch } = useApi();
const { t } = useI18n();

type SpecialistProfile = {
    name?: string;
    email?: string;
    phone?: string;
    bio?: string;
    isAvailable?: boolean;
    categoryRates?: Array<{
        categoryId: string;
        hourlyRate: number;
        experienceYears: number;
    }>;
};

type CategoryRate = {
    categoryId: string;
    hourlyRate: number;
    experienceYears: number;
};

const profileLoading = ref(false);
const profileError = ref("");
const profileForm = ref({
    name: "",
    email: "",
    phone: "",
    bio: "",
    isAvailable: true,
    categoryRates: [] as CategoryRate[],
});
const profileUpdating = ref(false);
const profileUpdateMessage = ref("");
const profileUpdateSuccess = ref(false);
const showRemoveAccountConfirm = ref(false);
const accountRemoving = ref(false);

const loadProfile = async () => {
    profileLoading.value = true;
    profileError.value = "";
    try {
        const specialistId = sessionStorage.getItem("specialistId");
        if (!specialistId) {
            profileError.value = t("auth.userIdNotFound");
            return;
        }
        const specialist = await $fetch<SpecialistProfile>(
            `${config.public.apiBase}/specialists/${specialistId}`,
        );
        profileForm.value = {
            name: specialist.name || "",
            email: specialist.email || "",
            phone: specialist.phone || "",
            bio: specialist.bio || "",
            isAvailable: specialist.isAvailable ?? true,
            categoryRates: specialist.categoryRates || [],
        };
    } catch (error: any) {
        profileError.value = error.message || t("profile.failedToLoad");
    } finally {
        profileLoading.value = false;
    }
};

const updateProfile = async () => {
    profileUpdating.value = true;
    profileUpdateMessage.value = "";
    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            profileUpdateMessage.value = t("auth.userIdNotFound");
            profileUpdateSuccess.value = false;
            return;
        }
        await $fetch(`${config.public.apiBase}/specialists/${userId}`, {
            method: "PUT",
            body: profileForm.value,
        });
        profileUpdateMessage.value = t("profile.profileUpdated");
        profileUpdateSuccess.value = true;
        // Auto-hide success message after 5 seconds
        setTimeout(() => {
            profileUpdateMessage.value = "";
        }, 5000);
    } catch (error: any) {
        profileUpdateMessage.value =
            error.message || t("profile.failedToUpdate");
        profileUpdateSuccess.value = false;
    } finally {
        profileUpdating.value = false;
    }
};

const removeAccount = async () => {
    accountRemoving.value = true;
    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            alert(t("auth.userIdNotFound"));
            return;
        }
        await $fetch(`${config.public.apiBase}/specialists/${userId}`, {
            method: "DELETE",
        });
        alert("Account removed successfully");
        logout();
    } catch (error: any) {
        alert(error.message || "Failed to remove account");
    } finally {
        accountRemoving.value = false;
        showRemoveAccountConfirm.value = false;
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
    loadProfile();
});

defineExpose({
    loadProfile,
});
</script>

<style scoped>
.section {
    margin-top: 2rem;
}

.form {
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.update-message {
    margin-top: 1rem;
    padding: 1rem;
    border-radius: 6px;
    font-weight: 600;
    font-size: 0.9rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    animation: slideDown 0.3s ease-out;
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

@keyframes slideDown {
    from {
        opacity: 0;
        transform: translateY(-10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}
</style>
