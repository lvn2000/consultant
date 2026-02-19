<template>
  <div class="list-state error">
    <slot>
      <p>{{ message || $t('common.error') }}</p>
    </slot>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  message?: string
}>()
</script>

<style scoped>
.list-state.error {
  padding: 2rem 1rem;
  background: #fee2e2;
  color: #dc2626;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
</style>
