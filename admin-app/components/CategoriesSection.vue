<template>
    <section v-if="visible" class="section">
        <div class="section-header">
            <h2>{{ $t("adminCategories.title") }}</h2>
            <button type="button" class="btn" @click="loadCategories">
                🔄 Refresh
            </button>
        </div>

        <div class="search-bar">
            <input
                v-model="categoriesSearchQuery"
                type="text"
                :placeholder="$t('adminCategories.searchPlaceholder')"
                class="search-input"
            />
            <button
                type="button"
                class="btn"
                @click="clearCategoriesSearch"
                v-if="categoriesSearchQuery"
            >
                ❌ Clear
            </button>
        </div>

        <div class="list-state" v-if="categoriesLoading">
            {{ $t("adminCategories.loading") }}
        </div>
        <div class="list-state error" v-else-if="categoriesError">
            {{ categoriesError }}
        </div>

        <div class="table" v-else>
            <div class="table-header categories-table">
                <span>{{ $t("common.name") }}</span>
                <span>{{ $t("common.description") }}</span>
                <span>{{ $t("adminCategories.parent") }}</span>
                <span>{{ $t("common.actions") }}</span>
            </div>
            <div
                v-for="category in pagedFilteredCategories"
                :key="category.id"
                class="table-row categories-table"
            >
                <span>{{ category.name }}</span>
                <span>{{ category.description || "-" }}</span>
                <span>{{ resolveCategoryName(category.parentId) }}</span>
                <span class="row-actions">
                    <button
                        type="button"
                        class="btn"
                        @click="startEditCategory(category)"
                    >
                        ✏️ {{ $t("common.select") }}
                    </button>
                    <button
                        type="button"
                        class="btn danger"
                        @click="removeCategory(category.id)"
                    >
                        🗑️ {{ $t("common.delete") }}
                    </button>
                </span>
            </div>
        </div>

        <div v-if="!categoriesLoading && !categoriesError" class="pagination">
            <div class="pagination-info">
                Page {{ categoryCurrentPage }} of {{ categoryTotalPages }}
            </div>
            <div class="pagination-controls">
                <button
                    type="button"
                    class="btn"
                    :disabled="categoryCurrentPage === 1"
                    @click="goToPreviousCategoryPage"
                >
                    ⬅️ Previous
                </button>
                <button
                    type="button"
                    class="btn"
                    :disabled="categoryCurrentPage === categoryTotalPages"
                    @click="goToNextCategoryPage"
                >
                    Next ➡️
                </button>
            </div>
            <div class="pagination-size">
                <label for="category-page-size">Page size</label>
                <select
                    id="category-page-size"
                    v-model.number="categoryPageSize"
                    @change="handleCategoryPageSizeChange"
                >
                    <option :value="10">10</option>
                    <option :value="20">20</option>
                    <option :value="50">50</option>
                </select>
            </div>
        </div>

        <form ref="categoryFormRef" class="form" @submit.prevent>
            <div class="form-grid">
                <div class="form-field">
                    <label for="category-name">{{ $t("common.name") }}</label>
                    <input
                        id="category-name"
                        v-model="categoryForm.name"
                        type="text"
                        :placeholder="$t('adminCategories.namePlaceholder')"
                    />
                </div>
                <div class="form-field">
                    <label for="category-description">{{
                        $t("common.description")
                    }}</label>
                    <input
                        id="category-description"
                        v-model="categoryForm.description"
                        type="text"
                        :placeholder="
                            $t('adminCategories.descriptionPlaceholder')
                        "
                    />
                </div>
                <div class="form-field">
                    <label for="category-parent">{{
                        $t("adminCategories.parentCategory")
                    }}</label>
                    <select
                        id="category-parent"
                        v-model="categoryForm.parentId"
                    >
                        <option :value="''">
                            {{ $t("adminCategories.noParent") }}
                        </option>
                        <option
                            v-for="category in availableParentCategories"
                            :key="category.id"
                            :value="category.id"
                        >
                            {{ category.name }}
                        </option>
                    </select>
                </div>
            </div>

            <div class="form-actions">
                <button type="button" class="btn primary" @click="addCategory">
                    ➕ {{ $t("adminCategories.addCategory") }}
                </button>
                <button
                    type="button"
                    class="btn"
                    :disabled="!selectedCategoryId"
                    @click="updateCategory"
                >
                    ✏️ {{ $t("adminCategories.updateCategory") }}
                </button>
                <button
                    type="button"
                    class="btn danger"
                    :disabled="!selectedCategoryId"
                    @click="deleteSelectedCategory"
                >
                    🗑️ {{ $t("adminCategories.deleteCategory") }}
                </button>
                <button type="button" class="btn" @click="resetCategoryForm">
                    ❌ Clear
                </button>
            </div>

            <p v-if="categoryActionMessage" class="form-message">
                {{ categoryActionMessage }}
            </p>
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
                        class="btn primary"
                        @click="confirmResolver?.(true)"
                    >
                        Confirm
                    </button>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, nextTick } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "../composables/useApi";

