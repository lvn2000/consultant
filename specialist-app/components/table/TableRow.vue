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
  <div class="table-row" :style="gridStyle" @click="$emit('row-click', $event)">
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  columns?: string[]
  columnWidths?: string[]
  gap?: string
}>()

defineEmits<{
  (e: 'row-click', event: MouseEvent): void
}>()

const gridStyle = computed(() => {
  if (props.columns?.length) {
    const widths = props.columnWidths || props.columns.map(() => '1fr')
    return {
      display: 'grid',
      gridTemplateColumns: widths.join(' '),
      gap: props.gap || '1rem',
      padding: '0.75rem 1rem'
    }
  }
  return {
    display: 'grid',
    padding: '0.75rem 1rem'
  }
})
</script>

<style scoped>
.table-row {
  border-bottom: 1px solid #f3f4f6;
  color: #1f2937;
}

.table-row:last-child {
  border-bottom: none;
}

.table-row:hover {
  background: #f9fafb;
}
</style>
