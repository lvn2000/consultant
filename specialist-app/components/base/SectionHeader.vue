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
