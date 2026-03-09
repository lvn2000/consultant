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
  <div v-if="show" class="modal-overlay" @click.self="close">
    <div class="modal">
      <h3>{{ title }}</h3>
      <p v-if="description">{{ description }}</p>
      <slot></slot>
      <div class="modal-actions">
        <button class="btn" @click="confirm" :disabled="loading">
          {{ confirmText }}
        </button>
        <button class="btn" @click="close" :disabled="loading">
          {{ cancelText }}
        </button>
      </div>
      <div v-if="error" class="error-message">{{ error }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue'

const props = defineProps<{
  show: boolean
  title: string
  description?: string
  confirmText?: string
  cancelText?: string
  loading?: boolean
  error?: string
}>()

const emit = defineEmits<{
  (e: 'confirm'): void
  (e: 'close'): void
}>()

function confirm() {
  emit('confirm')
}

function close() {
  emit('close')
}
</script>

<style scoped>
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

.modal {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  max-width: 500px;
  width: 90%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal h3 {
  margin-top: 0;
  color: #1f2937;
}

.modal p {
  color: #6b7280;
  margin: 1rem 0;
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

.btn:hover:not(:disabled) {
  background: #4b5563;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.error-message {
  color: #dc2626;
  margin-top: 1rem;
}
</style>
