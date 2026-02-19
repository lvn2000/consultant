/**
 * Composable for managing specialist consultations.
 * Provides methods to load, approve, decline, and mark consultations as missed,
 * along with state for loading, error, and paginated consultations.
 */

import { ref, computed } from 'vue'
import { useRuntimeConfig } from 'nuxt/app'
import { useApi } from '../useApi'

export interface Consultation {
  id: string
  clientId: string
  categoryId: string
  scheduledAt: string
  status: string
  duration: number
  description?: string
  rating?: number
  review?: string
}

export function useConsultations() {
  const config = useRuntimeConfig()
  const { $fetch } = useApi()

  const consultations = ref<Consultation[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const currentPage = ref(1)
  const itemsPerPage = ref(10)

  /**
   * Loads consultations for the current specialist.
   */
  async function loadConsultations() {
    loading.value = true
    error.value = null
    try {
      const specialistId = sessionStorage.getItem('userId')
      if (!specialistId) {
        error.value = 'Specialist ID not found'
        consultations.value = []
        return
      }
      const data = await $fetch<Consultation[]>(`${config.public.apiBase}/consultations/specialist/${specialistId}`)
      consultations.value = Array.isArray(data) ? data : []
    } catch (e: any) {
      error.value = e.data?.message || e.message || 'Failed to load consultations'
      consultations.value = []
    } finally {
      loading.value = false
    }
  }

  /**
   * Approves a consultation with a specified duration.
   */
  async function approveConsultation(consultationId: string, duration: number): Promise<boolean> {
    try {
      await $fetch(`${config.public.apiBase}/consultations/${consultationId}/approve`, {
        method: 'POST',
        body: { duration }
      })
      await loadConsultations()
      return true
    } catch (e: any) {
      error.value = e.data?.message || e.message || 'Failed to approve consultation'
      return false
    }
  }

  /**
   * Declines a consultation.
   */
  async function declineConsultation(consultationId: string): Promise<boolean> {
    try {
      await $fetch(`${config.public.apiBase}/consultations/${consultationId}/decline`, {
        method: 'POST'
      })
      await loadConsultations()
      return true
    } catch (e: any) {
      error.value = e.data?.message || e.message || 'Failed to decline consultation'
      return false
    }
  }

  /**
   * Marks a consultation as missed.
   */
  async function markAsMissed(consultationId: string): Promise<boolean> {
    try {
      await $fetch(`${config.public.apiBase}/consultations/${consultationId}/missed`, {
        method: 'POST'
      })
      await loadConsultations()
      return true
    } catch (e: any) {
      error.value = e.data?.message || e.message || 'Failed to mark consultation as missed'
      return false
    }
  }

  /**
   * Returns paginated consultations for the current page.
   */
  const paginatedConsultations = computed(() => {
    const start = (currentPage.value - 1) * itemsPerPage.value
    const end = start + itemsPerPage.value
    return consultations.value.slice(start, end)
  })

  /**
   * Pagination info.
   */
  const pagination = computed(() => {
    const total = consultations.value.length
    const totalPages = Math.ceil(total / itemsPerPage.value)
    return {
      currentPage: currentPage.value,
      totalPages: totalPages || 1,
      totalCount: total
    }
  })

  /**
   * Changes the current page.
   */
  function goToPage(page: number) {
    if (page >= 1 && page <= pagination.value.totalPages) {
      currentPage.value = page
    }
  }

  return {
    consultations,
    loading,
    error,
    paginatedConsultations,
    pagination,
    loadConsultations,
    approveConsultation,
    declineConsultation,
    markAsMissed,
    goToPage,
    itemsPerPage,
    currentPage
  }
}
