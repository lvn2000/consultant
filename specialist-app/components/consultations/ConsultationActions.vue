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
  <div class="consultation-actions">
    <button
      v-if="canApprove"
      class="btn btn-success"
      @click="onApprove"
      :disabled="loading"
    >
      Approve
    </button>
    <button
      v-if="canDecline"
      class="btn btn-danger"
      @click="onDecline"
      :disabled="loading"
    >
      Decline
    </button>
    <button
      v-if="canMarkMissed"
      class="btn btn-warning"
      @click="onMarkMissed"
      :disabled="loading"
    >
      Mark as Missed
    </button>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue'

const props = defineProps<{
  consultation: any
  loading: boolean
  canApprove: boolean
  canDecline: boolean
  canMarkMissed: boolean
}>()

const emit = defineEmits<{
  (e: 'approve', consultation: any): void
  (e: 'decline', consultation: any): void
  (e: 'mark-missed', consultation: any): void
}>()

function onApprove() {
  emit('approve', props.consultation)
}

function onDecline() {
  emit('decline', props.consultation)
}

function onMarkMissed() {
  emit('mark-missed', props.consultation)
}
</script>

<style scoped>
.consultation-actions {
  display: flex;
  gap: 0.5rem;
}

.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
}

.btn-success {
  background: #10b981;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #059669;
}

.btn-danger {
  background: #dc2626;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #b91c1c;
}

.btn-warning {
  background: #fbbf24;
  color: #1f2937;
}

.btn-warning:hover:not(:disabled) {
  background: #f59e42;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
