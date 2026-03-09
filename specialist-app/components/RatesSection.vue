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
        <SectionHeader
            icon="💰"
            :title="$t('rates.title')"
            :subtitle="$t('rates.subtitle')"
            :show-refresh="true"
            @refresh="loadRates"
        >
            <template #actions>
                <AppButton @click="loadRates">{{
                    $t("common.refresh")
                }}</AppButton>
            </template>
        </SectionHeader>

        <LoadingState v-if="ratesLoading" :message="$t('rates.loading')" />
        <ErrorState v-else-if="ratesError" :message="ratesError" />

        <div v-else-if="paginatedRates.length > 0">
            <AppTable>
                <TableHeader
                    :columns="tableColumns"
                    :column-widths="columnWidths"
                >
                    <span>{{ $t("rates.category") }}</span>
                    <span>{{ $t("rates.hourlyRate") }}</span>
                    <span>{{ $t("rates.experience") }}</span>
                    <span>{{ $t("common.actions") }}</span>
                </TableHeader>

                <TableRow
                    v-for="rate in paginatedRates"
                    :key="rate.categoryId"
                    :columns="tableColumns"
                    :column-widths="columnWidths"
                >
                    <span>{{ getCategoryName(rate.categoryId) }}</span>
                    <span>${{ rate.hourlyRate }}</span>
                    <span>{{ rate.experienceYears }}</span>
                    <span class="row-actions">
                        <AppButton @click="startEditRate(rate)">
                            {{ $t("common.select") || "Select" }}
                        </AppButton>
                        <AppButton
                            variant="danger"
                            @click="removeRate(rate.categoryId)"
                            >{{ $t("common.delete") }}</AppButton
                        >
                    </span>
                </TableRow>
            </AppTable>

            <!-- Pagination -->
            <Pagination
                v-if="ratePagination.totalPages > 1"
                :current-page="ratePagination.currentPage"
                :total-pages="ratePagination.totalPages"
                :total-count="ratePagination.totalCount"
                @page-change="goToRatePage"
            />
        </div>

        <LoadingState v-else :message="$t('rates.noRates')" />

        <!-- Info message when all categories are already used -->
        <div
            v-if="
                !editingRateId &&
                availableCategories.length === 0 &&
                categories.length > 0
            "
            class="info-message"
        >
            <span class="info-icon">ℹ️</span>
            <p>
                {{ $t("rates.allCategoriesUsed") }}
            </p>
        </div>

        <!-- Rate Form - Only show when adding is possible OR when editing -->
        <form
            v-if="editingRateId || availableCategories.length > 0"
            ref="rateFormRef"
            class="form"
            @submit.prevent="saveRate"
        >
            <h3>
                {{
                    editingRateId
                        ? $t("rates.updateRate")
                        : $t("rates.addCategoryRate")
                }}
            </h3>

            <!-- Category selector - disabled when editing (shows current category) -->
            <FormGrid>
                <div class="form-field">
                    <label for="rate-category">{{
                        $t("rates.category")
                    }}</label>
                    <select
                        id="rate-category"
                        :value="rateForm.categoryId"
                        @change="
                            rateForm.categoryId = (
                                $event.target as HTMLSelectElement
                            ).value
                        "
                        :disabled="!!editingRateId"
                        class="form-select"
                    >
                        <option value="">
                            {{ $t("rates.selectCategory") }}
                        </option>
                        <option
                            v-for="category in editingRateId
                                ? categories
                                : availableCategories"
                            :key="category.id"
                            :value="category.id"
                        >
                            {{ category.name }}
                        </option>
                    </select>
                </div>
                <p v-if="isDuplicateCategory" class="form-error">
                    {{ $t("rates.duplicateCategory") }}
                </p>
                <FormField
                    :label="$t('rates.hourlyRateDollar')"
                    input-id="rate-hourly"
                    type="number"
                    :min="1"
                    :step="0.01"
                    :required="true"
                    :model-value="rateForm.hourlyRate.toString()"
                    @update:model-value="rateForm.hourlyRate = Number($event)"
                />
                <FormField
                    :label="$t('rates.experience')"
                    input-id="rate-experience"
                    type="number"
                    :min="0"
                    :step="1"
                    :required="true"
                    :model-value="rateForm.experienceYears.toString()"
                    @update:model-value="
                        rateForm.experienceYears = Number($event)
                    "
                />
            </FormGrid>

            <FormActions>
                <AppButton
                    v-if="
                        !(
                            availableCategories.length === 0 &&
                            categories.length > 0 &&
                            !editingRateId
                        )
                    "
                    type="submit"
                    :loading="rateSaving"
                    :disabled="isDuplicateCategory"
                >
                    {{
                        rateSaving
                            ? $t("common.saving")
                            : editingRateId
                              ? $t("rates.updateRate")
                              : $t("rates.addRateButton")
                    }}
                </AppButton>
                <AppButton
                    v-if="editingRateId"
                    variant="secondary"
                    type="button"
                    @click="cancelEditRate"
                >
                    {{ $t("common.cancel") }}
                </AppButton>
            </FormActions>

            <FormMessage
                v-if="rateMessage"
                :variant="rateSuccess ? 'success' : 'error'"
            >
                {{ rateMessage }}
            </FormMessage>
        </form>
    </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "~/composables/useApi";

