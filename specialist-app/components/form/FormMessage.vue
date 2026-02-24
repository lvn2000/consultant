<template>
    <div :class="['form-message', variant]">
        <slot>{{ message }}</slot>
    </div>
</template>

<script setup lang="ts">
defineProps<{
    message?: string;
    variant?: "success" | "error" | "warning" | "info";
}>();
</script>

<style scoped>
.form-message {
    padding: 0.75rem;
    border-radius: 6px;
    font-size: 0.875rem;
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

.form-message.warning {
    background: #fef3c7;
    color: #92400e;
    border-left: 4px solid #f59e0b;
}

.form-message.info {
    background: #dbeafe;
    color: #1e40af;
    border-left: 4px solid #3b82f6;
}
</style>
