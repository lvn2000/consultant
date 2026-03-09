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
            icon="🤝"
            :title="$t('connections.title')"
            :subtitle="$t('connections.subtitle')"
        >
            <template #actions>
                <AppButton @click="startEditConnection(null)">
                    <span class="btn-icon">+</span>
                    {{ $t("connections.addConnection") }}
                </AppButton>
            </template>
        </SectionHeader>

        <LoadingState
            v-if="connectionsLoading"
            :message="$t('connections.loading')"
            :show-spinner="true"
        />
        <ErrorState v-else-if="connectionsError" :message="connectionsError" />

        <EmptyState
            v-else-if="connections.length === 0"
            icon="📱"
            :title="$t('connections.noConnections')"
            :description="$t('connections.noConnectionsDesc')"
        >
            <template #action>
                <AppButton @click="startEditConnection(null)">
                    <span class="btn-icon">+</span>
                    {{ $t("connections.addConnection") }}
                </AppButton>
            </template>
        </EmptyState>

        <div v-else class="connections-list">
            <div
                class="connection-card"
                v-for="connection in connections"
                :key="connection.id"
            >
                <div class="connection-header">
                    <Badge variant="purple">
                        {{ getConnectionTypeName(connection.connectionTypeId) }}
                    </Badge>
                    <div
                        class="connection-status"
                        :class="{ verified: connection.isVerified }"
                    >
                        <span class="status-dot"></span>
                        {{
                            connection.isVerified
                                ? $t("common.verified")
                                : $t("common.notVerified")
                        }}
                    </div>
                </div>
                <div class="connection-value">
                    {{ connection.connectionValue }}
                </div>
                <div class="connection-actions">
                    <AppButton
                        variant="secondary"
                        size="sm"
                        @click="startEditConnection(connection)"
                    >
                        ✏️ {{ $t("common.edit") }}
                    </AppButton>
                    <AppButton
                        variant="danger"
                        size="sm"
                        @click="removeConnection(connection.id)"
                    >
                        🗑️ {{ $t("common.delete") }}
                    </AppButton>
                </div>
            </div>
        </div>

        <!-- Connection Form (shown when editing or adding) -->
        <div v-if="editingConnectionId" class="connection-form">
            <h3>
                {{
                    editingConnectionId === "new"
                        ? $t("connections.addConnection")
                        : $t("connections.editConnection")
                }}
            </h3>
            <FormGrid>
                <div class="form-field">
                    <label for="connection-type">{{
                        $t("connections.connectionType")
                    }}</label>
                    <select
                        id="connection-type"
                        v-model="connectionForm.connectionTypeId"
                        class="form-select"
                        required
                    >
                        <option value="">
                            {{ $t("connections.selectConnectionType") }}
                        </option>
                        <option
                            v-for="type in connectionTypes"
                            :key="type.id"
                            :value="type.id"
                        >
                            {{ type.name
                            }}{{
                                type.description ? " - " + type.description : ""
                            }}
                        </option>
                    </select>
                </div>
                <div class="form-field">
                    <label for="connection-value">{{
                        $t("connections.connectionValue")
                    }}</label>
                    <input
                        id="connection-value"
                        type="text"
                        v-model="connectionForm.connectionValue"
                        :placeholder="$t('connections.valuePlaceholder')"
                        class="form-input"
                        required
                    />
                </div>
            </FormGrid>
            <FormActions>
                <AppButton
                    :loading="connectionSaving"
                    :disabled="
                        !connectionForm.connectionTypeId ||
                        !connectionForm.connectionValue
                    "
                    @click="saveConnection"
                >
                    {{
                        connectionSaving
                            ? $t("common.saving")
                            : $t("connections.saveConnection")
                    }}
                </AppButton>
                <AppButton variant="secondary" @click="cancelEditConnection">
                    {{ $t("common.cancel") }}
                </AppButton>
            </FormActions>
            <div
                v-if="connectionMessage"
                :class="[
                    'form-message',
                    connectionSuccess ? 'success' : 'error',
                ]"
            >
                {{ connectionMessage }}
            </div>
        </div>
    </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "~/composables/useApi";

import SectionHeader from "~/components/base/SectionHeader.vue";
import LoadingState from "~/components/base/LoadingState.vue";
import ErrorState from "~/components/base/ErrorState.vue";
import EmptyState from "~/components/base/EmptyState.vue";
import Badge from "~/components/ui/Badge.vue";
import AppButton from "~/components/ui/AppButton.vue";
import FormGrid from "~/components/form/FormGrid.vue";
import FormActions from "~/components/form/FormActions.vue";

const config = useRuntimeConfig();
const { $fetch } = useApi();
const { t } = useI18n();

type Connection = {
    id: string;
    connectionTypeId: string;
    connectionValue: string;
    isVerified: boolean;
};

type ConnectionType = {
    id: string;
    name: string;
    description?: string | null;
};

