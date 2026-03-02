<template>
    <section v-if="visible" class="section">
        <SectionHeader
            :title="$t('adminConnectionTypes.title')"
            show-refresh
            @refresh="fetchConnectionTypes"
        />

        <div v-if="loading" class="list-state">
            {{ $t("adminConnectionTypes.loading") }}
        </div>

        <div v-else-if="error" class="list-state list-state--error">
            {{ error }}
        </div>

        <BaseTable
            v-else
            :columns="[
                $t('common.name'),
                $t('common.description'),
                $t('common.actions'),
            ]"
            :items="items"
            :column-widths="['1fr', '2fr', '1.2fr']"
            :selected-id="selectedId"
            :empty-message="$t('adminConnectionTypes.noTypes')"
        >
            <template #row="{ item }">
                <span>{{ item.name }}</span>
                <span>{{ item.description || "-" }}</span>
                <span class="row-actions">
                    <button
                        type="button"
                        class="btn"
                        @click="selectConnectionType(item)"
                    >
                        ✏️ {{ $t("common.select") }}
                    </button>
                    <button
                        type="button"
                        class="btn danger"
                        @click="handleDelete(item.id)"
                    >
                        🗑️ {{ $t("common.delete") }}
                    </button>
                </span>
            </template>
        </BaseTable>

        <form ref="formRef" class="form" @submit.prevent>
            <div class="form-grid">
                <BaseInput
                    id="connection-type-name"
                    v-model="form.name"
                    :label="$t('common.name')"
                    :placeholder="$t('adminConnectionTypes.namePlaceholder')"
                    :required="true"
                />
                <BaseInput
                    id="connection-type-description"
                    v-model="form.description"
                    :label="$t('common.description')"
                    :placeholder="
                        $t('adminConnectionTypes.descriptionPlaceholder')
                    "
                />
            </div>

            <div class="form-actions">
                <BaseButton variant="primary" @click="handleCreate">
                    ➕ {{ $t("adminConnectionTypes.addType") }}
                </BaseButton>
                <BaseButton :disabled="!selectedId" @click="handleUpdate">
                    ✏️ {{ $t("adminConnectionTypes.updateType") }}
                </BaseButton>
                <BaseButton variant="default" @click="onClearClick">
                    ❌ {{ $t("common.clear") }}
                </BaseButton>
            </div>

            <p v-if="message" :class="['form-message', messageType]">
                {{ message }}
            </p>
        </form>

        <!-- Confirmation Dialog -->
        <Teleport to="body">
            <Transition name="fade">
                <div
                    v-if="confirmState.visible"
                    class="modal-overlay"
                    @click.self="cancelConfirm"
                >
                    <div class="modal-dialog">
                        <h3>{{ confirmState.title }}</h3>
                        <p>{{ confirmState.message }}</p>
                        <div class="modal-actions">
                            <BaseButton
                                variant="default"
                                @click="cancelConfirm"
                            >
                                {{ $t("common.cancel") }}
                            </BaseButton>
                            <BaseButton variant="primary" @click="confirm">
                                {{ $t("common.confirm") }}
                            </BaseButton>
                        </div>
                    </div>
                </div>
            </Transition>
        </Teleport>
    </section>
</template>

<script setup lang="ts">
import { useConnectionTypesStore } from "~/stores/connectionTypes";
import type { ConnectionType } from "~/types/api";
import SectionHeader from "~/components/base/SectionHeader.vue";

const props = defineProps<{
    visible: boolean;
}>();

const { t } = useI18n();

// Initialize store immediately (not lazily) to avoid composition API issues
const store = useConnectionTypesStore();

// Use store state
const items = computed(() => store.items);
const loading = computed(() => store.loading);
const error = computed(() => store.error);
const selectedId = ref<string | null>(null);
const message = ref("");
const messageType = ref<"success" | "error">("success");
const formRef = ref<HTMLFormElement | null>(null);

const form = ref({
    name: "",
    description: "",
});

// Confirmation state
const confirmState = ref({ visible: false, title: "", message: "" });
const confirmResolver = ref<((value: boolean) => void) | null>(null);

const fetchConnectionTypes = async () => {
    await store.fetchConnectionTypes();
};

const selectConnectionType = (type: ConnectionType) => {
    selectedId.value = type.id;
    form.value = {
        name: type.name,
        description: type.description ?? "",
    };
    message.value = "";
    scrollToForm();
};