// Component imports
import SectionHeader from "~/components/base/SectionHeader.vue";
import LoadingState from "~/components/base/LoadingState.vue";
import ErrorState from "~/components/base/ErrorState.vue";
import AppTable from "~/components/table/AppTable.vue";
import TableHeader from "~/components/table/TableHeader.vue";
import TableRow from "~/components/table/TableRow.vue";
import Pagination from "~/components/ui/Pagination.vue";
import FormGrid from "~/components/form/FormGrid.vue";
import FormField from "~/components/form/FormField.vue";
import FormActions from "~/components/form/FormActions.vue";
import FormMessage from "~/components/form/FormMessage.vue";
import AppButton from "~/components/ui/AppButton.vue";

const config = useRuntimeConfig();
const { $fetch } = useApi();
const { t } = useI18n();

type Category = {
    id: string;
    name: string;
};

type CategoryRate = {
    categoryId: string;
    hourlyRate: number;
    experienceYears: number;
    rating?: number | null;
    totalConsultations?: number | null;
};

type SpecialistProfile = {
    categoryRates?: CategoryRate[];
};

const ratesLoading = ref(false);
const ratesError = ref("");
const rates = ref<CategoryRate[]>([]);
const currentRatePage = ref(1);
const itemsPerPage = 5;
const categories = ref<Category[]>([]);
const rateFormRef = ref<HTMLFormElement | null>(null);
const rateForm = ref({
    categoryId: "",
    hourlyRate: 0,
    experienceYears: 0,
});
const editingRateId = ref("");
const rateSaving = ref(false);
const rateMessage = ref("");
const rateSuccess = ref(false);

// Table column configuration
const tableColumns = ["category", "hourlyRate", "experience", "actions"];
const columnWidths = ["2fr", "1fr", "1fr", "1.5fr"];

const loadRates = async () => {
    ratesLoading.value = true;
    ratesError.value = "";
    currentRatePage.value = 1;
    try {
        const specialistId = sessionStorage.getItem("specialistId");
        if (!specialistId) {
            ratesError.value = t("auth.userIdNotFound");
            return;
        }
        const specialist = await $fetch<SpecialistProfile>(
            `${config.public.apiBase}/specialists/${specialistId}`,
        );
        rates.value = specialist.categoryRates || [];
        console.log("Loaded specialist rates:", rates.value);

        // Load categories with detailed logging
        const categoriesUrl = `${config.public.apiBase}/categories`;
        console.log("Fetching categories from:", categoriesUrl);

        const categoriesData = await $fetch<Category[]>(categoriesUrl);
        console.log("Raw categories response:", categoriesData);
        console.log("Is array?", Array.isArray(categoriesData));

        categories.value = Array.isArray(categoriesData) ? categoriesData : [];
        console.log("Categories value set to:", categories.value);
        console.log("Categories length:", categories.value.length);

        if (categories.value.length === 0) {
            console.warn("No categories loaded! Check API response.");
        }
    } catch (error: any) {
        console.error("Error loading rates:", error);
        console.error("Error data:", error.data);
        console.error("Error message:", error.message);
        ratesError.value = error.message || t("rates.failedToLoad");
    } finally {
        ratesLoading.value = false;
    }
};

const startEditRate = (rate: any) => {
    editingRateId.value = rate.categoryId;
    rateForm.value = {
        categoryId: rate.categoryId,
        hourlyRate: rate.hourlyRate,
        experienceYears: rate.experienceYears,
    };
    console.log("Starting edit rate:", rate);
    console.log("Form set to:", rateForm.value);
    console.log("Categories available:", categories.value.length);

    // Scroll to form and focus
    nextTick(() => {
        if (rateFormRef.value) {
            rateFormRef.value.scrollIntoView({
                behavior: "smooth",
                block: "start",
            });
            const firstInput = rateFormRef.value.querySelector(
                "input, select",
            ) as HTMLInputElement | HTMLSelectElement;
            if (firstInput) {
                setTimeout(() => firstInput.focus(), 300);
            }
        }
    });
};

const cancelEditRate = () => {
    rateForm.value = { categoryId: "", hourlyRate: 0, experienceYears: 0 };
    editingRateId.value = "";
    rateMessage.value = "";
};

