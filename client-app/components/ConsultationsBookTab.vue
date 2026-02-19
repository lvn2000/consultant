<template>
    <div class="tab-panel">
        <BookingForm
            @submit="handleBookingSubmit"
            :creating="creating"
            :message="message"
            :success="success"
        >
            <template #specialist-dropdown>
                <SpecialistDropdown
                    :specialists="specialists"
                    v-model="form.specialistId"
                    label="Specialist"
                    placeholder="Search specialist"
                    noResultsText="No specialists found"
                    @select="onSpecialistSelect"
                />
            </template>
            <template #category-dropdown>
                <CategoryDropdown
                    :categories="filteredCategories"
                    v-model="form.categoryId"
                    label="Category"
                    placeholder="Select category"
                />
            </template>
            <template #slot-selector>
                <SlotSelector
                    :slots="availableSlots"
                    :loading="slotsLoading"
                    :error="slotsError"
                    :selectedSlotId="form.scheduledTime"
                    @select="onSlotSelect"
                />
            </template>
        </BookingForm>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "~/composables/useApi";
import { useSpecialists } from "~/composables/consultations/useSpecialists";
import { useCategories } from "~/composables/consultations/useCategories";
import { useSlots } from "~/composables/consultations/useSlots";
import { useBooking } from "~/composables/consultations/useBooking";
import SpecialistDropdown from "~/components/consultations/SpecialistDropdown.vue";
import CategoryDropdown from "~/components/consultations/CategoryDropdown.vue";
import SlotSelector from "~/components/consultations/SlotSelector.vue";
import BookingForm from "~/components/consultations/BookingForm.vue";

const emit = defineEmits(["consultation-created"]);

const form = ref({
    specialistId: "",
    categoryId: "",
    description: "",
    scheduledDate: "",
    scheduledTime: "",
});

const {
    specialists,
    loading: specialistsLoading,
    error: specialistsError,
    loadSpecialists,
} = useSpecialists();
const {
    categories,
    loading: categoriesLoading,
    error: categoriesError,
    loadCategories,
    filterCategories,
} = useCategories();
const {
    slots,
    loading: slotsLoading,
    error: slotsError,
    loadSlots,
} = useSlots();
const {
    bookingLoading: creating,
    bookingError: message,
    bookingSuccess: success,
    createBooking,
} = useBooking();

const availableSlots = slots;

const filteredCategories = computed(() => {
    if (!form.value.specialistId) return categories.value;
    // Optionally filter by specialist's categories if needed
    return categories.value;
});

function onSpecialistSelect(specialist) {
    form.value.specialistId = specialist.id;
    loadCategories();
    form.value.categoryId = "";
    loadSlots(specialist.id, form.value.categoryId, form.value.scheduledDate);
}

function onSlotSelect(slotId) {
    form.value.scheduledTime = slotId;
}

async function handleBookingSubmit(payload) {
    form.value.description = payload.description;
    const scheduledAt =
        form.value.scheduledDate && form.value.scheduledTime
            ? new Date(
                  `${form.value.scheduledDate}T${form.value.scheduledTime}`,
              ).toISOString()
            : null;

    const bookingParams = {
        specialistId: form.value.specialistId,
        categoryId: form.value.categoryId,
        description: form.value.description,
        scheduledAt,
        duration: 60,
    };

    const result = await createBooking(bookingParams);
    if (result.success) {
        emit("consultation-created");
        form.value = {
            specialistId: "",
            categoryId: "",
            description: "",
            scheduledDate: "",
            scheduledTime: "",
        };
    }
}

onMounted(() => {
    loadSpecialists();
    loadCategories();
    watch(
        [
            () => form.value.specialistId,
            () => form.value.categoryId,
            () => form.value.scheduledDate,
        ],
        ([specialistId, categoryId, date]) => {
            if (specialistId && date) {
                loadSlots(specialistId, categoryId, date);
            }
        },
    );
});
</script>

<style scoped>
.tab-panel {
    padding: 1rem;
}

.form {
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    padding: 1.5rem;
}

.form h4 {
    margin-top: 0;
    margin-bottom: 1.5rem;
    color: #1f2937;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1.5rem;
    margin-bottom: 1.5rem;
}

.form-field {
    display: flex;
    flex-direction: column;
    position: relative;
}

.form-field label {
    margin-bottom: 0.5rem;
    font-weight: 500;
    color: #374151;
    font-size: 0.9rem;
}

.form-field input,
.form-field textarea,
.form-field select {
    padding: 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.95rem;
    font-family: inherit;
}

.form-field input:focus,
.form-field textarea:focus,
.form-field select:focus {
    outline: none;
    border-color: #4f46e5;
    box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-field textarea {
    resize: vertical;
}

.dropdown {
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    z-index: 1000;
}

.form-actions {
    display: flex;
    gap: 1rem;
    margin-bottom: 1rem;
}

.btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 6px;
    font-size: 0.95rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s;
}

.btn-primary {
    background: #4f46e5;
    color: white;
}

.btn-primary:hover:not(:disabled) {
    background: #4338ca;
}

.btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.slots-container {
    padding: 1rem;
    background: #f9fafb;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
}

.slots-container.error {
    background: #fee2e2;
    border-color: #fca5a5;
    color: #991b1b;
}

.slots-container.empty {
    background: #f3f4f6;
    border-color: #d1d5db;
    color: #666;
}

.slots-spinner {
    text-align: center;
    color: #666;
}

.slots-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 0.75rem;
}

.slot-button {
    padding: 0.75rem;
    border: 2px solid #d1d5db;
    background: white;
    border-radius: 6px;
    cursor: pointer;
    font-size: 0.9rem;
    transition: all 0.15s;
}

.slot-button:hover {
    border-color: #4f46e5;
    color: #4f46e5;
}

.slot-button.selected {
    background: #4f46e5;
    color: white;
    border-color: #4f46e5;
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

.text-gray-500 {
    color: #6b7280;
}
</style>
