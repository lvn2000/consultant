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
  <button
    type="button"
    :class="['btn', variantClass, sizeClass]"
    :disabled="disabled || loading"
    :aria-label="ariaLabel"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="btn-spinner"></span>
    <slot></slot>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  variant?: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  disabled?: boolean
  loading?: boolean
  ariaLabel?: string
}>()

defineEmits<{
  (e: 'click', event: MouseEvent): void
}>()

const variantClass = computed(() => {
  const variants: Record<string, string> = {
    primary: '',
    secondary: 'btn-secondary',
    success: 'btn-success',
    danger: 'btn-danger',
    warning: 'btn-warning',
    ghost: 'btn-ghost'
  }
  return variants[props.variant || 'primary'] || ''
})

const sizeClass = computed(() => {
  const sizes: Record<string, string> = {
    sm: 'btn-sm',
    md: '',
    lg: 'btn-lg'
  }
  return sizes[props.size || 'md'] || ''
})
</script>

<style scoped>
.btn {
  padding: 0.5rem 1rem;
  background: #4f46e5;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn:hover:not(:disabled) {
  background: #4338ca;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: #6b7280;
}

.btn-secondary:hover:not(:disabled) {
  background: #4b5563;
}

.btn-success {
  background: #10b981;
}

.btn-success:hover:not(:disabled) {
  background: #059669;
}

.btn-danger {
  background: #dc2626;
}

.btn-danger:hover:not(:disabled) {
  background: #b91c1c;
}

.btn-warning {
  background: #f59e0b;
}

.btn-warning:hover:not(:disabled) {
  background: #d97706;
}

.btn-ghost {
  background: transparent;
  color: #4f46e5;
  border: 1px solid #4f46e5;
}

.btn-ghost:hover:not(:disabled) {
  background: #4f46e5;
  color: white;
}

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.75rem;
}

.btn-lg {
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
