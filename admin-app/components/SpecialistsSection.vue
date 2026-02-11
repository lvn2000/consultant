<template>
  <section v-if="visible" class="section">
    <div class="section-header">
      <h2>{{ $t('adminSpecialists.title') }}</h2>
      <button type="button" class="btn" @click="loadSpecialists">🔄 {{ $t('common.refresh') }}</button>
    </div>

    <div class="search-bar">
      <input
        v-model="specialistsSearchQuery"
        type="text"
        :placeholder="$t('adminSpecialists.searchPlaceholder')"
        class="search-input"
      />
      <button type="button" class="btn" @click="clearSpecialistsSearch" v-if="specialistsSearchQuery">❌ {{ $t('common.clear') }}</button>
    </div>

    <div class="list-state" v-if="specialistsLoading">{{ $t('adminSpecialists.loading') }}</div>
    <div class="list-state error" v-else-if="specialistsError">{{ specialistsError }}</div>

    <div class="table" v-else>
      <div class="table-header specialists-table">
        <span>{{ $t('common.name') }}</span>
        <span>{{ $t('common.email') }}</span>
        <span>{{ $t('common.phone') }}</span>
        <span>{{ $t('adminSpecialists.categories') }}</span>
        <span>{{ $t('adminSpecialists.rateItems') }}</span>
        <span>{{ $t('common.connections') }}</span>
        <span>{{ $t('common.availability') }}</span>
        <span>{{ $t('common.actions') }}</span>
      </div>
      <div v-for="specialist in filteredSpecialists" :key="specialist.id" class="table-row specialists-table">
        <span>{{ specialist.name }}</span>
        <span>{{ specialist.email }}</span>
        <span>{{ specialist.phone }}</span>
        <span>{{ resolveCategoryRates(specialist.categoryRates) }}</span>
        <span>{{ specialist.categoryRates.length }}</span>
        <span>{{ resolveSpecialistConnections(specialist.connections) }}</span>
        <span>{{ specialist.isAvailable ? $t('common.available') : $t('common.unavailable') }}</span>
        <span class="row-actions">
          <button type="button" class="btn" @click="startEditSpecialist(specialist)">✏️ {{ $t('common.select') }}</button>
          <button type="button" class="btn danger" @click="removeSpecialist(specialist.id)">🗑️ {{ $t('common.delete') }}</button>
        </span>
      </div>
    </div>

    <div v-if="!specialistsLoading && !specialistsError" class="pagination">
      <div class="pagination-info">
        {{ $t('common.pagination.pageNumber', { current: currentPage }) }}
      </div>
      <div class="pagination-controls">
        <button type="button" class="btn" :disabled="currentPage === 1" @click="goToPreviousPage">
          ⬅️ {{ $t('common.previous') }}
        </button>
        <button type="button" class="btn" :disabled="isLastPage" @click="goToNextPage">
          {{ $t('common.next') }} ➡️
        </button>
      </div>
      <div class="pagination-size">
        <label for="page-size">{{ $t('common.pagination.pageSize') }}</label>
        <select id="page-size" v-model.number="pageSize" @change="handlePageSizeChange">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </div>
    </div>

    <form ref="specialistFormRef" class="form" v-if="selectedSpecialistId" @submit.prevent>
      <div class="form-header">
        <h3>{{ $t('adminSpecialists.specialistDetails') }}</h3>
        <span class="form-subtitle" v-if="specialistForm.name">{{ specialistForm.name }}</span>
      </div>

      <div class="form-field form-field--full">
        <div class="details-tabs">
          <button
            type="button"
            class="tab"
            :class="{ active: specialistDetailsTab === 'general' }"
            @click="specialistDetailsTab = 'general'"
          >
            {{ $t('adminSpecialists.general') }}
          </button>
          <button
            type="button"
            class="tab"
            :class="{ active: specialistDetailsTab === 'categoryRates' }"
            @click="specialistDetailsTab = 'categoryRates'"
          >
            {{ $t('adminSpecialists.categoryRates') }}
          </button>
          <button
            type="button"
            class="tab"
            :class="{ active: specialistDetailsTab === 'connections' }"
            @click="specialistDetailsTab = 'connections'"
          >
            {{ $t('common.connections') }}
          </button>
          <button
            type="button"
            class="tab"
            :class="{ active: specialistDetailsTab === 'notifications' }"
            @click="specialistDetailsTab = 'notifications'"
          >
            {{ $t('common.notifications') }}
          </button>
        </div>

        <div v-if="specialistDetailsTab === 'general'" class="details-panel">
          <div class="form-grid">
            <div class="form-field">
              <label for="specialist-name">{{ $t('profile.fullName') }}</label>
              <input id="specialist-name" v-model="specialistForm.name" type="text" :placeholder="$t('adminSpecialists.namePlaceholder')" />
            </div>
            <div class="form-field">
              <label for="specialist-email">{{ $t('common.email') }}</label>
              <input id="specialist-email" v-model="specialistForm.email" type="email" :placeholder="$t('adminSpecialists.emailPlaceholder')" />
            </div>
            <div class="form-field">
              <label for="specialist-phone">{{ $t('common.phone') }}</label>
              <input id="specialist-phone" v-model="specialistForm.phone" type="tel" :placeholder="$t('adminSpecialists.phonePlaceholder')" />
            </div>
            <div class="form-field form-field--full">
              <label for="specialist-bio">{{ $t('profile.bio') }}</label>
              <textarea id="specialist-bio" v-model="specialistForm.bio" :placeholder="$t('profile.bioPlaceholder')" rows="4"></textarea>
            </div>
            <div class="form-field">
              <label for="specialist-availability">{{ $t('profile.availabilityStatus') }}</label>
              <select id="specialist-availability" v-model="specialistForm.isAvailable">
                <option :value="true">{{ $t('common.available') }}</option>
                <option :value="false">{{ $t('common.unavailable') }}</option>
              </select>
            </div>
          </div>
        </div>

        <div v-if="specialistDetailsTab === 'categoryRates'" class="details-panel">
          <div class="rate-rows">
            <div class="rate-header">
              <span>{{ $t('rates.category') }}</span>
              <span>{{ $t('rates.rate') }}</span>
              <span>{{ $t('rates.experienceShort') }}</span>
              <span>{{ $t('common.action') }}</span>
            </div>
            <div v-for="(rate, index) in specialistForm.categoryRates" :key="index" class="rate-row">
              <select v-model="rate.categoryId">
                <option value="">{{ $t('rates.selectCategory') }}</option>
                <option v-for="category in categories" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
              <input
                v-model.number="rate.hourlyRate"
                type="number"
                min="1"
                step="0.01"
                :placeholder="$t('rates.rate')"
              />
              <input
                v-model.number="rate.experienceYears"
                type="number"
                min="0"
                step="1"
                :placeholder="$t('rates.experienceShort')"
              />
              <button type="button" class="btn" @click="removeCategoryRate(index)">🗑️ {{ $t('common.remove') }}</button>
            </div>
            <button type="button" class="btn" @click="addCategoryRate">➕ {{ $t('rates.addCategoryRate') }}</button>
          </div>
        </div>

        <div v-else-if="specialistDetailsTab === 'connections'" class="details-panel">
          <p v-if="!selectedSpecialistId" class="muted">
            {{ $t('adminSpecialists.selectToManageConnections') }}
          </p>
          <div v-else class="connections-manager">
            <div class="list-state" v-if="specialistConnectionsLoading">{{ $t('adminSpecialists.loadingConnections') }}</div>
            <div class="list-state error" v-else-if="specialistConnectionsError">
              {{ specialistConnectionsError }}
            </div>

            <div class="table" v-else>
              <div class="table-header specialist-connections-table">
                <span>{{ $t('adminSpecialists.type') }}</span>
                <span>{{ $t('adminSpecialists.value') }}</span>
                <span>{{ $t('common.verified') }}</span>
                <span>{{ $t('common.actions') }}</span>
              </div>
              <div
                v-for="connection in specialistConnections"
                :key="connection.id"
                class="table-row specialist-connections-table"
              >
                <span>{{ resolveConnectionTypeName(connection.connectionTypeId) }}</span>
                <span>{{ connection.connectionValue }}</span>
                <span>{{ connection.isVerified ? $t('common.yes') : $t('common.no') }}</span>
                <span class="row-actions">
                  <button type="button" class="btn" @click="startEditConnection(connection)">✏️ {{ $t('common.select') }}</button>
                  <button type="button" class="btn danger" @click="removeConnection(connection.id)">🗑️ {{ $t('common.delete') }}</button>
                </span>
              </div>
            </div>

            <div ref="connectionFormRef" class="form-grid">
              <div class="form-field">
                <label for="specialist-connection-type">{{ $t('connections.connectionType') }}</label>
                <select id="specialist-connection-type" v-model="connectionForm.connectionTypeId">
                  <option value="">{{ $t('adminSpecialists.selectType') }}</option>
                  <option v-for="type in connectionTypes" :key="type.id" :value="type.id">
                    {{ type.name }}
                  </option>
                </select>
              </div>
              <div class="form-field">
                <label for="specialist-connection-value">{{ $t('connections.connectionValue') }}</label>
                <input
                  id="specialist-connection-value"
                  v-model="connectionForm.connectionValue"
                  type="text"
                  :placeholder="$t('adminSpecialists.connectionValuePlaceholder')"
                />
              </div>
            </div>

            <div class="form-actions">
              <button type="button" class="btn primary" @click="addConnection">➕ {{ $t('adminSpecialists.addConnection') }}</button>
              <button type="button" class="btn" :disabled="!selectedConnectionId" @click="updateConnection">
                ✏️ {{ $t('adminSpecialists.updateConnection') }}
              </button>
              <button
                type="button"
                class="btn danger"
                :disabled="!selectedConnectionId"
                @click="deleteSelectedConnection"
              >
                🗑️ {{ $t('adminSpecialists.deleteConnection') }}
              </button>
              <button type="button" class="btn" @click="resetConnectionForm">❌ {{ $t('common.clear') }}</button>
            </div>

            <p v-if="connectionActionMessage" class="form-message">{{ connectionActionMessage }}</p>
          </div>
        </div>

        <div v-else-if="specialistDetailsTab === 'notifications'" class="details-panel">
          <p v-if="!selectedSpecialistId" class="muted">
            {{ $t('adminSpecialists.selectToManageNotifications') }}
          </p>
          <div v-else class="notifications-manager">
            <div class="list-state" v-if="notificationsLoading">{{ $t('notifications.loading') }}</div>
            <div class="list-state error" v-else-if="notificationsError">
              {{ notificationsError }}
            </div>

            <div v-else class="notifications-list">
              <div v-for="pref in specialistNotifications" :key="pref.id" class="notification-item">
                <div class="notification-label">
                  <span class="notification-name">{{ formatNotificationType(pref.notificationType) }}</span>
                  <span class="notification-description">{{ getNotificationDescription(pref.notificationType) }}</span>
                </div>
                <label class="toggle-switch">
                  <input 
                    type="checkbox" 
                    v-model="pref.emailEnabled"
                    @change="updateNotificationPreference(pref)"
                    :disabled="updatingNotificationId === pref.id"
                    :aria-label="formatNotificationType(pref.notificationType) + ' ' + $t('notifications.emailNotifications')"
                  />
                  <span class="toggle-slider"></span>
                </label>
              </div>
            </div>

            <div v-if="notificationUpdateMessage" :class="['form-message', notificationUpdateSuccess ? 'success' : 'error']">
              {{ notificationUpdateMessage }}
            </div>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button type="button" class="btn primary" @click="addSpecialist">➕ {{ $t('adminSpecialists.addSpecialist') }}</button>
        <button type="button" class="btn" :disabled="!selectedSpecialistId" @click="updateSpecialist">
          ✏️ {{ $t('adminSpecialists.updateSpecialist') }}
        </button>
        <button type="button" class="btn" :disabled="!selectedSpecialistId || specialistDetailsTab !== 'categoryRates'" @click="updateCategoryRates">
          💾 {{ $t('adminSpecialists.saveCategoryRates') }}
        </button>
        <button type="button" class="btn danger" :disabled="!selectedSpecialistId" @click="deleteSelectedSpecialist">
          🗑️ {{ $t('adminSpecialists.deleteSpecialist') }}
        </button>
        <button type="button" class="btn" @click="resetSpecialistForm">❌ {{ $t('common.clear') }}</button>
      </div>

      <p v-if="specialistActionMessage" class="form-message">{{ specialistActionMessage }}</p>
    </form>

    <!-- Confirmation Dialog -->
    <div v-if="confirmState.visible" class="modal-overlay" @click.self="confirmResolver?.(false)">
      <div class="modal-dialog">
        <h3>{{ confirmState.title }}</h3>
        <p>{{ confirmState.message }}</p>
        <div class="modal-actions">
          <button type="button" class="btn" @click="confirmResolver?.(false)">{{ $t('common.cancel') }}</button>
          <button type="button" class="btn primary" @click="confirmResolver?.(true)">{{ $t('common.confirm') }}</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch, nextTick } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { useApi } from '../composables/useApi'

