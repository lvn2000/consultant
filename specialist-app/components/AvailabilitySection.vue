<template>
    <section class="section">
        <SectionHeader
            icon="📅"
            :title="$t('availability.title')"
            :subtitle="$t('availability.subtitle')"
            :show-refresh="true"
            @refresh="loadAvailability"
        >
            <template #actions>
                <AppButton @click="loadAvailability">{{
                    $t("common.refresh")
                }}</AppButton>
            </template>
        </SectionHeader>

        <LoadingState
            v-if="availabilityLoading"
            :message="$t('availability.loading')"
        />
        <ErrorState
            v-else-if="availabilityError"
            :message="availabilityError"
        />

        <div v-else class="availability-section">
            <!-- Current Availability -->
            <div v-if="availability.length > 0" class="availability-list">
                <h3>{{ $t("availability.currentSlots") }}</h3>
                <div class="availability-grid">
                    <div
                        v-for="slot in sortedAvailability"
                        :key="slot.id"
                        class="availability-slot"
                    >
                        <div class="slot-day">
                            {{ getDayName(slot.dayOfWeek) }}
                        </div>
                        <div class="slot-time">
                            {{ formatTime(slot.startTime) }} -
                            {{ formatTime(slot.endTime) }}
                        </div>
                        <div class="slot-actions">
                            <AppButton
                                variant="danger"
                                size="sm"
                                :loading="deletingAvailabilityId === slot.id"
                                @click="deleteAvailability(slot.id)"
                            >
                                🗑️ {{ $t("common.delete") }}
                            </AppButton>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Add New Availability -->
            <div class="form">
                <h3>
                    {{
                        editingAvailabilityId
                            ? $t("availability.editSlot")
                            : $t("availability.addSlot")
                    }}
                </h3>
                <FormGrid>
                    <div class="form-field">
                        <label for="day-of-week">{{
                            $t("availability.dayOfWeek")
                        }}</label>
                        <select
                            id="day-of-week"
                            :value="String(availabilityForm.dayOfWeek)"
                            @input="
                                availabilityForm.dayOfWeek = Number(
                                    ($event.target as HTMLSelectElement).value,
                                )
                            "
                            :required="true"
                            class="form-select"
                        >
                            <option value="" disabled>
                                {{ $t("availability.selectDay") }}
                            </option>
                            <option value="0">
                                {{ $t("availability.days.monday") }}
                            </option>
                            <option value="1">
                                {{ $t("availability.days.tuesday") }}
                            </option>
                            <option value="2">
                                {{ $t("availability.days.wednesday") }}
                            </option>
                            <option value="3">
                                {{ $t("availability.days.thursday") }}
                            </option>
                            <option value="4">
                                {{ $t("availability.days.friday") }}
                            </option>
                            <option value="5">
                                {{ $t("availability.days.saturday") }}
                            </option>
                            <option value="6">
                                {{ $t("availability.days.sunday") }}
                            </option>
                        </select>
                        <p class="form-hint">
                            Selected value:
                            {{ availabilityForm.dayOfWeek }} (type:
                            {{ typeof availabilityForm.dayOfWeek }})
                        </p>
                    </div>
                    <FormField
                        :label="$t('availability.startTime')"
                        input-id="start-time"
                        type="time"
                        :required="true"
                        :model-value="availabilityForm.startTime"
                        @update:model-value="
                            availabilityForm.startTime = $event
                        "
                    />
                    <FormField
                        :label="$t('availability.endTime')"
                        input-id="end-time"
                        type="time"
                        :required="true"
                        :model-value="availabilityForm.endTime"
                        @update:model-value="availabilityForm.endTime = $event"
                    />
                </FormGrid>

                <FormActions>
                    <AppButton
                        :loading="availabilitySaving"
                        :disabled="!isAvailabilityFormValid"
                        @click="saveAvailability"
                    >
                        {{
                            availabilitySaving
                                ? $t("common.saving")
                                : editingAvailabilityId
                                  ? $t("availability.updateSlot")
                                  : $t("availability.addSlotButton")
                        }}
                    </AppButton>
                    <AppButton
                        v-if="editingAvailabilityId"
                        variant="secondary"
                        @click="cancelEditAvailability"
                    >
                        {{ $t("common.cancel") }}
                    </AppButton>
                </FormActions>

                <FormMessage
                    v-if="availabilityMessage"
                    :variant="availabilitySuccess ? 'success' : 'error'"
                >
                    {{ availabilityMessage }}
                </FormMessage>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
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
import AppButton from "~/components/ui/AppButton.vue";

