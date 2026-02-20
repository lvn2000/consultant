<template>
  <div class="category-dropdown">
    <label>{{ label }}</label>
    <select v-model="selectedCategory" @change="onCategoryChange" class="category-select">
      <option value="">{{ placeholder }}</option>
      <option v-for="category in categories" :key="category.id" :value="category.id">
        {{ category.name }}
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
// Props
interface Category {
  id: string;
  name: string;
  description?: string;
}

const props = defineProps<{
  categories: Category[];
  modelValue: string;
  label?: string;
  placeholder?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const selectedCategory = ref(props.modelValue);

watch(() => props.modelValue, (newVal) => {
  selectedCategory.value = newVal;
});

function onCategoryChange() {
  emit('update:modelValue', selectedCategory.value);
}
</script>

<style scoped>
.category-dropdown {
  display: flex;
  flex-direction: column;
  margin-bottom: 1rem;
}

.category-select {
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.95rem;
  font-family: inherit;
  background: white;
  color: #374151;
  outline: none;
  transition: border-color 0.2s;
}

.category-select:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}
</style>
