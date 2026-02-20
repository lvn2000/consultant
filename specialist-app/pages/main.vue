<template>
  <div class="main-container">
    <nav class="menu-panel">
      <div class="menu-header">
        <div class="menu-title">{{ $t('specialist.menu.title') }}</div>
        <LocaleSwitcher />
      </div>
      <ul>
        <li :class="{ active: selectedMenu === 'profile' }" @click="selectMenu('profile')">
          {{ $t('specialist.menu.profile') }}
        </li>
        <li :class="{ active: selectedMenu === 'notifications' }" @click="selectMenu('notifications')">
          {{ $t('specialist.menu.notifications') }}
        </li>
        <li :class="{ active: selectedMenu === 'rates' }" @click="selectMenu('rates')">
          {{ $t('specialist.menu.rates') }}
        </li>
        <li :class="{ active: selectedMenu === 'availability' }" @click="selectMenu('availability')">
          {{ $t('specialist.menu.availability') }}
        </li>
        <li :class="{ active: selectedMenu === 'connections' }" @click="selectMenu('connections')">
          {{ $t('specialist.menu.connections') }}
        </li>
        <li :class="{ active: selectedMenu === 'consultations' }" @click="selectMenu('consultations')">
          {{ $t('specialist.menu.consultations') }}
        </li>
      </ul>
      <div class="menu-divider"></div>
      <ul>
        <li class="logout" @click="logout">{{ $t('common.logout') }}</li>
      </ul>
    </nav>
    <div class="content">
      <div class="welcome-header">
        <div class="welcome-content">
          <h1 class="welcome-title">
            <span class="wave">👋</span> {{ $t('specialist.welcome.title') }}
          </h1>
          <p class="welcome-subtitle">{{ $t('specialist.welcome.subtitle') }}</p>
        </div>
      </div>

      <!-- Profile Section -->
      <ProfileSection 
        v-if="selectedMenu === 'profile'"
        ref="profileSectionRef"
      />

      <!-- Rates Section -->
      <RatesSection 
        v-if="selectedMenu === 'rates'"
        ref="ratesSectionRef"
      />

      <!-- Availability Section -->
      <AvailabilitySection 
        v-if="selectedMenu === 'availability'"
        ref="availabilitySectionRef"
      />

      <!-- Connections Section -->
      <ConnectionsSection 
        v-if="selectedMenu === 'connections'"
        ref="connectionsSectionRef"
      />

      <!-- Consultations Section -->
      <ConsultationsSection 
        v-if="selectedMenu === 'consultations'"
        ref="consultationsSectionRef"
      />

      <!-- Notifications Section -->
      <NotificationsSection 
        v-if="selectedMenu === 'notifications'"
        ref="notificationsSectionRef"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRuntimeConfig } from 'nuxt/app'
import { useApi } from '~/composables/useApi'
import ProfileSection from '~/components/ProfileSection.vue'
import RatesSection from '~/components/RatesSection.vue'
import AvailabilitySection from '~/components/AvailabilitySection.vue'
import ConnectionsSection from '~/components/ConnectionsSection.vue'
import ConsultationsSection from '~/components/ConsultationsSection.vue'
import NotificationsSection from '~/components/NotificationsSection.vue'

const router = useRouter()
const config = useRuntimeConfig()
const { $fetch } = useApi()

// Menu state
const selectedMenu = ref('profile')

// Component refs
const profileSectionRef = ref<any>(null)
const ratesSectionRef = ref<any>(null)
const availabilitySectionRef = ref<any>(null)
const connectionsSectionRef = ref<any>(null)
const consultationsSectionRef = ref<any>(null)
const notificationsSectionRef = ref<any>(null)

const selectMenu = (menu: string) => {
  selectedMenu.value = menu
  if (menu === 'profile') profileSectionRef.value?.loadProfile()
  if (menu === 'rates') ratesSectionRef.value?.loadRates()
  if (menu === 'availability') availabilitySectionRef.value?.loadAvailability()
  if (menu === 'connections') connectionsSectionRef.value?.loadConnections()
  if (menu === 'consultations') consultationsSectionRef.value?.loadConsultations()
  if (menu === 'notifications') notificationsSectionRef.value?.loadNotificationPreferences()
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
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('sessionId')
    sessionStorage.removeItem('userId')
    sessionStorage.removeItem('specialistId')
    sessionStorage.removeItem('login')
    sessionStorage.removeItem('email')
    sessionStorage.removeItem('role')
    localStorage.removeItem('specialist_session')
    router.push('/login')
  }
}

onMounted(() => {
  // Check if user is properly logged in
  const userId = sessionStorage.getItem('userId')
  const role = sessionStorage.getItem('role')?.toLowerCase()
  
  if (!userId || role !== 'specialist') {
    // Not logged in or not a specialist, redirect to login
    localStorage.removeItem('specialist_session')
    router.push('/login')
    return
  }
  
  profileSectionRef.value?.loadProfile()
})
</script>

<style scoped>
.main-container {
  display: flex;
  min-height: 100vh;
  width: 100%;
  justify-content: center;
  background: #f8fafc;
}

.menu-panel {
  width: 200px;
  background: #f5f5f5;
  padding: 1.5rem 0.75rem;
  border-right: 1px solid #ddd;
  flex-shrink: 0;
}

.menu-title {
  font-weight: 700;
  color: #1f2937;
  font-size: 0.95rem;
}

.menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.menu-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 0.75rem 0;
}

.menu-panel ul {
  list-style: none;
  padding: 0;
}

.menu-panel li {
  padding: 0.55rem 0.75rem;
  cursor: pointer;
  color: #4f46e5;
  border-radius: 4px;
  transition: background 0.15s, color 0.15s;
  font-size: 0.9rem;
}

.menu-panel li:hover {
  background: #e0e7ff;
}

.menu-panel li.active {
  background: #4f46e5;
  color: white;
}

.menu-panel li.logout {
  color: #dc2626;
}

.menu-panel li.logout:hover {
  background: #fee2e2;
}

.content {
  flex: 1;
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2.5rem;
  border-radius: 12px;
  margin-bottom: 2rem;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.15);
}

.welcome-content {
  color: white;
}

.welcome-title {
  margin: 0 0 0.5rem 0;
  font-size: 2rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.wave {
  display: inline-block;
  animation: wave 2.5s ease-in-out infinite;
}

@keyframes wave {
  0%, 100% {
    transform: rotate(0deg);
  }
  25% {
    transform: rotate(14deg);
  }
  75% {
    transform: rotate(-14deg);
  }
}

.welcome-subtitle {
  margin: 0;
  font-size: 1rem;
  opacity: 0.95;
  font-weight: 400;
}
</style>
