<template>
  <div class="form-grid" :style="gridStyle">
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  columns?: number
  minColumnWidth?: string
  gap?: string
}>()

const gridStyle = computed(() => {
  if (props.columns) {
    return {
      gridTemplateColumns: `repeat(${props.columns}, 1fr)`,
      gap: props.gap || '1rem'
    }
  }
  return {
    gridTemplateColumns: `repeat(auto-fit, minmax(${props.minColumnWidth || '250px'}, 1fr))`,
    gap: props.gap || '1rem'
  }
})
</script>

<style scoped>
.form-grid {
  display: grid;
  margin-bottom: 1.5rem;
}
</style>