const { t } = useI18n();

defineProps<{
    visible: boolean;
}>();

const config = useRuntimeConfig();
const { $fetch } = useApi();

type Category = {
    id: string;
    name: string;
    description: string;
    parentId: string | null;
};

const categories = ref<Category[]>([]);
const categoriesLoading = ref(false);
const categoriesError = ref("");
const selectedCategoryId = ref<string | null>(null);
const categoryActionMessage = ref("");
const categoryCurrentPage = ref(1);
const categoryPageSize = ref(20);
const categoriesSearchQuery = ref("");
const categoryFormRef = ref<HTMLFormElement | null>(null);
const categoryForm = ref({
    name: "",
    description: "",
    parentId: "" as string | "",
});

const confirmState = ref({
    visible: false,
    title: "",
    message: "",
});
const confirmResolver = ref<((value: boolean) => void) | null>(null);

const categoryTotalPages = computed(() =>
    Math.max(
        1,
        Math.ceil(filteredCategories.value.length / categoryPageSize.value),
    ),
);

const filteredCategories = computed(() => {
    if (!categoriesSearchQuery.value.trim()) {
        return categories.value;
    }

    const query = categoriesSearchQuery.value.toLowerCase().trim();
    return categories.value.filter((category) => {
        return (
            category.name.toLowerCase().includes(query) ||
            category.description.toLowerCase().includes(query)
        );
    });
});

const pagedFilteredCategories = computed(() => {
    const start = (categoryCurrentPage.value - 1) * categoryPageSize.value;
    return filteredCategories.value.slice(
        start,
        start + categoryPageSize.value,
    );
});

const availableParentCategories = computed(() => {
    // Filter out the current category being edited to prevent self-reference
    return categories.value.filter(
        (cat) => cat.id !== selectedCategoryId.value,
    );
});

const confirmAction = (title: string, message: string) =>
    new Promise<boolean>((resolve) => {
        confirmState.value = { visible: true, title, message };
        confirmResolver.value = (confirmed: boolean) => {
            resolve(confirmed);
            confirmState.value.visible = false;
        };
    });

const resolveCategoryName = (id: string | null) => {
    if (!id) return "-";
    return categories.value.find((category) => category.id === id)?.name ?? id;
};

const loadCategories = async () => {
    categoriesLoading.value = true;
    categoriesError.value = "";

    try {
        const data = await $fetch<Category[]>(
            `${config.public.apiBase}/categories`,
            {
                method: "GET",
            },
        );
        categories.value = data;
        if (categoryCurrentPage.value > categoryTotalPages.value) {
            categoryCurrentPage.value = categoryTotalPages.value;
        }
    } catch (error) {
        categories.value = [];
        categoriesError.value = t("adminCategories.failedToLoad");
    } finally {
        categoriesLoading.value = false;
    }
};

const clearCategoriesSearch = () => {
    categoriesSearchQuery.value = "";
    categoryCurrentPage.value = 1;
};

const goToPreviousCategoryPage = () => {
    if (categoryCurrentPage.value > 1) {
        categoryCurrentPage.value -= 1;
    }
};

const goToNextCategoryPage = () => {
    if (categoryCurrentPage.value < categoryTotalPages.value) {
        categoryCurrentPage.value += 1;
    }
};

