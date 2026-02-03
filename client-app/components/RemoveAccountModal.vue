<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <h3>Remove Account</h3>
      <p>Are you sure you want to remove your account? This action cannot be undone.</p>
      <div class="modal-actions">
        <button class="btn danger" @click="removeAccount" :disabled="removing">
          {{ removing ? 'Removing...' : 'Yes, Remove' }}
        </button>
        <button class="btn" @click="$emit('close')">Cancel</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'

interface Props {
  show: boolean
}

defineProps<Props>()
defineEmits(['close'])

const config = useRuntimeConfig()
const removing = ref(false)

const removeAccount = async () => {
  removing.value = true
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      alert('User ID not found')
      return
    }
    // Note: User deletion endpoint not yet implemented in API
    // TODO: Add DELETE /api/users/:id endpoint to the backend
    alert('Account deletion not yet available. API endpoint needs to be implemented.')
    return
  } catch (error: any) {
    alert(error.message || 'Failed to remove account')
  } finally {
    removing.value = false
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  max-width: 500px;
  width: 90%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal h3 {
  margin-top: 0;
  color: #1f2937;
}

.modal p {
  color: #6b7280;
  margin: 1rem 0;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 1.5rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  background: #6b7280;
  color: white;
}

.btn:hover:not(:disabled) {
  background: #4b5563;
}

.btn.danger {
  background: #dc2626;
}

.btn.danger:hover:not(:disabled) {
  background: #b91c1c;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