const { t } = useI18n()
const config = useRuntimeConfig()
const { $fetch } = useApi()
defineProps<{ visible: boolean }>()

type Specialist = {
  id: string
  name: string
  email: string
  phone: string
  bio: string
  categoryRates: Array<{
    categoryId: string
    hourlyRate: number
    experienceYears: number
    rating?: number | null
    totalConsultations?: number | null
  }>
  connections: SpecialistConnection[]
  isAvailable: boolean
}

type Category = {
  id: string
  name: string
  description: string
  parentId: string | null
}

type ConnectionType = {
  id: string
  name: string
  description: string | null
}

type SpecialistConnection = {
  id: string
  specialistId: string
  connectionTypeId: string
  connectionValue: string
  isVerified: boolean
  createdAt: string
  updatedAt: string
}

const specialists = ref<Specialist[]>([])
const specialistsLoading = ref(false)
const specialistsError = ref('')
const specialistActionMessage = ref('')
const selectedSpecialistId = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = ref(20)
const isLastPage = ref(false)
const specialistsSearchQuery = ref('')
const categories = ref<Category[]>([])
const connectionTypes = ref<ConnectionType[]>([])
const specialistConnections = ref<SpecialistConnection[]>([])
const specialistConnectionsLoading = ref(false)
const specialistConnectionsError = ref('')
const selectedConnectionId = ref<string | null>(null)
const connectionActionMessage = ref('')
const specialistDetailsTab = ref<'general' | 'categoryRates' | 'connections' | 'notifications'>('general')
const specialistFormRef = ref<HTMLFormElement | null>(null)
const connectionFormRef = ref<HTMLDivElement | null>(null)
const specialistNotifications = ref<any[]>([])
const notificationsLoading = ref(false)
const notificationsError = ref('')
const notificationUpdateMessage = ref('')
const notificationUpdateSuccess = ref(false)
const updatingNotificationId = ref<string | null>(null)

