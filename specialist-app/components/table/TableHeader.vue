<template>
  <div class="table-header" :style="gridStyle">
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
.table-header {
  background: #f9fafb;
  font-weight: 600;
  font-size: 0.875rem;
  color: #6b7280;
  border-bottom: 1px solid #e5e7eb;
}
</style>
