<template>
  <div class="main-container">
    <nav class="menu-panel">
      <div class="menu-title">Client Menu</div>
      <ul>
        <li :class="{ active: selectedMenu === 'profile' }" @click="selectMenu('profile')">Profile</li>
        <li :class="{ active: selectedMenu === 'connections' }" @click="selectMenu('connections')">My Connections</li>
        <li :class="{ active: selectedMenu === 'consultations' }" @click="selectMenu('consultations')">My Consultations</li>
      </ul>
      <div class="menu-divider"></div>
      <ul>
        <li class="logout" @click="logout">Logout</li>
      </ul>
    </nav>
    
    <div class="content">
      <h1>Client Dashboard</h1>

      <!-- Profile Section -->
      <ProfileSection 
        v-if="selectedMenu === 'profile'"
        ref="profileSectionRef"
        @remove-account="showRemoveAccountConfirm = true"
      />

      <!-- Connections Section -->
      <ConnectionsSection 
        v-if="selectedMenu === 'connections'"
        ref="connectionsSectionRef"
      />

      <!-- Consultations Section -->
      <div v-if="selectedMenu === 'consultations'" class="section">
        <h3>My Consultations</h3>
        
        <!-- Tabs for View and Book -->
        <div class="tabs-header">
          <button 
            :class="['tab-btn', consultationsTab === 'view' ? 'active' : '']"
            @click="consultationsTab = 'view'"
          >
            View Consultations
          </button>
          <button 
            :class="['tab-btn', consultationsTab === 'book' ? 'active' : '']"
            @click="consultationsTab = 'book'"
          >
            Book Consultation
          </button>
        </div>
        
        <!-- View Consultations Tab -->
        <ConsultationsViewTab 
          v-if="consultationsTab === 'view'"
          :loading="consultationsLoading"
          :error="consultationsError"
          :consultations="consultations"
          :filters="consultationFilters"
          :pagination="consultationPagination"
          @load-consultations="loadConsultations"
          @go-to-page="goToPage"
          @clear-filters="clearConsultationFilters"
        />

        <!-- Book Consultation Tab -->
        <ConsultationsBookTab 
          v-if="consultationsTab === 'book'"
          @consultation-created="loadConsultations"
        />
      </div>
    </div>

    <!-- Remove Account Confirmation Modal -->
    <RemoveAccountModal 
      :show="showRemoveAccountConfirm"
      @close="showRemoveAccountConfirm = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRuntimeConfig } from 'nuxt/app'
import { $fetch } from 'ofetch'
import ProfileSection from '~/components/ProfileSection.vue'
import ConnectionsSection from '~/components/ConnectionsSection.vue'
import ConsultationsViewTab from '~/components/ConsultationsViewTab.vue'
import ConsultationsBookTab from '~/components/ConsultationsBookTab.vue'
import RemoveAccountModal from '~/components/RemoveAccountModal.vue'

const router = useRouter()
const config = useRuntimeConfig()

// Menu state
const selectedMenu = ref('profile')
const showRemoveAccountConfirm = ref(false)

// Component refs
const profileSectionRef = ref<any>(null)
const connectionsSectionRef = ref<any>(null)

// Consultations state
const consultationsTab = ref('view')
const consultationsLoading = ref(false)
const consultationsError = ref('')
const consultations = ref<any[]>([])
const consultationFilters = ref({
  status: '',
  fromDate: '',
  toDate: '',
  search: ''
})
const consultationPagination = ref({
  currentPage: 1,
  pageSize: 10,
  totalCount: 0,
  totalPages: 0
})

const selectMenu = (menu: string) => {
  selectedMenu.value = menu
  if (menu === 'profile') {
    profileSectionRef.value?.loadProfile()
  }
  if (menu === 'connections') {
    connectionsSectionRef.value?.loadConnections()
  }
  if (menu === 'consultations') {
    loadConsultations()
  }
}

// Consultations operations
const loadConsultations = async () => {
  consultationsLoading.value = true
  consultationsError.value = ''
  try {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      consultationsError.value = 'User ID not found'
      return
    }
    const data = await $fetch(`${config.public.apiBase}/consultations/user/${userId}`)
    consultations.value = data
    consultationPagination.value.totalCount = data.length
    consultationPagination.value.totalPages = Math.ceil(data.length / consultationPagination.value.pageSize)
    consultationPagination.value.currentPage = 1
  } catch (error: any) {
    console.error('Consultations load error:', error)
    consultationsError.value = error.data?.message || error.message || 'Failed to load consultations'
  } finally {
    consultationsLoading.value = false
  }
}

const clearConsultationFilters = () => {
  consultationFilters.value = {
    status: '',
    fromDate: '',
    toDate: '',
    search: ''
  }
  consultationPagination.value.currentPage = 1
}

const goToPage = (page: number) => {
  if (page >= 1 && page <= consultationPagination.value.totalPages) {
    consultationPagination.value.currentPage = page
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
    localStorage.removeItem('client_session')
    router.push('/login')
  }
}

onMounted(() => {
  selectMenu('profile')
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
  margin-bottom: 0.75rem;
  color: #1f2937;
  font-size: 0.95rem;
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

.content h1 {
  margin-bottom: 2rem;
  color: #1f2937;
}

.section {
  margin-top: 2rem;
}

.section h3 {
  color: #1f2937;
  margin-bottom: 1.5rem;
}

.tabs-header {
  display: flex;
  gap: 0.5rem;
  border-bottom: 2px solid #e5e7eb;
  margin-bottom: 1.5rem;
}

.tab-btn {
  padding: 0.75rem 1.5rem;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: #6b7280;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  bottom: -2px;
}

.tab-btn:hover {
  color: #374151;
  background: #f9fafb;
}

.tab-btn.active {
  color: #4f46e5;
  border-bottom-color: #4f46e5;
  background: white;
}
</style>