const specialistForm = ref({
  name: '',
  email: '',
  phone: '',
  bio: '',
  categoryRates: [] as Array<{
    categoryId: string
    hourlyRate: number
    experienceYears: number
    rating?: number | null
    totalConsultations?: number | null
  }>,
  isAvailable: true,
})

const connectionForm = ref({
  connectionTypeId: '',
  connectionValue: '',
})

const confirmState = ref({
  visible: false,
  title: '',
  message: '',
})
const confirmResolver = ref<((value: boolean) => void) | null>(null)

const confirmAction = (title: string, message: string) =>
  new Promise<boolean>(resolve => {
    confirmState.value = { visible: true, title, message }
    confirmResolver.value = (confirmed: boolean) => {
      resolve(confirmed)
      confirmState.value.visible = false
    }
  })

const filteredSpecialists = computed(() => {
  if (!specialistsSearchQuery.value.trim()) {
    return specialists.value
  }
  const query = specialistsSearchQuery.value.toLowerCase().trim()
  return specialists.value.filter(specialist => {
    return (
      specialist.name.toLowerCase().includes(query) ||
      specialist.email.toLowerCase().includes(query) ||
      specialist.phone.toLowerCase().includes(query)
    )
  })
})

const resolveCategoryRates = (
  rates: Array<{ categoryId: string; hourlyRate: number; experienceYears: number }>
) =>
  rates
    .map(rate => {
      const name = categories.value.find(category => category.id === rate.categoryId)?.name ?? rate.categoryId
      return `${name} (${rate.hourlyRate}, ${rate.experienceYears} ${t('rates.yearsShort')})`
    })
    .join(', ')

