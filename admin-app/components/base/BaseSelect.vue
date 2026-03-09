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
  <div class="select-wrapper">
    <label v-if="label" :for="id" class="select-label">
      {{ label }}
      <span v-if="required" class="required">*</span>
    </label>
    <select
      :id="id"
      :value="modelValue"
      :disabled="disabled"
      :required="required"
      :class="['select', { 'select--error': error }]"
      @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
      @blur="$emit('blur', $event)"
    >
      <option v-if="placeholder" value="" disabled selected>
        {{ placeholder }}
      </option>
      <slot />
    </select>
    <span v-if="error" class="select-error">{{ error }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  id?: string
  label?: string
  modelValue?: string | number
  placeholder?: string
  disabled?: boolean
  required?: boolean
  error?: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
  blur: [event: FocusEvent]
}>()
</script>

<style scoped>
.select-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.select-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
}

.select-label .required {
  color: #dc2626;
  margin-left: 0.25rem;
}

.select {
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  font-family: inherit;
  transition: all 0.2s ease;
  background: #ffffff;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%236b7280'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.5rem center;
  background-size: 1.25rem;
  padding-right: 2.5rem;
}

.select:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.select:disabled {
  background: #f3f4f6;
  cursor: not-allowed;
  opacity: 0.6;
}

.select--error {
  border-color: #dc2626;
}

.select--error:focus {
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.1);
}

.select-error {
  font-size: 0.75rem;
  color: #dc2626;
}
</style>
