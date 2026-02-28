<template>
    <section v-if="visible" class="section">
        <SectionHeader
            :title="$t('adminSettings.title')"
            icon="⚙️"
            :subtitle="$t('adminSettings.subtitle')"
        />

        <div v-if="loading" class="list-state">
            {{ $t("adminSettings.loading") }}
        </div>

        <div v-else-if="error" class="list-state list-state--error">
            {{ error }}
        </div>

        <form v-else ref="formRef" class="form" @submit.prevent="saveSettings">
            <div class="settings-group">
                <h3 class="settings-group-title">
                    🔐 {{ $t("adminSettings.idleTimeout.title") }}
                </h3>
                <p class="settings-group-description">
                    {{ $t("adminSettings.idleTimeout.description") }}
                </p>

                <div class="form-grid">
                    <div class="form-field">
                        <label for="idle-timeout">
                            {{ $t("adminSettings.idleTimeout.timeoutLabel") }}
                        </label>
                        <BaseInput
                            id="idle-timeout"
                            v-model="settings.idleTimeoutMinutes"
                            type="number"
                            :min="5"
                            :max="480"
                            :step="5"
                            :placeholder="
                                $t(
                                    'adminSettings.idleTimeout.timeoutPlaceholder',
                                )
                            "
                        />
                        <p class="field-hint">
                            {{ $t("adminSettings.idleTimeout.timeoutHint") }}
                        </p>
                    </div>

                    <div class="form-field">
                        <label for="idle-warning">
                            {{ $t("adminSettings.idleTimeout.warningLabel") }}
                        </label>
                        <BaseInput
                            id="idle-warning"
                            v-model="settings.idleWarningMinutes"
                            type="number"
                            :min="1"
                            :max="60"
                            :step="1"
                            :placeholder="
                                $t(
                                    'adminSettings.idleTimeout.warningPlaceholder',
                                )
                            "
                        />
                        <p class="field-hint">
                            {{ $t("adminSettings.idleTimeout.warningHint") }}
                        </p>
                    </div>
                </div>
            </div>

            <div class="form-actions">
                <BaseButton
                    type="submit"
                    variant="primary"
                    :loading="saving"
                    :disabled="!isDirty"
                >
                    💾 {{ $t("adminSettings.saveButton") }}
                </BaseButton>
                <BaseButton
                    type="button"
                    variant="default"
                    @click="resetForm"
                    :disabled="!isDirty"
                >
                    ❌ {{ $t("common.cancel") }}
                </BaseButton>
            </div>

            <p v-if="message" :class="['form-message', messageType]">
                {{ message }}
            </p>
        </form>
    </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "~/composables/useApi";
import SectionHeader from "~/components/base/SectionHeader.vue";
import BaseInput from "~/components/base/BaseInput.vue";
import BaseButton from "~/components/base/BaseButton.vue";

const props = defineProps<{
    visible: boolean;
}>();

const config = useRuntimeConfig();
const { $fetch } = useApi();
const { t } = useI18n();

const loading = ref(false);
const saving = ref(false);
const error = ref("");
const message = ref("");
const messageType = ref<"success" | "error">("success");
const formRef = ref<HTMLFormElement | null>(null);

const settings = ref({
    idleTimeoutMinutes: 30,
    idleWarningMinutes: 5,
});

const originalSettings = ref({
    idleTimeoutMinutes: 30,
    idleWarningMinutes: 5,
});

const isDirty = computed(() => {
    const timeoutChanged =
        Number(settings.value.idleTimeoutMinutes) !==
        Number(originalSettings.value.idleTimeoutMinutes);
    const warningChanged =
        Number(settings.value.idleWarningMinutes) !==
        Number(originalSettings.value.idleWarningMinutes);
    return timeoutChanged || warningChanged;
});

const loadSettings = async () => {
    loading.value = true;
    error.value = "";

    try {
        const data = await $fetch<any>(
            `${config.public.apiBase}/settings/idle-timeout`,
        );
        settings.value = {
            idleTimeoutMinutes: data.idleTimeoutMinutes,
            idleWarningMinutes: data.idleWarningMinutes,
        };
        originalSettings.value = { ...settings.value };
    } catch (err: any) {
        error.value = err.message || t("adminSettings.failedToLoad");
    } finally {
        loading.value = false;
    }
};

const saveSettings = async () => {
    // Validate
    if (
        settings.value.idleWarningMinutes >= settings.value.idleTimeoutMinutes
    ) {
        showMessage(t("adminSettings.idleTimeout.warningError"), "error");
        return;
    }

    saving.value = true;

    try {
        await $fetch(`${config.public.apiBase}/settings/idle-timeout`, {
            method: "PUT",
            body: {
                idleTimeoutMinutes: settings.value.idleTimeoutMinutes,
                idleWarningMinutes: settings.value.idleWarningMinutes,
            },
        });

        originalSettings.value = { ...settings.value };
        showMessage(t("adminSettings.saved"), "success");

        // Reload idle timeout configuration in the composable
        // This ensures the new settings take effect immediately
        const idleConfig = await $fetch<any>(
            `${config.public.apiBase}/settings/idle-timeout`,
        );
        // Dispatch a custom event that the idle timeout composable can listen to
        window.dispatchEvent(
            new CustomEvent("idle-timeout-updated", {
                detail: idleConfig,
            }),
        );
    } catch (err: any) {
        showMessage(err.message || t("adminSettings.failedToSave"), "error");
    } finally {
        saving.value = false;
    }
};

const resetForm = () => {
    settings.value = { ...originalSettings.value };
    message.value = "";
};

const showMessage = (msg: string, type: "success" | "error" = "success") => {
    message.value = msg;
    messageType.value = type;

    if (type === "success") {
        setTimeout(() => {
            message.value = "";
        }, 5000);
    }
};

onMounted(() => {
    if (props.visible) {
        loadSettings();
    }
});

// Watch for visibility changes
watch(
    () => props.visible,
    (newVal) => {
        if (props.visible) {
            loadSettings();
        }
    },
);
</script>

<style scoped>
.section {
    margin-top: 1.25rem;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: 1.5rem;
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

.form {
    display: flex;
    flex-direction: column;
    gap: 2rem;
}

.settings-group {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.settings-group-title {
    margin: 0;
    font-size: 1.125rem;
    font-weight: 700;
    color: #1f2937;
}

.settings-group-description {
    margin: 0;
    color: #6b7280;
    font-size: 0.875rem;
    line-height: 1.6;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 1.5rem;
    margin-top: 1rem;
}

.form-field {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.form-field label {
    font-weight: 600;
    font-size: 0.875rem;
    color: #374151;
}

.field-hint {
    margin: 0.25rem 0 0 0;
    font-size: 0.75rem;
    color: #6b7280;
    font-style: italic;
}

.form-actions {
    display: flex;
    gap: 0.75rem;
    padding-top: 1rem;
    border-top: 1px solid #e5e7eb;
}

.form-message {
    margin: 0;
    padding: 0.75rem;
    border-radius: 6px;
    font-size: 0.875rem;
    font-weight: 500;
}

.form-message.success {
    background: #f0fdf4;
    color: #166534;
    border: 1px solid #86efac;
}

.form-message.error {
    background: #fee2e2;
    color: #991b1b;
    border: 1px solid #fca5a5;
}
</style>
