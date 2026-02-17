<template>
    <section v-if="visible" class="section">
        <div class="section-header">
            <h2>{{ $t("adminConnectionTypes.title") }}</h2>
            <button type="button" class="btn" @click="loadConnectionTypes">
                🔄 Refresh
            </button>
        </div>

        <div class="list-state" v-if="connectionTypesLoading">
            {{ $t("adminConnectionTypes.loading") }}
        </div>
        <div class="list-state error" v-else-if="connectionTypesError">
            {{ connectionTypesError }}
        </div>

        <div class="table" v-else>
            <div class="table-header connections-types-table">
                <span>{{ $t("common.name") }}</span>
                <span>{{ $t("common.description") }}</span>
                <span>{{ $t("common.actions") }}</span>
            </div>
            <div
                v-for="type in connectionTypes"
                :key="type.id"
                class="table-row connections-types-table"
            >
                <span>{{ type.name }}</span>
                <span>{{ type.description || "-" }}</span>
                <span class="row-actions">
                    <button
                        type="button"
                        class="btn"
                        @click="startEditConnectionType(type)"
                    >
                        ✏️ {{ $t("common.select") }}
                    </button>
                    <button
                        type="button"
                        class="btn danger"
                        @click="removeConnectionType(type.id)"
                    >
                        🗑️ {{ $t("common.delete") }}
                    </button>
                </span>
            </div>
        </div>

        <form ref="connectionTypeFormRef" class="form" @submit.prevent>
            <div class="form-grid">
                <div class="form-field">
                    <label for="connection-type-name">{{
                        $t("common.name")
                    }}</label>
                    <input
                        id="connection-type-name"
                        v-model="connectionTypeForm.name"
                        type="text"
                        :placeholder="
                            $t('adminConnectionTypes.namePlaceholder')
                        "
                    />
                </div>
                <div class="form-field">
                    <label for="connection-type-description">{{
                        $t("common.description")
                    }}</label>
                    <input
                        id="connection-type-description"
                        v-model="connectionTypeForm.description"
                        type="text"
                        :placeholder="
                            $t('adminConnectionTypes.descriptionPlaceholder')
                        "
                    />
                </div>
            </div>

            <div class="form-actions">
                <button
                    type="button"
                    class="btn primary"
                    @click="addConnectionType"
                >
                    ➕ {{ $t("adminConnectionTypes.addType") }}
                </button>
                <button
                    type="button"
                    class="btn"
                    :disabled="!selectedConnectionTypeId"
                    @click="updateConnectionType"
                >
                    ✏️ {{ $t("adminConnectionTypes.updateType") }}
                </button>
                <button
                    type="button"
                    class="btn danger"
                    :disabled="!selectedConnectionTypeId"
                    @click="deleteSelectedConnectionType"
                >
                    🗑️ {{ $t("adminConnectionTypes.deleteType") }}
                </button>
                <button
                    type="button"
                    class="btn"
                    @click="resetConnectionTypeForm"
                >
                    ❌ {{ $t("common.cancel") }}
                </button>
            </div>

            <p v-if="connectionTypeActionMessage" class="form-message">
                {{ connectionTypeActionMessage }}
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
                        {{ $t("common.saving") }}
                    </button>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { onMounted, ref, nextTick } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "../composables/useApi";

const { t } = useI18n();

defineProps<{
    visible: boolean;
}>();

const config = useRuntimeConfig();
const { $fetch } = useApi();

type ConnectionType = {
    id: string;
    name: string;
    description: string | null;
};

const connectionTypes = ref<ConnectionType[]>([]);
const connectionTypesLoading = ref(false);
const connectionTypesError = ref("");
const selectedConnectionTypeId = ref<string | null>(null);
const connectionTypeActionMessage = ref("");
const connectionTypeFormRef = ref<HTMLFormElement | null>(null);
const connectionTypeForm = ref({
    name: "",
    description: "",
});

const confirmState = ref({
    visible: false,
    title: "",
    message: "",
});
const confirmResolver = ref<((value: boolean) => void) | null>(null);

const confirmAction = (title: string, message: string) =>
    new Promise<boolean>((resolve) => {
        confirmState.value = { visible: true, title, message };
        confirmResolver.value = (confirmed: boolean) => {
            resolve(confirmed);
            confirmState.value.visible = false;
        };
    });

