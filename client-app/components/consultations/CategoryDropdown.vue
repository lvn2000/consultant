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