const resetForm = (clearMessage: boolean = true) => {
    selectedId.value = null;
    form.value = { name: "", description: "" };
    if (clearMessage) {
        message.value = "";
    }
};

const onClearClick = () => {
    resetForm();
};

const scrollToForm = () => {
    nextTick(() => {
        if (formRef.value) {
            formRef.value.scrollIntoView({
                behavior: "smooth",
                block: "start",
            });
            const firstInput = formRef.value.querySelector(
                "input",
            ) as HTMLInputElement;
            if (firstInput) {
                setTimeout(() => firstInput.focus(), 300);
            }
        }
    });
};

const showMessage = (msg: string, type: "success" | "error" = "success") => {
    message.value = msg;
    messageType.value = type;
};

const confirmDialog = (title: string, msg: string): Promise<boolean> => {
    confirmState.value = { visible: true, title, message: msg };
    return new Promise((resolve) => {
        confirmResolver.value = (confirmed: boolean) => {
            resolve(confirmed);
            confirmState.value.visible = false;
            confirmResolver.value = null;
        };
    });
};

const cancelConfirm = () => {
    confirmResolver.value?.(false);
    confirmState.value.visible = false;
    confirmResolver.value = null;
};

const confirm = () => {
    confirmResolver.value?.(true);
    confirmState.value.visible = false;
    confirmResolver.value = null;
};

const handleCreate = async () => {
    if (!form.value.name.trim()) {
        showMessage(t("adminConnectionTypes.nameRequired"), "error");
        return;
    }

    const confirmed = await confirmDialog(
        t("adminConnectionTypes.addTitle"),
        t("adminConnectionTypes.addConfirm"),
    );

    if (!confirmed) return;

    const result = await store.createConnectionType({
        name: form.value.name,
        description: form.value.description || null,
    });

    if (result.success) {
        showMessage(t("adminConnectionTypes.created"));
        resetForm();
    } else {
        showMessage(
            result.error || t("adminConnectionTypes.failedToCreate"),
            "error",
        );
    }
};

const handleUpdate = async () => {
    if (!selectedId.value) return;

    const confirmed = await confirmDialog(
        t("adminConnectionTypes.updateTitle"),
        t("adminConnectionTypes.updateConfirm"),
    );

    if (!confirmed) return;

    const result = await store.updateConnectionType(selectedId.value, {
        name: form.value.name,
        description: form.value.description,
    });

    if (result.success) {
        showMessage(t("adminConnectionTypes.updated"));
        resetForm(false); // Don't clear the message so user can see it
    } else {
        showMessage(
            result.error || t("adminConnectionTypes.failedToUpdate"),
            "error",
        );
    }
};

const handleDeleteSelected = async () => {
    if (!selectedId.value) return;
    await handleDelete(selectedId.value);
};

const handleDelete = async (id: string) => {
    const confirmed = await confirmDialog(
        t("adminConnectionTypes.deleteTitle"),
        t("adminConnectionTypes.deleteConfirm"),
    );

    if (!confirmed) return;

    const result = await store.deleteConnectionType(id);

    if (result.success) {
        showMessage(t("adminConnectionTypes.deleted"));
        if (selectedId.value === id) {
            resetForm();
        }
    } else {
        showMessage(
            result.error || t("adminConnectionTypes.failedToDelete"),
            "error",
        );
    }
};

// Initialize
onMounted(() => {
    fetchConnectionTypes();
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

.list-state {
    padding: 0.75rem 1rem;
    border-radius: 6px;
    background: #f8fafc;
    color: #475569;
    margin-bottom: 1rem;
}

.list-state--error {
    background: #fee2e2;
    color: #b91c1c;
}

.row-actions {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.form {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
    margin-top: 1.5rem;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 1rem;
}

.form-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
}

.form-message {
    margin-top: 0.5rem;
    padding: 0.75rem;
    border-radius: 4px;
    font-size: 0.875rem;
}

.form-message.success {
    background: #f0fdf4;
    color: #166534;
    border-left: 4px solid #22c55e;
}

.form-message.error {
    background: #fee2e2;
    color: #b91c1c;
    border-left: 4px solid #dc2626;
}

/* Modal */
.modal-overlay {
    position: fixed;
    inset: 0;
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

/* Transitions */
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

/* Button Styles - matching CategoriesSection */
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

.btn--primary {
    background: #4f46e5;
    color: #ffffff;
    border-color: #4f46e5;
}

.btn--primary:hover:not(:disabled) {
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
</style>
