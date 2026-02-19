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
