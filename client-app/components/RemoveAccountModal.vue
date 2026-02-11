<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <h3>{{ $t('profile.removeAccount') }}</h3>
      <p>{{ $t('profile.removeAccountConfirm') }}</p>
      <div class="modal-actions">
        <button class="btn danger" @click="removeAccount" :disabled="removing">
          {{ removing ? $t('common.removing') : $t('profile.yesRemoveAccount') }}
        </button>
        <button class="btn" @click="$emit('close')">{{ $t('common.cancel') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { useApi } from '../composables/useApi'

const { t } = useI18n()

interface Props {
  show: boolean
}

defineProps<Props>()
defineEmits(['close'])

const config = useRuntimeConfig()
const { $fetch } = useApi()
const removing = ref(false)

const removeAccount = async () => {
  removing.value = true
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      alert(t('auth.userIdNotFound'))
      return
    }
    // Note: User deletion endpoint not yet implemented in API
    // TODO: Add DELETE /api/users/:id endpoint to the backend
    alert(t('profile.accountDeletionNotAvailable'))
    return
  } catch (error: any) {
    alert(error.message || t('profile.failedToRemove'))
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
