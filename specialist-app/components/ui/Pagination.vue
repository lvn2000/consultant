<template>
  <div v-if="totalPages > 1" class="pagination">
    <button
      type="button"
      class="pagination-btn"
      :disabled="currentPage === 1"
      @click="$emit('page-change', currentPage - 1)"
      :aria-label="$t('common.previous')"
    >
      <span v-if="showLabels">{{ $t('common.previous') }}</span>
      <span v-else>&larr;</span>
    </button>

    <span class="pagination-info">
      <slot name="info">
        {{ $t('common.pagination.pageWithTotal', { current: currentPage, total: totalPages, count: totalCount }) }}
      </slot>
    </span>

    <button
      type="button"
      class="pagination-btn"
      :disabled="currentPage === totalPages"
      @click="$emit('page-change', currentPage + 1)"
      :aria-label="$t('common.next')"
    >
      <span v-if="showLabels">{{ $t('common.next') }}</span>
      <span v-else>&rarr;</span>
    </button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  currentPage: number
  totalPages: number
  totalCount?: number
  showLabels?: boolean
}>()

defineEmits<{
  (e: 'page-change', page: number): void
}>()
</script>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 1rem;
}

.pagination-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  color: #1f2937;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
  font-size: 0.875rem;
}

.pagination-btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  color: #6b7280;
  font-size: 0.875rem;
  font-weight: 500;
}
</style>