const handleCategoryPageSizeChange = () => {
    categoryCurrentPage.value = 1;
};

const startEditCategory = (category: Category) => {
    selectedCategoryId.value = category.id;
    categoryForm.value = {
        name: category.name,
        description: category.description,
        parentId: category.parentId ?? "",
    };
    categoryActionMessage.value = "";
    // Scroll to form and focus
    nextTick(() => {
        if (categoryFormRef.value) {
            categoryFormRef.value.scrollIntoView({
                behavior: "smooth",
                block: "start",
            });
            const firstInput = categoryFormRef.value.querySelector(
                "input",
            ) as HTMLInputElement;
            if (firstInput) {
                setTimeout(() => firstInput.focus(), 300);
            }
        }
    });
};

const resetCategoryForm = () => {
    categoryForm.value = {
        name: "",
        description: "",
        parentId: "",
    };
    selectedCategoryId.value = null;
};

const addCategory = async () => {
    categoryActionMessage.value = "";

    // Validate category name is not empty
    if (!categoryForm.value.name.trim()) {
        categoryActionMessage.value = t("adminCategories.nameRequired");
        return;
    }

    // Check for duplicate category name
    const isDuplicate = categories.value.some(
        (cat) =>
            cat.name.toLowerCase() === categoryForm.value.name.toLowerCase(),
    );
    if (isDuplicate) {
        categoryActionMessage.value = t("adminCategories.nameExists");
        return;
    }

    const confirmed = await confirmAction(
        t("adminCategories.addCategory"),
        t("adminCategories.addConfirm"),
    );
    if (!confirmed) return;

    try {
        await $fetch(`${config.public.apiBase}/categories`, {
            method: "POST",
            body: {
                name: categoryForm.value.name,
                description: categoryForm.value.description,
                parentId: categoryForm.value.parentId || null,
            },
        });
        categoryActionMessage.value = t("adminCategories.created");
        resetCategoryForm();
        await loadCategories();
    } catch (error: any) {
        console.error("Add category error:", error);
        if (error.data?.message) {
            categoryActionMessage.value = `${t("adminCategories.failedToCreate")}: ${error.data.message}`;
        } else if (error.message) {
            categoryActionMessage.value = `${t("adminCategories.failedToCreate")}: ${error.message}`;
        } else {
            categoryActionMessage.value = t("adminCategories.failedToCreate");
        }
    }
};

const updateCategory = async () => {
    if (!selectedCategoryId.value) {
        categoryActionMessage.value = t("adminCategories.selectToUpdate");
        return;
    }

    const confirmed = await confirmAction(
        t("adminCategories.updateCategory"),
        t("adminCategories.updateConfirm"),
    );
    if (!confirmed) return;

    categoryActionMessage.value = "";

    try {
        await $fetch(
            `${config.public.apiBase}/categories/${selectedCategoryId.value}`,
            {
                method: "PUT",
                body: {
                    name: categoryForm.value.name,
                    description: categoryForm.value.description,
                    parentId: categoryForm.value.parentId || null,
                },
            },
        );
        categoryActionMessage.value = t("adminCategories.updated");
        await loadCategories();
    } catch (error: any) {
        console.error("Update category error:", error);
        if (error.data?.message) {
            categoryActionMessage.value = `${t("adminCategories.failedToUpdate")}: ${error.data.message}`;
        } else if (error.message) {
            categoryActionMessage.value = `${t("adminCategories.failedToUpdate")}: ${error.message}`;
        } else {
            categoryActionMessage.value = t("adminCategories.failedToUpdate");
        }
    }
};

const deleteSelectedCategory = async () => {
    if (!selectedCategoryId.value) {
        categoryActionMessage.value = t("adminCategories.selectToDelete");
        return;
    }

    const confirmed = await confirmAction(
        t("adminCategories.deleteCategory"),
        t("adminCategories.deleteConfirm"),
    );
    if (!confirmed) return;

    try {
        await $fetch(
            `${config.public.apiBase}/categories/${selectedCategoryId.value}`,
            {
                method: "DELETE",
            },
        );
        categoryActionMessage.value = t("adminCategories.deleted");
        resetCategoryForm();
        await loadCategories();
    } catch (error: any) {
        console.error("Delete category error:", error);
        if (error.data?.message) {
            categoryActionMessage.value = `${t("adminCategories.failedToDelete")}: ${error.data.message}`;
        } else if (error.message) {
            categoryActionMessage.value = `${t("adminCategories.failedToDelete")}: ${error.message}`;
        } else {
            categoryActionMessage.value = t("adminCategories.failedToDelete");
        }
    }
};

