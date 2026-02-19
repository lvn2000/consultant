<template>
  <div class="list-state">
    <div v-if="showSpinner" class="spinner"></div>
    <slot>
      <p>{{ message || $t('common.loading') }}</p>
    </slot>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  message?: string
  showSpinner?: boolean
}>()
</script>

<style scoped>
.list-state {
  padding: 2rem 1rem;
  background: white;
  border-radius: 12px;
  text-align: center;
  color: #6b7280;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e5e7eb;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