const saveRate = async () => {
    rateSaving.value = true;
    rateMessage.value = "";
    console.log("Saving rate:", rateForm.value);
    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            rateMessage.value = t("auth.userIdNotFound");
            rateSuccess.value = false;
            console.error("No userId found");
            return;
        }
        const specialist = await $fetch<SpecialistProfile>(
            `${config.public.apiBase}/specialists/${userId}`,
        );
        console.log("Current specialist:", specialist);

        let updatedRates = [...(specialist.categoryRates || [])];

        if (editingRateId.value) {
            // Update existing rate
            const index = updatedRates.findIndex(
                (r: CategoryRate) => r.categoryId === editingRateId.value,
            );
            if (index !== -1) {
                updatedRates[index] = {
                    ...rateForm.value,
                    rating: updatedRates[index]!.rating || null,
                    totalConsultations:
                        updatedRates[index]!.totalConsultations || 0,
                };
            }
            console.log("Updated rate at index:", index, updatedRates[index]);
        } else {
            // Add new rate
            updatedRates.push({
                ...rateForm.value,
                rating: null,
                totalConsultations: 0,
            });
            console.log("Added new rate:", rateForm.value);
        }

        await $fetch(`${config.public.apiBase}/specialists/${userId}`, {
            method: "PUT",
            body: {
                ...specialist,
                categoryRates: updatedRates,
            },
        });

        rateMessage.value = editingRateId.value
            ? t("rates.rateUpdated")
            : t("rates.rateAdded");
        rateSuccess.value = true;
        console.log("Rate saved successfully");

        // Save current page before resetting form
        const savedPage = currentRatePage.value;

        // Reset form and exit edit mode after successful save
        rateForm.value = {
            categoryId: "",
            hourlyRate: 0,
            experienceYears: 0,
        };
        editingRateId.value = "";

        await loadRates();

        // Restore the page we were on
        currentRatePage.value = savedPage;
    } catch (error: any) {
        console.error("Error saving rate:", error);
        rateMessage.value = error.message || t("rates.failedToSave");
        rateSuccess.value = false;
    } finally {
        rateSaving.value = false;
    }
};

const removeRate = async (categoryId: string) => {
    if (!confirm(t("rates.confirmRemove"))) return;

    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            alert(t("auth.userIdNotFound"));
            return;
        }
        const specialist = await $fetch<SpecialistProfile>(
            `${config.public.apiBase}/specialists/${userId}`,
        );
        const updatedRates = (specialist.categoryRates || []).filter(
            (r: CategoryRate) => r.categoryId !== categoryId,
        );

        await $fetch(`${config.public.apiBase}/specialists/${userId}`, {
            method: "PUT",
            body: {
                ...specialist,
                categoryRates: updatedRates,
            },
        });

        await loadRates();
    } catch (error: any) {
        alert(error.message || t("rates.failedToRemove"));
    }
};

const getCategoryName = (categoryId: string) => {
    const category = categories.value.find(
        (c: Category) => c.id === categoryId,
    );
    return category ? category.name : "Unknown";
};

// Filter out categories that are already added
const availableCategories = computed(() => {
    const addedCategoryIds = rates.value.map((r) => r.categoryId);
    console.log("=== Available Categories Debug ===");
    console.log("Added category IDs:", addedCategoryIds);
    console.log("All categories:", categories.value);
    const filtered = categories.value.filter(
        (c) => !addedCategoryIds.includes(c.id),
    );
    console.log("Available categories (filtered):", filtered);
    console.log("Available count:", filtered.length);
    return filtered;
});

// Check if current form selection is a duplicate
const isDuplicateCategory = computed(() => {
    if (!rateForm.value.categoryId || editingRateId.value) return false;
    return rates.value.some((r) => r.categoryId === rateForm.value.categoryId);
});

const ratePagination = computed(() => {
    const total = rates.value.length;
    const totalPages = Math.ceil(total / itemsPerPage);
    return {
        currentPage: currentRatePage.value,
        totalPages: totalPages || 1,
        totalCount: total,
    };
});

const paginatedRates = computed(() => {
    const start = (currentRatePage.value - 1) * itemsPerPage;
    const end = start + itemsPerPage;
    return rates.value.slice(start, end);
});

const goToRatePage = (page: number) => {
    const totalPages = ratePagination.value.totalPages;
    if (page >= 1 && page <= totalPages) {
        currentRatePage.value = page;
    }
};

onMounted(() => {
    loadRates();
});

defineExpose({
    loadRates,
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

.form h3 {
    margin-top: 0;
    margin-bottom: 1rem;
    color: #1f2937;
}

.row-actions {
    display: flex;
    gap: 0.5rem;
}

/* Make update message visible */
:deep(.form-message) {
    margin-top: 1rem;
    padding: 1rem;
    border-radius: 6px;
    font-weight: 600;
    font-size: 0.9rem;
}

:deep(.form-message.success) {
    background: #d1fae5;
    color: #065f46;
    border: 2px solid #059669;
}

:deep(.form-message.error) {
    background: #fee2e2;
    color: #dc2626;
    border: 2px solid #dc2626;
}

.form-select {
    padding: 0.625rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.875rem;
    background: white;
    color: #1f2937;
    width: 100%;
}

.form-select:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-select:disabled {
    background: #f3f4f6;
    cursor: not-allowed;
}

.form-error {
    color: #dc2626;
    font-size: 0.875rem;
    margin-top: 0.25rem;
}

.info-message {
    background: #dbeafe;
    color: #1e40af;
    border: 2px solid #3b82f6;
    border-radius: 6px;
    padding: 1rem;
    margin-top: 1rem;
}

.info-message p {
    margin: 0;
    font-size: 0.875rem;
    line-height: 1.5;
}

.no-categories-note {
    color: #6b7280;
    font-size: 0.875rem;
    font-style: italic;
}
</style>