const removeCategory = async (id: string) => {
    const confirmed = await confirmAction(
        t("adminCategories.deleteCategory"),
        t("adminCategories.deleteConfirm"),
    );
    if (!confirmed) return;

    try {
        await $fetch(`${config.public.apiBase}/categories/${id}`, {
            method: "DELETE",
        });
        await loadCategories();
    } catch (error: any) {
        console.error("Remove category error:", error);
        if (error.data?.message) {
            categoryActionMessage.value = `${t("adminCategories.failedToDelete")}: ${error.data.message}`;
        } else if (error.message) {
            categoryActionMessage.value = `${t("adminCategories.failedToDelete")}: ${error.message}`;
        } else {
            categoryActionMessage.value = t("adminCategories.failedToDelete");
        }
    }
};

onMounted(() => {
    loadCategories();
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
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1rem;
}

.section h2 {
    margin-bottom: 0.75rem;
    font-size: 1.25rem;
    color: #111827;
}

.list-state {
    padding: 0.75rem 1rem;
    border-radius: 6px;
    background: #f8fafc;
    color: #475569;
    margin-bottom: 1rem;
}

.list-state.error {
    background: #fee2e2;
    color: #b91c1c;
}

.table {
    display: flex;
    flex-direction: column;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    overflow: hidden;
    margin-bottom: 1.5rem;
}

.table-header,
.table-row {
    display: grid;
    gap: 0.5rem;
    padding: 0.5rem 0.75rem;
    align-items: center;
    font-size: 0.85rem;
}

.table-header.categories-table,
.table-row.categories-table {
    grid-template-columns: 1fr 2fr 1fr 1.2fr;
}

.table-header {
    background: #f1f5f9;
    font-weight: 600;
    color: #1f2937;
}

.table-row {
    border-top: 1px solid #e5e7eb;
    background: #ffffff;
}

.row-actions {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.pagination {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1.5rem;
}

.pagination-info {
    font-weight: 600;
    color: #1f2937;
}

.pagination-controls {
    display: flex;
    gap: 0.5rem;
}

.pagination-size {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    color: #374151;
}

.form {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 1rem;
}

.form-field {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.form-field label {
    font-weight: 600;
    color: #374151;
    font-size: 0.85rem;
}

.form-field input,
.form-field select {
    padding: 0.45rem 0.6rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-family: inherit;
    font-size: 0.85rem;
}

.form-field input:focus,
.form-field select:focus {
    border-color: #6366f1;
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.form-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
}

.form-message {
    margin-top: 0.5rem;
    color: #1f2937;
    padding: 0.75rem;
    background: #f0fdf4;
    border-left: 4px solid #22c55e;
    border-radius: 4px;
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
    transition: all 0.2s;
}

.btn:hover:not(:disabled) {
    background: #f3f4f6;
    border-color: #9ca3af;
}

.btn.primary {
    background: #4f46e5;
    color: #ffffff;
    border-color: #4f46e5;
}

.btn.primary:hover {
    background: #4338ca;
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

/* Modal Dialog Styles */
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
}

.modal-dialog {
    background: white;
    border-radius: 12px;
    padding: 2rem;
    max-width: 400px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
    animation: slideUp 0.3s ease-out;
}

.modal-dialog h3 {
    margin: 0 0 0.5rem 0;
    font-size: 1.25rem;
    color: #111827;
}

.modal-dialog p {
    margin: 0 0 1.5rem 0;
    color: #6b7280;
    line-height: 1.5;
}

.modal-actions {
    display: flex;
    gap: 0.75rem;
    justify-content: flex-end;
}

.modal-actions .btn {
    flex: 1;
}

@keyframes slideUp {
    from {
        opacity: 0;
        transform: translateY(20px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}
</style>