const loadConnectionTypes = async () => {
    connectionTypesLoading.value = true;
    connectionTypesError.value = "";

    try {
        const data = await $fetch<ConnectionType[]>(
            `${config.public.apiBase}/connection-types`,
            {
                method: "GET",
            },
        );
        connectionTypes.value = data;
    } catch (error) {
        connectionTypes.value = [];
        connectionTypesError.value = t("adminConnectionTypes.failedToLoad");
    } finally {
        connectionTypesLoading.value = false;
    }
};

const startEditConnectionType = (type: ConnectionType) => {
    selectedConnectionTypeId.value = type.id;
    connectionTypeForm.value = {
        name: type.name,
        description: type.description ?? "",
    };
    connectionTypeActionMessage.value = "";
    // Scroll to form and focus
    nextTick(() => {
        if (connectionTypeFormRef.value) {
            connectionTypeFormRef.value.scrollIntoView({
                behavior: "smooth",
                block: "start",
            });
            const firstInput = connectionTypeFormRef.value.querySelector(
                "input",
            ) as HTMLInputElement;
            if (firstInput) {
                setTimeout(() => firstInput.focus(), 300);
            }
        }
    });
};

const resetConnectionTypeForm = () => {
    connectionTypeForm.value = { name: "", description: "" };
    selectedConnectionTypeId.value = null;
};

const addConnectionType = async () => {
    connectionTypeActionMessage.value = "";

    if (!connectionTypeForm.value.name.trim()) {
        connectionTypeActionMessage.value = t(
            "adminConnectionTypes.nameRequired",
        );
        return;
    }

    const confirmed = await confirmAction(
        t("adminConnectionTypes.addTitle"),
        t("adminConnectionTypes.addConfirm"),
    );
    if (!confirmed) return;

    try {
        await $fetch(`${config.public.apiBase}/connection-types`, {
            method: "POST",
            body: {
                name: connectionTypeForm.value.name,
                description: connectionTypeForm.value.description || null,
            },
        });
        connectionTypeActionMessage.value = t("adminConnectionTypes.created");
        resetConnectionTypeForm();
        await loadConnectionTypes();
    } catch (error) {
        connectionTypeActionMessage.value = t(
            "adminConnectionTypes.failedToCreate",
        );
    }
};

const updateConnectionType = async () => {
    if (!selectedConnectionTypeId.value) return;

    const confirmed = await confirmAction(
        t("adminConnectionTypes.updateTitle"),
        t("adminConnectionTypes.updateConfirm"),
    );
    if (!confirmed) return;

    try {
        await $fetch(
            `${config.public.apiBase}/connection-types/${selectedConnectionTypeId.value}`,
            {
                method: "PUT",
                body: {
                    name: connectionTypeForm.value.name,
                    description: connectionTypeForm.value.description,
                },
            },
        );
        connectionTypeActionMessage.value = t("adminConnectionTypes.updated");
        resetConnectionTypeForm();
        await loadConnectionTypes();
    } catch (error) {
        connectionTypeActionMessage.value = t(
            "adminConnectionTypes.failedToUpdate",
        );
    }
};

const deleteSelectedConnectionType = async () => {
    if (!selectedConnectionTypeId.value) return;

    const confirmed = await confirmAction(
        t("adminConnectionTypes.deleteTitle"),
        t("adminConnectionTypes.deleteConfirm"),
    );
    if (!confirmed) return;

    try {
        await $fetch(
            `${config.public.apiBase}/connection-types/${selectedConnectionTypeId.value}`,
            {
                method: "DELETE",
            },
        );
        connectionTypeActionMessage.value = t("adminConnectionTypes.deleted");
        resetConnectionTypeForm();
        await loadConnectionTypes();
    } catch (error) {
        connectionTypeActionMessage.value = t(
            "adminConnectionTypes.failedToDelete",
        );
    }
};

const removeConnectionType = async (id: string) => {
    const confirmed = await confirmAction(
        t("adminConnectionTypes.deleteTitle"),
        t("adminConnectionTypes.deleteConfirm"),
    );
    if (!confirmed) return;

    try {
        await $fetch(`${config.public.apiBase}/connection-types/${id}`, {
            method: "DELETE",
        });
        await loadConnectionTypes();
    } catch (error) {
        connectionTypeActionMessage.value = t(
            "adminConnectionTypes.failedToDelete",
        );
    }
};

onMounted(() => {
    loadConnectionTypes();
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

.table-header.connections-types-table,
.table-row.connections-types-table {
    grid-template-columns: 1fr 2fr 1.2fr;
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