const resolveConnectionTypeName = (id: string) =>
  connectionTypes.value.find(type => type.id === id)?.name ?? id

const resolveSpecialistConnections = (connections: SpecialistConnection[]) =>
  connections
    .map(connection => `${resolveConnectionTypeName(connection.connectionTypeId)}: ${connection.connectionValue}`)
    .join(', ')

const loadSpecialists = async () => {
  specialistsLoading.value = true
  specialistsError.value = ''
  try {
    const data = await $fetch<Specialist[]>(`${config.public.apiBase}/specialists/search`, {
      method: 'GET',
      query: {
        offset: (currentPage.value - 1) * pageSize.value,
        limit: pageSize.value,
      },
    })
    specialists.value = data
    isLastPage.value = data.length < pageSize.value
  } catch (error) {
    specialistsError.value = t('adminSpecialists.failedToLoad')
  } finally {
    specialistsLoading.value = false
  }
}

const loadCategories = async () => {
  try {
    const data = await $fetch<Category[]>(`${config.public.apiBase}/categories`, {
      method: 'GET',
    })
    categories.value = data
  } catch (error) {
    categories.value = []
  }
}

const loadConnectionTypes = async () => {
  try {
    const data = await $fetch<ConnectionType[]>(`${config.public.apiBase}/connection-types`, {
      method: 'GET',
    })
    connectionTypes.value = data
  } catch (error) {
    connectionTypes.value = []
  }
}

const loadSpecialistConnections = async () => {
  if (!selectedSpecialistId.value) {
    specialistConnections.value = []
    return
  }
  specialistConnectionsLoading.value = true
  specialistConnectionsError.value = ''
  try {
    const data = await $fetch<SpecialistConnection[]>(
      `${config.public.apiBase}/specialists/${selectedSpecialistId.value}/connections`,
      { method: 'GET' }
    )
    specialistConnections.value = data
  } catch (error) {
    specialistConnections.value = []
    specialistConnectionsError.value = t('adminSpecialists.failedToLoadConnections')
  } finally {
    specialistConnectionsLoading.value = false
  }
}