const config = useRuntimeConfig();
const { $fetch } = useApi();
const { t } = useI18n();

type AvailabilitySlot = {
    id: string;
    dayOfWeek: number;
    startTime: string;
    endTime: string;
};

const availabilityLoading = ref(false);
const availabilityError = ref("");
const availability = ref<AvailabilitySlot[]>([]);
const availabilityForm = ref({
    dayOfWeek: "" as number | string,
    startTime: "",
    endTime: "",
});
const editingAvailabilityId = ref<string | null>(null);
const availabilitySaving = ref(false);
const availabilityMessage = ref("");
const availabilitySuccess = ref(false);
const deletingAvailabilityId = ref<string | null>(null);

const sortedAvailability = computed(() => {
    return [...availability.value].sort((a, b) => a.dayOfWeek - b.dayOfWeek);
});

const isAvailabilityFormValid = computed(() => {
    const dayOfWeek = availabilityForm.value.dayOfWeek;
    const startTime = availabilityForm.value.startTime;
    const endTime = availabilityForm.value.endTime;

    // dayOfWeek can be 0 (Monday), so we check if it's not empty string
    const dayOfWeekValid =
        dayOfWeek !== "" && dayOfWeek !== null && dayOfWeek !== undefined;
    const startTimeValid = startTime && startTime.trim() !== "";
    const endTimeValid = endTime && endTime.trim() !== "";
    const timeOrderValid =
        startTimeValid && endTimeValid && startTime < endTime;

    const isValid =
        dayOfWeekValid && startTimeValid && endTimeValid && timeOrderValid;

    console.log("Form validation:", {
        dayOfWeek,
        dayOfWeekValid,
        startTime,
        startTimeValid,
        endTime,
        endTimeValid,
        timeOrderValid,
        isValid,
    });

    return isValid;
});

const getDayName = (dayOfWeek: number): string => {
    const days = [
        t("availability.days.monday"),
        t("availability.days.tuesday"),
        t("availability.days.wednesday"),
        t("availability.days.thursday"),
        t("availability.days.friday"),
        t("availability.days.saturday"),
        t("availability.days.sunday"),
    ];
    return days[dayOfWeek] || "Unknown";
};

const formatTime = (timeString: string): string => {
    return timeString || "N/A";
};

const loadAvailability = async () => {
    availabilityLoading.value = true;
    availabilityError.value = "";
    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            availabilityError.value = t("auth.userIdNotFound");
            return;
        }
        const data = await $fetch<AvailabilitySlot[]>(
            `${config.public.apiBase}/specialists/${userId}/availability`,
        );
        availability.value = data || [];
    } catch (error: any) {
        availabilityError.value =
            error.data?.message ||
            error.message ||
            t("availability.failedToLoad");
    } finally {
        availabilityLoading.value = false;
    }
};