const connectionsLoading = ref(false);
const connectionsError = ref("");
const connections = ref<Connection[]>([]);
const connectionTypes = ref<ConnectionType[]>([]);
const connectionForm = ref({
    connectionTypeId: "",
    connectionValue: "",
    isVerified: false,
});
const editingConnectionId = ref<string | null>(null);
const connectionSaving = ref(false);
const connectionMessage = ref("");
const connectionSuccess = ref(false);

const loadConnections = async () => {
    connectionsLoading.value = true;
    connectionsError.value = "";
    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            connectionsError.value = t("auth.userIdNotFound");
            return;
        }
        const connectionsData = await $fetch<Connection[]>(
            `${config.public.apiBase}/specialists/${userId}/connections`,
        );
        connections.value = connectionsData || [];
        const typesData = await $fetch<ConnectionType[]>(
            `${config.public.apiBase}/connection-types`,
        );
        connectionTypes.value = typesData || [];
    } catch (error: any) {
        connectionsError.value = error.message || t("connections.failedToLoad");
    } finally {
        connectionsLoading.value = false;
    }
};

const startEditConnection = (connection: Connection | null) => {
    if (connection) {
        connectionForm.value = {
            connectionTypeId: connection.connectionTypeId,
            connectionValue: connection.connectionValue,
            isVerified: connection.isVerified,
        };
        editingConnectionId.value = connection.id;
    } else {
        connectionForm.value = {
            connectionTypeId: "",
            connectionValue: "",
            isVerified: false,
        };
        editingConnectionId.value = "new";
    }
    connectionMessage.value = "";
};

const cancelEditConnection = () => {
    connectionForm.value = {
        connectionTypeId: "",
        connectionValue: "",
        isVerified: false,
    };
    editingConnectionId.value = null;
    connectionMessage.value = "";
};

const saveConnection = async () => {
    connectionSaving.value = true;
    connectionMessage.value = "";
    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            connectionMessage.value = t("auth.userIdNotFound");
            connectionSuccess.value = false;
            return;
        }

        if (editingConnectionId.value && editingConnectionId.value !== "new") {
            await $fetch(
                `${config.public.apiBase}/specialists/${userId}/connections/${editingConnectionId.value}`,
                {
                    method: "PUT",
                    body: connectionForm.value,
                },
            );
            connectionMessage.value = t("connections.connectionUpdated");
        } else {
            await $fetch(
                `${config.public.apiBase}/specialists/${userId}/connections`,
                {
                    method: "POST",
                    body: connectionForm.value,
                },
            );
            connectionMessage.value = t("connections.connectionAdded");
        }

        connectionSuccess.value = true;
        setTimeout(() => {
            cancelEditConnection();
            loadConnections();
        }, 1500);
    } catch (error: any) {
        connectionMessage.value =
            error.message || t("connections.failedToSave");
        connectionSuccess.value = false;
    } finally {
        connectionSaving.value = false;
    }
};

const removeConnection = async (connectionId: string) => {
    if (!confirm(t("connections.confirmRemove"))) return;
    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            alert(t("auth.userIdNotFound"));
            return;
        }
        await $fetch(
            `${config.public.apiBase}/specialists/${userId}/connections/${connectionId}`,
            {
                method: "DELETE",
            },
        );
        await loadConnections();
    } catch (error: any) {
        alert(error.message || t("connections.failedToRemove"));
    }
};

const getConnectionTypeName = (typeId: string) => {
    const type = connectionTypes.value.find((t) => t.id === typeId);
    return type ? type.name : t("common.unknown");
};

onMounted(() => {
    loadConnections();
});

defineExpose({ loadConnections });
</script>

<style scoped>
.section {
    margin-top: 2rem;
}

.connections-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 1rem;
}

.connection-card {
    background: white;
    border-radius: 12px;
    padding: 1rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    transition: all 0.2s;
    border: 2px solid transparent;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.connection-card:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    border-color: #e0e7ff;
    transform: translateY(-2px);
}

.connection-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.5rem;
}

.connection-status {
    display: flex;
    align-items: center;
    gap: 0.375rem;
    font-size: 0.75rem;
    color: #ef4444;
    font-weight: 500;
}

.connection-status.verified {
    color: #10b981;
}

.status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: currentColor;
}

.connection-value {
    font-size: 1.125rem;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 0.5rem;
    word-break: break-all;
}

.connection-actions {
    display: flex;
    gap: 0.75rem;
}

.btn-icon {
    font-size: 1.125rem;
    margin-right: 0.25rem;
}

.connection-form {
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    margin-top: 1rem;
}

.connection-form h3 {
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

.form-select,
.form-input {
    padding: 0.625rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.875rem;
    background: white;
    color: #1f2937;
}

.form-select:focus,
.form-input:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-message {
    padding: 0.75rem;
    border-radius: 6px;
    font-size: 0.875rem;
    margin-top: 1rem;
}

.form-message.success {
    background: #d1fae5;
    color: #065f46;
    border-left: 4px solid #059669;
}

.form-message.error {
    background: #fee2e2;
    color: #dc2626;
    border-left: 4px solid #dc2626;
}
</style>