const loadSpecialistNotifications = async () => {
  if (!selectedSpecialistId.value) {
    specialistNotifications.value = []
    return
  }
  notificationsLoading.value = true
  notificationsError.value = ''
  try {
    const token = sessionStorage.getItem('accessToken') || sessionStorage.getItem('sessionId')
    if (!token) {
      notificationsError.value = t('auth.sessionNotFound')
      return
    }
    const data = await $fetch<any>(`${config.public.apiBase}/notification-preferences`, {
      method: 'GET',
      headers: {
        'X-User-Id': selectedSpecialistId.value
      }
    })
    // Extract preferences array from response object
    specialistNotifications.value = data.preferences || []
  } catch (error: any) {
    console.error('Error loading notifications:', error)
    specialistNotifications.value = []
    notificationsError.value = error?.message || t('notifications.failedToLoad')
  } finally {
    notificationsLoading.value = false
  }
}

const formatNotificationType = (type: string): string => {
  return type
    .split(/(?=[A-Z])/)
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

const getNotificationDescription = (type: string): string => {
  const descriptions: Record<string, string> = {
    'ConsultationApproved': t('notifications.descriptionsAdmin.approved'),
    'ConsultationDeclined': t('notifications.descriptionsAdmin.declined'),
    'ConsultationCompleted': t('notifications.descriptionsAdmin.completed'),
    'ConsultationMissed': t('notifications.descriptionsAdmin.missed'),
    'ConsultationCancelled': t('notifications.descriptionsAdmin.cancelled')
  }
  return descriptions[type] || t('notifications.fallbackDescription')
}

const updateNotificationPreference = async (pref: any) => {
  if (!selectedSpecialistId.value) {
    notificationUpdateMessage.value = t('adminSpecialists.selectToUpdatePreferences')
    notificationUpdateSuccess.value = false
    return
  }
  updatingNotificationId.value = pref.id
  notificationUpdateMessage.value = ''
  try {
    const token = sessionStorage.getItem('accessToken') || sessionStorage.getItem('sessionId')
    if (!token) {
      notificationUpdateMessage.value = t('auth.sessionNotFound')
      notificationUpdateSuccess.value = false
      return
    }
    await $fetch(`${config.public.apiBase}/notification-preferences/${pref.notificationType}`, {
      method: 'PUT',
      body: {
        emailEnabled: pref.emailEnabled,
        smsEnabled: pref.smsEnabled || false
      },
      headers: {
        'X-User-Id': selectedSpecialistId.value
      }
    })
    notificationUpdateMessage.value = t('adminSpecialists.notificationUpdated')
    notificationUpdateSuccess.value = true
  } catch (error) {
    notificationUpdateMessage.value = t('adminSpecialists.failedToUpdateNotification')
    notificationUpdateSuccess.value = false
    // Reload preferences to sync UI with actual persisted state
    await loadSpecialistNotifications()
  } finally {
    updatingNotificationId.value = null
  }
}

watch(
  () => specialistDetailsTab.value,
  async (newTab) => {
    if (newTab === 'connections' && selectedSpecialistId.value) {
      await loadSpecialistConnections()
    }
    if (newTab === 'notifications' && selectedSpecialistId.value) {
      await loadSpecialistNotifications()
    }
  }
)

watch(
  () => selectedSpecialistId.value,
  async newId => {
    if (newId && specialistDetailsTab.value === 'connections') {
      await loadSpecialistConnections()
    }
  }
)

const goToPreviousPage = async () => {
  if (currentPage.value > 1) {
    currentPage.value -= 1
    await loadSpecialists()
  }
}

const goToNextPage = async () => {
  if (!isLastPage.value) {
    currentPage.value += 1
    await loadSpecialists()
  }
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadSpecialists()
}

const clearSpecialistsSearch = () => {
  specialistsSearchQuery.value = ''
}

const resetSpecialistForm = () => {
  specialistForm.value = {
    name: '',
    email: '',
    phone: '',
    bio: '',
    categoryRates: [],
    isAvailable: true,
  }
  selectedSpecialistId.value = null
  specialistConnections.value = []
  specialistNotifications.value = []
  specialistDetailsTab.value = 'general'
}

const resetConnectionForm = () => {
  connectionForm.value = {
    connectionTypeId: '',
    connectionValue: '',
  }
  selectedConnectionId.value = null
}

const startEditSpecialist = (specialist: Specialist) => {
  selectedSpecialistId.value = specialist.id
  specialistForm.value = {
    name: specialist.name,
    email: specialist.email,
    phone: specialist.phone,
    bio: specialist.bio,
    categoryRates: specialist.categoryRates.map(rate => ({ ...rate })),
    isAvailable: specialist.isAvailable,
  }
  specialistActionMessage.value = ''
  resetConnectionForm()
  loadSpecialistConnections()
  loadSpecialistNotifications()
  specialistDetailsTab.value = 'general'
  // Scroll to form and focus
  nextTick(() => {
    if (specialistFormRef.value) {
      specialistFormRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
      const firstInput = specialistFormRef.value.querySelector('input') as HTMLInputElement
      if (firstInput) {
        setTimeout(() => firstInput.focus(), 300)
      }
    }
  })
}

const startEditConnection = (connection: SpecialistConnection) => {
  selectedConnectionId.value = connection.id
  connectionForm.value = {
    connectionTypeId: connection.connectionTypeId,
    connectionValue: connection.connectionValue,
  }
  connectionActionMessage.value = ''
  // Scroll to form and focus
  nextTick(() => {
    if (connectionFormRef.value) {
      connectionFormRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
      const firstInput = connectionFormRef.value.querySelector('select, input') as HTMLInputElement | HTMLSelectElement
      if (firstInput) {
        setTimeout(() => firstInput.focus(), 300)
      }
    }
  })
}

const addCategoryRate = () => {
  specialistForm.value.categoryRates.push({
    categoryId: '',
    hourlyRate: 0,
    experienceYears: 0,
    rating: null,
    totalConsultations: null,
  })
}

const removeCategoryRate = (index: number) => {
  specialistForm.value.categoryRates.splice(index, 1)
}

const addSpecialist = async () => {
  specialistActionMessage.value = ''
  const confirmed = await confirmAction(t('adminSpecialists.addSpecialist'), t('adminSpecialists.addSpecialistConfirm'))
  if (!confirmed) return
  try {
    await $fetch(`${config.public.apiBase}/specialists`, {
      method: 'POST',
      body: {
        email: specialistForm.value.email,
        name: specialistForm.value.name,
        phone: specialistForm.value.phone,
        bio: specialistForm.value.bio,
        categoryRates: specialistForm.value.categoryRates,
        isAvailable: specialistForm.value.isAvailable,
      },
    })
    specialistActionMessage.value = t('adminSpecialists.specialistCreated')
    resetSpecialistForm()
    await loadSpecialists()
  } catch (error) {
    specialistActionMessage.value = t('adminSpecialists.failedToCreate')
  }
}

const updateSpecialist = async () => {
  if (!selectedSpecialistId.value) {
    specialistActionMessage.value = t('adminSpecialists.selectToUpdate')
    return
  }
  const confirmed = await confirmAction(t('adminSpecialists.updateSpecialist'), t('adminSpecialists.updateSpecialistConfirm'))
  if (!confirmed) return
  specialistActionMessage.value = ''
  try {
    await $fetch(`${config.public.apiBase}/specialists/${selectedSpecialistId.value}`, {
      method: 'PUT',
      body: {
        email: specialistForm.value.email,
        name: specialistForm.value.name,
        phone: specialistForm.value.phone,
        bio: specialistForm.value.bio,
        categoryRates: specialistForm.value.categoryRates,
        isAvailable: specialistForm.value.isAvailable,
      },
    })
    specialistActionMessage.value = t('adminSpecialists.specialistUpdated')
    await loadSpecialists()
  } catch (error) {
    specialistActionMessage.value = t('adminSpecialists.failedToUpdate')
  }
}

const updateCategoryRates = async () => {
  if (!selectedSpecialistId.value) {
    specialistActionMessage.value = t('adminSpecialists.selectToUpdate')
    return
  }
  if (specialistForm.value.categoryRates.length === 0) {
    specialistActionMessage.value = t('adminSpecialists.addAtLeastOneRate')
    return
  }
  const confirmed = await confirmAction(t('adminSpecialists.updateCategoryRates'), t('adminSpecialists.saveCategoryRatesConfirm'))
  if (!confirmed) return
  specialistActionMessage.value = ''
  try {
    await $fetch(`${config.public.apiBase}/specialists/${selectedSpecialistId.value}`, {
      method: 'PUT',
      body: {
        email: specialistForm.value.email,
        name: specialistForm.value.name,
        phone: specialistForm.value.phone,
        bio: specialistForm.value.bio,
        categoryRates: specialistForm.value.categoryRates,
        isAvailable: specialistForm.value.isAvailable,
      },
    })
    specialistActionMessage.value = t('adminSpecialists.ratesUpdated')
    await loadSpecialists()
  } catch (error) {
    specialistActionMessage.value = t('adminSpecialists.failedToUpdateRates')
  }
}

const deleteSelectedSpecialist = async () => {
  if (!selectedSpecialistId.value) {
    specialistActionMessage.value = t('adminSpecialists.selectToDelete')
    return
  }
  const confirmed = await confirmAction(t('adminSpecialists.deleteSpecialist'), t('adminSpecialists.deleteSpecialistConfirm'))
  if (!confirmed) return
  try {
    await $fetch(`${config.public.apiBase}/specialists/${selectedSpecialistId.value}`, {
      method: 'DELETE',
    })
    specialistActionMessage.value = t('adminSpecialists.specialistDeleted')
    resetSpecialistForm()
    await loadSpecialists()
  } catch (error) {
    specialistActionMessage.value = t('adminSpecialists.failedToDelete')
  }
}

const removeSpecialist = async (id: string) => {
  const confirmed = await confirmAction(t('adminSpecialists.deleteSpecialist'), t('adminSpecialists.deleteSpecialistConfirm'))
  if (!confirmed) return
  try {
    await $fetch(`${config.public.apiBase}/specialists/${id}`, { method: 'DELETE' })
    await loadSpecialists()
  } catch (error) {
    specialistActionMessage.value = t('adminSpecialists.failedToDelete')
  }
}

const addConnection = async () => {
  connectionActionMessage.value = ''
  if (!selectedSpecialistId.value) {
    connectionActionMessage.value = t('adminSpecialists.selectToAddConnection')
    return
  }
  if (!connectionForm.value.connectionTypeId || !connectionForm.value.connectionValue) {
    connectionActionMessage.value = t('adminSpecialists.selectConnectionTypeAndValue')
    return
  }
  const confirmed = await confirmAction(t('adminSpecialists.addConnection'), t('adminSpecialists.addConnectionConfirm'))
  if (!confirmed) return
  try {
    await $fetch(`${config.public.apiBase}/specialists/${selectedSpecialistId.value}/connections`, {
      method: 'POST',
      body: {
        connectionTypeId: connectionForm.value.connectionTypeId,
        connectionValue: connectionForm.value.connectionValue,
      },
    })
    connectionActionMessage.value = t('adminSpecialists.connectionAdded')
    resetConnectionForm()
    await loadSpecialistConnections()
  } catch (error) {
    connectionActionMessage.value = t('adminSpecialists.failedToAddConnection')
  }
}

const updateConnection = async () => {
  connectionActionMessage.value = ''
  if (!selectedConnectionId.value) {
    connectionActionMessage.value = t('adminSpecialists.selectConnectionToUpdate')
    return
  }
  const confirmed = await confirmAction(t('adminSpecialists.updateConnection'), t('adminSpecialists.updateConnectionConfirm'))
  if (!confirmed) return
  try {
    await $fetch(`${config.public.apiBase}/specialists/connections/${selectedConnectionId.value}`, {
      method: 'PUT',
      body: {
        connectionTypeId: connectionForm.value.connectionTypeId,
        connectionValue: connectionForm.value.connectionValue,
      },
    })
    connectionActionMessage.value = t('adminSpecialists.connectionUpdated')
    resetConnectionForm()
    await loadSpecialistConnections()
  } catch (error) {
    connectionActionMessage.value = t('adminSpecialists.failedToUpdateConnection')
  }
}

const deleteSelectedConnection = async () => {
  if (!selectedConnectionId.value) {
    connectionActionMessage.value = t('adminSpecialists.selectConnectionToDelete')
    return
  }
  const confirmed = await confirmAction(t('adminSpecialists.deleteConnection'), t('adminSpecialists.deleteConnectionConfirm'))
  if (!confirmed) return
  try {
    await $fetch(`${config.public.apiBase}/specialists/connections/${selectedConnectionId.value}`, {
      method: 'DELETE',
    })
    connectionActionMessage.value = t('adminSpecialists.connectionDeleted')
    resetConnectionForm()
    await loadSpecialistConnections()
  } catch (error) {
    connectionActionMessage.value = t('adminSpecialists.failedToDeleteConnection')
  }
}

const removeConnection = async (id: string) => {
  const confirmed = await confirmAction(t('adminSpecialists.deleteConnection'), t('adminSpecialists.deleteConnectionConfirm'))
  if (!confirmed) return
  try {
    await $fetch(`${config.public.apiBase}/specialists/connections/${id}`, { method: 'DELETE' })
    await loadSpecialistConnections()
  } catch (error) {
    connectionActionMessage.value = t('adminSpecialists.failedToDeleteConnection')
  }
}

onMounted(() => {
  loadSpecialists()
  loadCategories()
  loadConnectionTypes()
})
</script>

<style scoped>
.section {
  margin-top: 1.25rem;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 1rem;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.section h2 {
  margin-bottom: 0.75rem;
  font-size: 1.25rem;
  color: #111827;
}

.search-bar {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
  align-items: center;
}

.search-input {
  flex: 1;
  padding: 0.6rem 0.9rem;
  border: 1px solid #cbd5f5;
  border-radius: 6px;
  font-size: 0.9rem;
  font-family: inherit;
}

.search-input:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.list-state {
  padding: 0.75rem 1rem;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  margin-bottom: 1rem;
}

.list-state.error {
  background: #fee2e2;
  color: #b91c1c;
}

.table {
  display: flex;
  flex-direction: column;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 1.5rem;
}

.table-header,
.table-row {
  display: grid;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  align-items: center;
  font-size: 0.85rem;
}

.table-header.specialists-table,
.table-row.specialists-table {
  grid-template-columns: 1.2fr 1.5fr 1fr 1.4fr 0.8fr 1.6fr 0.8fr 1.3fr;
}

.table-header {
  background: #f1f5f9;
  font-weight: 600;
  color: #1f2937;
}

.table-row {
  border-top: 1px solid #e5e7eb;
  background: #ffffff;
}

.row-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.pagination-info {
  font-weight: 600;
  color: #1f2937;
}

.pagination-controls {
  display: flex;
  gap: 0.5rem;
}

.pagination-size {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #374151;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e5e7eb;
}

.form-header h3 {
  font-size: 1.125rem;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.form-subtitle {
  font-size: 1rem;
  color: #4f46e5;
  font-weight: 600;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field--full {
  grid-column: 1 / -1;
}

.form-field label {
  font-weight: 600;
  color: #374151;
  font-size: 0.85rem;
}

.form-field input,
.form-field textarea,
.form-field select {
  padding: 0.45rem 0.6rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-family: inherit;
  font-size: 0.85rem;
}

.form-field input:focus,
.form-field textarea:focus,
.form-field select:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.form-message {
  margin-top: 0.5rem;
  color: #1f2937;
  padding: 0.75rem;
  background: #f0fdf4;
  border-left: 4px solid #22c55e;
  border-radius: 4px;
}

.btn {
  padding: 0.45rem 0.9rem;
  border-radius: 6px;
  border: 1px solid #cbd5f5;
  background: #ffffff;
  color: #1f2937;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.85rem;
  transition: all 0.2s;
}

.btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.btn.primary {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
}

.btn.primary:hover {
  background: #4338ca;
}

.btn.danger {
  background: #fee2e2;
  color: #b91c1c;
  border-color: #fecaca;
}

.btn.danger:hover:not(:disabled) {
  background: #fca5a5;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.details-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.tab {
  padding: 0.4rem 0.85rem;
  border-radius: 999px;
  border: 1px solid #cbd5f5;
  background: #ffffff;
  color: #1f2937;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.85rem;
}

.tab.active {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
}

.details-panel {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 0.75rem;
  background: #f8fafc;
}

.rate-header {
  display: grid;
  grid-template-columns: 1.2fr 0.6fr 0.6fr auto;
  gap: 0.75rem;
  font-weight: 600;
  font-size: 0.8rem;
  color: #475569;
}

.rate-row {
  display: grid;
  grid-template-columns: 1.2fr 0.6fr 0.6fr auto;
  gap: 0.75rem;
  align-items: center;
}

.rate-rows {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.connections-manager {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.specialist-connections-table {
  grid-template-columns: 1.2fr 1.6fr 0.6fr 1.2fr;
}

.muted {
  color: #6b7280;
  font-size: 0.9rem;
}

.notifications-manager {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.notification-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background: #f3f4f6;
}

.notification-label {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.notification-name {
  font-weight: 600;
  color: #1f2937;
  font-size: 0.9rem;
  margin-bottom: 0.15rem;
}

.notification-description {
  color: #6b7280;
  font-size: 0.8rem;
}

/* Toggle Switch Styles */
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 26px;
  flex-shrink: 0;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: 0.3s;
  border-radius: 26px;
}

.toggle-slider:before {
  position: absolute;
  content: '';
  height: 20px;
  width: 20px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
}

input:checked + .toggle-slider {
  background-color: #4f46e5;
}

input:checked + .toggle-slider:before {
  transform: translateX(24px);
}

input:disabled + .toggle-slider {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-message.success {
  background: #dcfce7;
  color: #166534;
  border-left: 4px solid #22c55e;
}

.form-message.error {
  background: #fee2e2;
  color: #b91c1c;
  border-left: 4px solid #dc2626;
}

/* Modal Dialog Styles */
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

.modal-dialog {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  max-width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease-out;
}

.modal-dialog h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.25rem;
  color: #111827;
}

.modal-dialog p {
  margin: 0 0 1.5rem 0;
  color: #6b7280;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.modal-actions .btn {
  flex: 1;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