const saveAvailability = async () => {
    availabilitySaving.value = true;
    availabilityMessage.value = "";
    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            availabilityMessage.value = t("auth.userIdNotFound");
            availabilitySuccess.value = false;
            return;
        }

        if (editingAvailabilityId.value) {
            // Update existing availability
            await $fetch(
                `${config.public.apiBase}/specialists/${userId}/availability/${editingAvailabilityId.value}`,
                {
                    method: "PUT",
                    body: {
                        dayOfWeek: Number(availabilityForm.value.dayOfWeek),
                        startTime: availabilityForm.value.startTime,
                        endTime: availabilityForm.value.endTime,
                    },
                },
            );
            availabilityMessage.value = t("availability.updated");
        } else {
            // Add new availability
            await $fetch(
                `${config.public.apiBase}/specialists/${userId}/availability`,
                {
                    method: "POST",
                    body: {
                        dayOfWeek: Number(availabilityForm.value.dayOfWeek),
                        startTime: availabilityForm.value.startTime,
                        endTime: availabilityForm.value.endTime,
                    },
                },
            );
            availabilityMessage.value = t("availability.added");
        }

        availabilitySuccess.value = true;

        // Only auto-clear on success, not on error
        setTimeout(() => {
            if (availabilitySuccess.value) {
                cancelEditAvailability();
                loadAvailability();
            }
        }, 1500);
    } catch (error: any) {
        console.error("Error saving availability:", error);
        console.log("Error data:", error.data);
        console.log("Error message:", error.message);

        const errorMsg = error.data?.message || error.message || "";

        // Check for duplicate slot error (database constraint violation)
        if (
            errorMsg.includes("already exists") ||
            errorMsg.includes("duplicate") ||
            errorMsg.includes("unique constraint") ||
            errorMsg.includes("Failed to create availability")
        ) {
            availabilityMessage.value = t("availability.duplicateSlot");
        } else {
            availabilityMessage.value =
                errorMsg || t("availability.failedToSave");
        }
        availabilitySuccess.value = false;
        // Don't auto-clear error messages - let user read them
    } finally {
        availabilitySaving.value = false;
    }
};

const cancelEditAvailability = () => {
    availabilityForm.value = { dayOfWeek: "", startTime: "", endTime: "" };
    editingAvailabilityId.value = null;
    availabilityMessage.value = "";
};

const deleteAvailability = async (availabilityId: string) => {
    if (!confirm(t("availability.confirmDelete"))) return;

    deletingAvailabilityId.value = availabilityId;
    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            alert(t("auth.userIdNotFound"));
            return;
        }
        await $fetch(
            `${config.public.apiBase}/specialists/${userId}/availability/${availabilityId}`,
            {
                method: "DELETE",
            },
        );
        await loadAvailability();
    } catch (error: any) {
        alert(
            error.data?.message ||
                error.message ||
                t("availability.failedToDelete"),
        );
    } finally {
        deletingAvailabilityId.value = null;
    }
};

onMounted(() => {
    loadAvailability();
});

defineExpose({
    loadAvailability,
});
</script>

<style scoped>
.section {
    margin-top: 2rem;
}

.availability-section {
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.availability-list {
    margin-bottom: 2rem;
}

.availability-list h3 {
    margin-top: 0;
    margin-bottom: 1rem;
    color: #1f2937;
}

.availability-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 1rem;
}

.availability-slot {
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    padding: 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.slot-day {
    font-weight: 600;
    color: #1f2937;
    font-size: 0.95rem;
}

.slot-time {
    color: #6b7280;
    font-size: 0.875rem;
}

.slot-actions {
    margin-top: 0.5rem;
}

.form {
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    border-top: 1px solid #e5e7eb;
    margin-top: 1rem;
}

.form h3 {
    margin-top: 0;
    margin-bottom: 1rem;
    color: #1f2937;
}

.form-field {
    display: flex;
    flex-direction: column;
}

.form-field label {
    font-weight: 500;
    margin-bottom: 0.5rem;
    color: #374151;
    font-size: 0.875rem;
}

.form-select {
    padding: 0.625rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.875rem;
    background: white;
    color: #1f2937;
}

.form-select:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-hint {
    color: #6b7280;
    font-size: 0.75rem;
    margin-top: 0.5rem;
}
</style>
