<template>
  <div class="form-actions" :class="{ 'justify-end': alignEnd, 'justify-between': justifyBetween }">
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  alignEnd?: boolean
  justifyBetween?: boolean
}>()
</script>

<style scoped>
.form-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.form-actions.justify-end {
  justify-content: flex-end;
}

.form-actions.justify-between {
  justify-content: space-between;
}
</style>
