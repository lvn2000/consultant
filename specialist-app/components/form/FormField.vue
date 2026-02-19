<template>
    <div class="form-field">
        <label v-if="label" :for="inputId">{{ label }}</label>

        <template v-if="type === 'select'">
            <select
                :id="inputId"
                :value="String(modelValue ?? '')"
                :required="required"
                :disabled="disabled"
                :class="['form-select', { 'has-error': error }]"
                @change="
                    $emit(
                        'update:modelValue',
                        ($event.target as HTMLSelectElement).value,
                    )
                "
            >
                <slot></slot>
            </select>
        </template>

        <template v-else-if="type === 'textarea'">
            <textarea
                :id="inputId"
                :value="String(modelValue ?? '')"
                :placeholder="placeholder"
                :required="required"
                :disabled="disabled"
                :rows="rows"
                :class="['form-textarea', { 'has-error': error }]"
                @input="
                    $emit(
                        'update:modelValue',
                        ($event.target as HTMLTextAreaElement).value,
                    )
                "
            ></textarea>
        </template>

        <template v-else>
            <input
                :id="inputId"
                :value="String(modelValue ?? '')"
                :type="type || 'text'"
                :placeholder="placeholder"
                :required="required"
                :disabled="disabled"
                :min="min"
                :max="max"
                :step="step"
                :class="['form-input', { 'has-error': error }]"
                @input="
                    $emit(
                        'update:modelValue',
                        ($event.target as HTMLInputElement).value,
                    )
                "
            />
        </template>

        <p v-if="error" class="form-error">{{ error }}</p>
        <p v-if="hint && !error" class="form-hint">{{ hint }}</p>
    </div>
</template>

<script setup lang="ts">
defineProps<{
    modelValue?: string | number | boolean;
    label?: string;
    type?:
        | "text"
        | "email"
        | "password"
        | "number"
        | "tel"
        | "time"
        | "date"
        | "select"
        | "textarea";
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    min?: string | number;
    max?: string | number;
    step?: string | number;
    rows?: number;
    error?: string;
    hint?: string;
    inputId?: string;
}>();

defineEmits<{
    (e: "update:modelValue", value: string): void;
}>();
</script>

<style scoped>
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

.form-input,
.form-select,
.form-textarea {
    padding: 0.625rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.875rem;
    transition:
        border-color 0.15s,
        box-shadow 0.15s;
    background: white;
    color: #1f2937;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input.has-error,
.form-select.has-error,
.form-textarea.has-error {
    border-color: #dc2626;
}

.form-input.has-error:focus,
.form-select.has-error:focus,
.form-textarea.has-error:focus {
    box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.1);
}

.form-textarea {
    resize: vertical;
    min-height: 80px;
}

.form-error {
    color: #dc2626;
    font-size: 0.875rem;
    margin-top: 0.25rem;
}

.form-hint {
    color: #6b7280;
    font-size: 0.75rem;
    margin-top: 0.25rem;
}
</style>
