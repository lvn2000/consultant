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
  <div class="section-header">
    <div class="header-content">
      <h2>
        <span v-if="icon" class="icon">{{ icon }}</span>
        <slot name="title">{{ title }}</slot>
      </h2>
      <p v-if="subtitle" class="header-subtitle">{{ subtitle }}</p>
    </div>
    <slot name="actions">
      <button v-if="showRefresh" type="button" class="btn" @click="$emit('refresh')">
        {{ refreshLabel || $t('common.refresh') }}
      </button>
    </slot>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon?: string
  title?: string
  subtitle?: string
  showRefresh?: boolean
  refreshLabel?: string
}>()

defineEmits<{
  (e: 'refresh'): void
}>()
</script>

<style scoped>
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  gap: 1.5rem;
}

.header-content {
  flex: 1;
}

.section-header h2 {
  color: #1f2937;
  margin: 0 0 0.35rem 0;
  font-size: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-subtitle {
  color: #6b7280;
  margin: 0;
  font-size: 0.875rem;
  font-weight: 400;
}

.icon {
  font-size: 1.5rem;
}
</style>
