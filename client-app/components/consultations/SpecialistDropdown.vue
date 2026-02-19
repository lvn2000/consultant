<template>
  <div class="specialist-dropdown">
    <label>{{ label }}</label>
    <input
      type="text"
      v-model="search"
      :placeholder="placeholder"
      @focus="showDropdown = true"
      @input="onInput"
      class="dropdown-input"
    />
    <div v-if="showDropdown" class="dropdown-menu">
      <div
        v-for="specialist in filteredSpecialists"
        :key="specialist.id"
        class="dropdown-item"
        @click="selectSpecialist(specialist)"
      >
        <span class="specialist-name">{{ specialist.name }}</span>
        <span class="specialist-email">{{ specialist.email }}</span>
      </div>
      <div v-if="filteredSpecialists.length === 0" class="dropdown-item no-results">
        {{ noResultsText }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface Specialist {
  id: string
  name: string
  email: string
}

const props = defineProps<{
  specialists: Specialist[]
  modelValue: string | null
  label?: string
  placeholder?: string
  noResultsText?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void
  (e: 'select', specialist: Specialist): void
}>()

const search = ref('')
const showDropdown = ref(false)

const filteredSpecialists = computed(() => {
  if (!search.value) return props.specialists
  const lower = search.value.toLowerCase()
  return props.specialists.filter(
    s =>
      s.name.toLowerCase().includes(lower) ||
      s.email.toLowerCase().includes(lower)
  )
})

function selectSpecialist(specialist: Specialist) {
  emit('update:modelValue', specialist.id)
  emit('select', specialist)
  showDropdown.value = false
  search.value = specialist.name
}

function onInput() {
  showDropdown.value = true
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      const selected = props.specialists.find(s => s.id === val)
      if (selected) search.value = selected.name
    }
  }
)
</script>

<style scoped>
.specialist-dropdown {
  position: relative;
  width: 100%;
}

.dropdown-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.95rem;
  font-family: inherit;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  z-index: 10;
  max-height: 200px;
  overflow-y: auto;
}

.dropdown-item {
  padding: 0.75rem;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.dropdown-item:hover {
  background: #f3f4f6;
}

.dropdown-item.no-results {
  color: #999;
  cursor: default;
}

.specialist-name {
  font-weight: 500;
  color: #1f2937;
}

.specialist-email {
  font-size: 0.85rem;
  color: #6b7280;
}
</style>
