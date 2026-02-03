<template>
  <section class="section">
    <div class="section-header">
      <h2>My Profile</h2>
      <button type="button" class="btn" @click="loadProfile">Refresh</button>
    </div>

    <div class="list-state" v-if="profileLoading">Loading profile...</div>
    <div class="list-state error" v-else-if="profileError">{{ profileError }}</div>

    <form v-else class="form" @submit.prevent="updateProfile">
      <div class="form-grid">
        <div class="form-field">
          <label for="name">Full Name</label>
          <input id="name" v-model="profileForm.name" type="text" placeholder="Your name" required />
        </div>
        <div class="form-field">
          <label for="email">Email</label>
          <input id="email" v-model="profileForm.email" type="email" placeholder="your@email.com" required />
        </div>
        <div class="form-field">
          <label for="phone">Phone</label>
          <input id="phone" v-model="profileForm.phone" type="tel" placeholder="+1 555 123 4567" />
        </div>
        <div class="form-field">
          <label for="availability">Availability Status</label>
          <select id="availability" v-model="profileForm.isAvailable">
            <option :value="true">Available</option>
            <option :value="false">Unavailable</option>
          </select>
        </div>
      </div>
      <div class="form-actions">
        <button type="submit" class="btn" :disabled="profileUpdating">
          {{ profileUpdating ? 'Updating...' : 'Update Profile' }}
        </button>
        <button type="button" class="btn danger" @click="showRemoveAccountConfirm = true">
          Remove Account
        </button>
      </div>
      <div v-if="profileUpdateMessage" :class="['form-message', profileUpdateSuccess ? 'success' : 'error']">
        {{ profileUpdateMessage }}
      </div>
    </form>

    <!-- Remove Account Confirmation -->
    <div v-if="showRemoveAccountConfirm" class="modal-overlay" @click="showRemoveAccountConfirm = false">
      <div class="modal" @click.stop>
        <h3>Remove Account</h3>
        <p>Are you sure you want to remove your account? This action cannot be undone.</p>
        <div class="modal-actions">
          <button type="button" class="btn danger" @click="removeAccount" :disabled="accountRemoving">
            {{ accountRemoving ? 'Removing...' : 'Yes, Remove Account' }}
          </button>
          <button type="button" class="btn" @click="showRemoveAccountConfirm = false">Cancel</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'

const router = useRouter()
const config = useRuntimeConfig()

const profileLoading = ref(false)
const profileError = ref('')
const profileForm = ref({
  name: '',
  email: '',
  phone: '',
  isAvailable: true
})
const profileUpdating = ref(false)
const profileUpdateMessage = ref('')
const profileUpdateSuccess = ref(false)
const showRemoveAccountConfirm = ref(false)
const accountRemoving = ref(false)

const loadProfile = async () => {
  profileLoading.value = true
  profileError.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      profileError.value = 'User ID not found'
      return
    }
    const specialist = await $fetch(`${config.public.apiBase}/specialists/${userId}`)
    profileForm.value = {
      name: specialist.name || '',
      email: specialist.email || '',
      phone: specialist.phone || '',
      isAvailable: specialist.isAvailable ?? true
    }
  } catch (error: any) {
    profileError.value = error.message || 'Failed to load profile'
  } finally {
    profileLoading.value = false
  }
}

const updateProfile = async () => {
  profileUpdating.value = true
  profileUpdateMessage.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      profileUpdateMessage.value = 'User ID not found'
      profileUpdateSuccess.value = false
      return
    }
    await $fetch(`${config.public.apiBase}/specialists/${userId}`, {
      method: 'PUT',
      body: profileForm.value
    })
    profileUpdateMessage.value = 'Profile updated successfully'
    profileUpdateSuccess.value = true
  } catch (error: any) {
    profileUpdateMessage.value = error.message || 'Failed to update profile'
    profileUpdateSuccess.value = false
  } finally {
    profileUpdating.value = false
  }
}

const removeAccount = async () => {
  accountRemoving.value = true
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      alert('User ID not found')
      return
    }
    await $fetch(`${config.public.apiBase}/specialists/${userId}`, {
      method: 'DELETE'
    })
    alert('Account removed successfully')
    logout()
  } catch (error: any) {
    alert(error.message || 'Failed to remove account')
  } finally {
    accountRemoving.value = false
    showRemoveAccountConfirm.value = false
  }
}

const logout = async () => {
  const sessionId = sessionStorage.getItem('sessionId')
  try {
    if (sessionId) {
      await $fetch(`${config.public.apiBase}/users/logout`, {
        method: 'POST',
        body: { sessionId },
      })
    }
  } finally {
    sessionStorage.removeItem('sessionId')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('login')
    sessionStorage.removeItem('email')
    sessionStorage.removeItem('role')
    localStorage.removeItem('specialist_session')
    router.push('/login')
  }
}

onMounted(() => {
  loadProfile()
})

defineExpose({
  loadProfile
})
</script>

<style scoped>
.section {
  margin-top: 2rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-header h2 {
  color: #1f2937;
  margin: 0;
}

.list-state {
  padding: 2rem 1rem;
  background: white;
  border-radius: 12px;
  text-align: center;
  color: #6b7280;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.list-state.error {
  background: #fee2e2;
  color: #dc2626;
}

.form {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
}

.form-field label {
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #374151;
  font-size: 0.875rem;
}

.form-field input,
.form-field select {
  padding: 0.625rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: border-color 0.15s, box-shadow 0.15s;
  background: white;
  color: #1f2937;
}

.form-field input:focus,
.form-field select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.btn {
  padding: 0.5rem 1rem;
  background: #4f46e5;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn:hover:not(:disabled) {
  background: #4338ca;
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

.form-message {
  padding: 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
}

.form-message.success {
  background: #d1fae5;
  color: #065f46;
  border-left: 4px solid #059669;
}

.form-message.error {
  background: #fee2e2;
  color: #dc2626;
  border-left: 4px solid #dc2626;
}

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
</style>
