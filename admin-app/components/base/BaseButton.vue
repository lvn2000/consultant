<template>
  <button
    :type="type"
    :class="[
      'btn',
      `btn--${variant}`,
      `btn--${size}`,
      { 'btn--loading': loading, 'btn--full-width': fullWidth },
    ]"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="btn__spinner" />
    <slot />
  </button>
</template>

<script setup lang="ts">
export type ButtonVariant = 'default' | 'primary' | 'danger' | 'success' | 'warning'
export type ButtonSize = 'small' | 'medium' | 'large'

defineProps<{
  type?: 'button' | 'submit' | 'reset'
  variant?: ButtonVariant
  size?: ButtonSize
  disabled?: boolean
  loading?: boolean
  fullWidth?: boolean
}>()

defineEmits<{
  click: [event: MouseEvent]
}>()
</script>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 1px solid transparent;
  border-radius: 6px;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn--default {
  background: #ffffff;
  color: #1f2937;
  border-color: #cbd5f5;
}

.btn--default:hover:not(:disabled) {
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

.btn--danger {
  background: #fee2e2;
  color: #b91c1c;
  border-color: #fecaca;
}

.btn--danger:hover:not(:disabled) {
  background: #fca5a5;
}

.btn--success {
  background: #dcfce7;
  color: #166534;
  border-color: #bbf7d0;
}

.btn--success:hover:not(:disabled) {
  background: #86efac;
}

.btn--warning {
  background: #fef3c7;
  color: #92400e;
  border-color: #fde68a;
}

.btn--warning:hover:not(:disabled) {
  background: #fcd34d;
}

.btn--small {
  padding: 0.25rem 0.625rem;
  font-size: 0.75rem;
}

.btn--medium {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
}

.btn--large {
  padding: 0.75rem 1.25rem;
  font-size: 1rem;
}

.btn--full-width {
  width: 100%;
}

.btn__spinner {
  width: 1rem;
  height: 1rem;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spin 0.75s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
