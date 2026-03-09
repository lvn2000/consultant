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
  <div class="pagination">
    <div class="pagination-info">
      {{ infoText }}
    </div>
    <div class="pagination-controls">
      <BaseButton
        variant="default"
        size="small"
        :disabled="currentPage === 1"
        @click="previousPage"
      >
        ⬅️ {{ $t('common.previous') }}
      </BaseButton>
      <BaseButton
        variant="default"
        size="small"
        :disabled="isLastPage"
        @click="nextPage"
      >
        {{ $t('common.next') }} ➡️
      </BaseButton>
    </div>
    <div class="pagination-size">
      <label :for="`page-size-${id}`">{{ $t('common.pagination.pageSize') }}</label>
      <BaseSelect
        :id="`page-size-${id}`"
        :model-value="pageSize.toString()"
        @update:model-value="handlePageSizeChange"
      >
        <option value="10">10</option>
        <option value="20">20</option>
        <option value="50">50</option>
      </BaseSelect>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    currentPage: number
    pageSize: number
    total: number
  }>(),
  {
    currentPage: 1,
    pageSize: 20,
    total: 0,
  }
)

const emit = defineEmits<{
  'update:currentPage': [value: number]
  'update:pageSize': [value: number]
}>()

const id = Math.random().toString(36).slice(2, 9)

const totalPages = computed(() => {
  return Math.max(1, Math.ceil(props.total / props.pageSize))
})

const isLastPage = computed(() => {
  return props.currentPage === totalPages.value
})

const infoText = computed(() => {
  return `Page ${props.currentPage} of ${totalPages.value}`
})

const previousPage = () => {
  if (props.currentPage > 1) {
    emit('update:currentPage', props.currentPage - 1)
  }
}

const nextPage = () => {
  if (props.currentPage < totalPages.value) {
    emit('update:currentPage', props.currentPage + 1)
  }
}

const handlePageSizeChange = (value: string) => {
  emit('update:pageSize', parseInt(value, 10))
  emit('update:currentPage', 1)
}
</script>

<style scoped>
.pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 0;
}

.pagination-info {
  font-weight: 600;
  color: #1f2937;
  font-size: 0.875rem;
}

.pagination-controls {
  display: flex;
  gap: 0.5rem;
}

.pagination-size {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #374151;
  font-size: 0.875rem;
}
</style>
