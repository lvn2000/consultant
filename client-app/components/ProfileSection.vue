<template>
  <div class="section">
    <div v-if="profileLoading" class="list-state">Loading profile...</div>
    <div v-else-if="profileError" class="list-state error">{{ profileError }}</div>
    <div v-else class="form">
      <h3>Profile Information</h3>
      <div class="form-grid">
        <div class="form-field">
          <label>Name</label>
          <input v-model="profileForm.name" type="text" placeholder="Your name" />
        </div>
        <div class="form-field">
          <label>Email</label>
          <input v-model="profileForm.email" type="email" placeholder="your@email.com" />
        </div>
        <div class="form-field">
          <label>Phone</label>
          <input v-model="profileForm.phone" type="tel" placeholder="+1234567890" />
        </div>
      </div>
      <div class="form-actions">
        <button class="btn btn-primary" @click="updateProfile" :disabled="profileUpdating">
          {{ profileUpdating ? 'Updating...' : 'Update Profile' }}
        </button>
        <button class="btn btn-danger" @click="$emit('remove-account')">
          Remove Account
        </button>
      </div>
      <div v-if="profileUpdateMessage" :class="['form-message', profileUpdateSuccess ? 'success' : 'error']">
        {{ profileUpdateMessage }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'

const config = useRuntimeConfig()

const profileLoading = ref(false)
const profileError = ref('')
const profileForm = ref({
  name: '',
  email: '',
  phone: ''
})
const profileUpdating = ref(false)
const profileUpdateMessage = ref('')
const profileUpdateSuccess = ref(false)

const loadProfile = async () => {
  profileLoading.value = true
  profileError.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      profileError.value = 'User ID not found'
      return
    }
    const user = await $fetch(`${config.public.apiBase}/users/${userId}`)
    profileForm.value = {
      name: user.name || '',
      email: user.email || '',
      phone: user.phone || ''
    }
  } catch (error: any) {
    console.error('Profile load error:', error)
    profileError.value = error.data?.message || error.message || 'Failed to load profile'
  } finally {
    profileLoading.value = false
  }
}

const updateProfile = async () => {
  profileUpdating.value = true
  profileUpdateMessage.value = ''
  profileUpdateSuccess.value = false
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      profileUpdateMessage.value = 'User ID not found'
      return
    }
    
    await $fetch(`${config.public.apiBase}/users/${userId}`, {
      method: 'PUT',
      body: {
        name: profileForm.value.name,
        email: profileForm.value.email,
        phone: profileForm.value.phone || null
      }
    })
    
    profileUpdateMessage.value = 'Profile updated successfully'
    profileUpdateSuccess.value = true
    
    // Reload profile to get the updated data
    setTimeout(() => {
      loadProfile()
      profileUpdateMessage.value = ''
    }, 2000)
  } catch (error: any) {
    profileUpdateMessage.value = error.data?.message || error.message || 'Failed to update profile'
    profileUpdateSuccess.value = false
  } finally {
    profileUpdating.value = false
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

.list-state {
  padding: 2rem;
  text-align: center;
  color: #666;
  background: white;
  border-radius: 8px;
}

.list-state.error {
  color: #dc2626;
  background: #fee2e2;
}

.form {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 2rem;
}

.form h3 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  color: #1f2937;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
}

.form-field label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #374151;
  font-size: 0.9rem;
}

.form-field input,
.form-field textarea,
.form-field select {
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.95rem;
  font-family: inherit;
}

.form-field input:focus,
.form-field textarea:focus,
.form-field select:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-actions {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary {
  background: #4f46e5;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #4338ca;
}

.btn-danger {
  background: #dc2626;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #b91c1c;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-message {
  padding: 1rem;
  border-radius: 6px;
  margin-top: 1rem;
}

.form-message.success {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #86efac;
}

.form-message.error {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fca5a5;
}
</style>
