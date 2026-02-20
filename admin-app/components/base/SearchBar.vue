<template>
  <div class="search-bar">
    <BaseInput
      :model-value="modelValue"
      :placeholder="placeholder"
      type="search"
      class="search-input"
      @update:model-value="$emit('update:modelValue', $event)"
      @keyup.enter="$emit('search')"
    />
    <BaseButton
      v-if="modelValue"
      variant="default"
      size="small"
      @click="$emit('clear')"
    >
      ❌ {{ $t('common.clear') }}
    </BaseButton>
    <BaseButton v-else variant="primary" size="small" @click="$emit('search')">
      🔍 {{ $t('common.search') }}
    </BaseButton>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string
  placeholder?: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
  search: []
  clear: []
}>()
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.search-input {
  flex: 1;
}
</style>
