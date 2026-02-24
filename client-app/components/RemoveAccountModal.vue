<template>
    <div v-if="show" class="modal-overlay" @click.self="$emit('close')">
        <div class="modal">
            <div class="modal-header">
                <span class="modal-icon">⚠️</span>
                <h3>{{ $t("profile.removeAccount") }}</h3>
            </div>

            <div class="warning-box">
                <p class="warning-title">❗ {{ $t("profile.warningTitle") }}</p>
                <ul class="warning-list">
                    <li>{{ $t("profile.warningDataLoss") }}</li>
                    <li>{{ $t("profile.warningConsultations") }}</li>
                    <li>{{ $t("profile.warningConnections") }}</li>
                    <li>{{ $t("profile.warning irreversible") }}</li>
                </ul>
            </div>

            <p class="confirm-text">{{ $t("profile.removeAccountConfirm") }}</p>

            <div class="modal-actions">
                <button
                    class="btn danger"
                    @click="removeAccount"
                    :disabled="removing"
                >
                    {{
                        removing
                            ? $t("common.removing")
                            : $t("profile.yesRemoveAccount")
                    }}
                </button>
                <button
                    class="btn btn-default"
                    @click="$emit('close')"
                    :disabled="removing"
                    autofocus
                >
                    {{ $t("common.cancel") }}
                </button>
            </div>

            <div
                v-if="removeMessage"
                :class="['form-message', removeSuccess ? 'success' : 'error']"
            >
                {{ removeMessage }}
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useRouter } from "vue-router";
import { useApi } from "../composables/useApi";

const { t } = useI18n();
const router = useRouter();
const config = useRuntimeConfig();
const { $fetch } = useApi();

interface Props {
    show: boolean;
}

defineProps<Props>();
defineEmits(["close", "removed"]);

const removing = ref(false);
const removeMessage = ref("");
const removeSuccess = ref(false);

const removeAccount = async () => {
    removing.value = true;
    removeMessage.value = "";

    try {
        const userId = sessionStorage.getItem("userId");
        if (!userId) {
            throw new Error(t("auth.userIdNotFound"));
        }

        await $fetch(`${config.public.apiBase}/users/${userId}`, {
            method: "DELETE",
        });

        removeSuccess.value = true;
        removeMessage.value = t("profile.accountRemoved");

        // Clear session and redirect to login after delay
        setTimeout(async () => {
            sessionStorage.removeItem("accessToken");
            sessionStorage.removeItem("sessionId");
            sessionStorage.removeItem("userId");
            sessionStorage.removeItem("login");
            sessionStorage.removeItem("email");
            sessionStorage.removeItem("role");
            localStorage.removeItem("admin_session");

            // Emit event for parent to handle
            emit("removed");

            // Redirect to login
            router.push("/login");
        }, 2000);
    } catch (error: any) {
        removeSuccess.value = false;
        removeMessage.value =
            error.data?.message || error.message || t("profile.failedToRemove");
    } finally {
        removing.value = false;
    }
};
</script>

<style scoped>
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.6);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
    from {
        opacity: 0;
    }
    to {
        opacity: 1;
    }
}

.modal {
    background: white;
    padding: 2rem;
    border-radius: 12px;
    max-width: 550px;
    width: 90%;
    max-height: 90vh;
    overflow-y: auto;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.modal-header {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 1.5rem;
}

.modal-icon {
    font-size: 2rem;
}

.modal h3 {
    margin: 0;
    color: #1f2937;
    font-size: 1.5rem;
}

.warning-box {
    background: #fef3c7;
    border: 1px solid #fcd34d;
    border-radius: 8px;
    padding: 1rem;
    margin-bottom: 1.5rem;
}

.warning-title {
    margin: 0 0 0.75rem 0;
    color: #92400e;
    font-weight: 600;
    font-size: 1rem;
}

.warning-list {
    margin: 0;
    padding-left: 1.5rem;
    color: #78350f;
    font-size: 0.9rem;
}

.warning-list li {
    margin-bottom: 0.5rem;
}

.confirm-text {
    color: #4b5563;
    margin: 1rem 0;
    font-size: 0.95rem;
    line-height: 1.5;
}

.confirm-input {
    margin: 1.5rem 0;
}

.confirm-input label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 500;
    color: #374151;
    font-size: 0.9rem;
}

.confirm-input input {
    width: 100%;
    padding: 0.75rem;
    border: 2px solid #d1d5db;
    border-radius: 6px;
    font-size: 1rem;
    font-family: monospace;
    font-weight: 600;
    text-transform: uppercase;
    box-sizing: border-box;
}

.confirm-input input:focus {
    outline: none;
    border-color: #dc2626;
    box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.1);
}

.modal-actions {
    display: flex;
    gap: 1rem;
    justify-content: flex-end;
    margin-top: 1.5rem;
}

.btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 6px;
    font-size: 0.95rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s;
    background: #6b7280;
    color: white;
}

.btn-default {
    background: #4f46e5;
    border: 2px solid #4f46e5;
}

.btn-default:hover:not(:disabled) {
    background: #4338ca;
    border-color: #4338ca;
}

.btn:hover:not(:disabled) {
    background: #4b5563;
}

.btn.danger {
    background: #dc2626;
}

.btn.danger:hover:not(:disabled) {
    background: #b91c1c;
}

.btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.form-message {
    padding: 1rem;
    border-radius: 6px;
    margin-top: 1.5rem;
    font-weight: 500;
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
