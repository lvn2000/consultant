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

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSpecialistsStore } from '~/stores/specialists'
import type { Specialist } from '~/types/api'

// Mock the useApi composable
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({
    $fetch: vi.fn(),
  }),
}))

// Mock useRuntimeConfig
vi.mock('nuxt/app', () => ({
  useRuntimeConfig: () => ({
    public: {
      apiBase: 'http://localhost:8090/api',
    },
  }),
}))

describe('Specialists Store', () => {
  let store: ReturnType<typeof useSpecialistsStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useSpecialistsStore()
  })

  const mockSpecialists: Specialist[] = [
    {
      id: '1',
      login: 'john.doe',
      email: 'john@example.com',
      name: 'John Doe',
      phone: '+1234567890',
      role: 'Specialist',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      isAvailable: true,
      bio: 'Experienced consultant',
      categoryRates: [
        {
          categoryId: 'cat1',
          hourlyRate: 100,
          experienceYears: 5,
        },
      ],
      consultationIds: [],
    },
    {
      id: '2',
      login: 'jane.smith',
      email: 'jane@example.com',
      name: 'Jane Smith',
      phone: '+0987654321',
      role: 'Specialist',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      isAvailable: false,
      bio: 'Expert in multiple fields',
      categoryRates: [],
      consultationIds: [],
    },
  ]

  describe('State', () => {
    it('should initialize with default state', () => {
      expect(store.items).toEqual([])
      expect(store.loading).toBe(false)
      expect(store.error).toBe(null)
      expect(store.pagination).toEqual({
        currentPage: 1,
        pageSize: 20,
        total: 0,
      })
      expect(store.searchQuery).toBe('')
      expect(store.selectedId).toBe(null)
    })
  })

  describe('Getters', () => {
    beforeEach(() => {
      store.items = mockSpecialists
    })

    it('should return all items when no search query', () => {
      store.searchQuery = ''
      expect(store.filteredSpecialists).toEqual(mockSpecialists)
    })

    it('should filter specialists by name', () => {
      store.searchQuery = 'john'
      expect(store.filteredSpecialists).toHaveLength(1)
      expect(store.filteredSpecialists[0].name).toBe('John Doe')
    })

    it('should filter specialists by email', () => {
      store.searchQuery = 'jane@example.com'
      expect(store.filteredSpecialists).toHaveLength(1)
      expect(store.filteredSpecialists[0].email).toBe('jane@example.com')
    })

    it('should filter specialists by phone', () => {
      store.searchQuery = '+1234'
      expect(store.filteredSpecialists).toHaveLength(1)
      expect(store.filteredSpecialists[0].phone).toBe('+1234567890')
    })

    it('should paginate specialists', () => {
      store.pagination.currentPage = 1
      store.pagination.pageSize = 1
      expect(store.pagedSpecialists).toHaveLength(1)
      expect(store.pagedSpecialists[0].id).toBe('1')
    })

    it('should calculate total pages', () => {
      store.pagination.pageSize = 1
      expect(store.totalPages).toBe(2)
    })

    it('should identify last page', () => {
      store.pagination.pageSize = 1
      store.pagination.currentPage = 2
      expect(store.isLastPage).toBe(true)
    })

    it('should get selected specialist', () => {
      store.selectedId = '1'
      expect(store.selectedSpecialist?.id).toBe('1')
    })

    it('should get specialist by ID', () => {
      const specialist = store.getSpecialistById('2')
      expect(specialist?.name).toBe('Jane Smith')
    })
  })

  describe('Actions', () => {
    it('should fetch specialists successfully', async () => {
      const { useApi } = await import('~/composables/useApi')
      vi.mocked(useApi).mockReturnValue({
        $fetch: vi.fn().mockResolvedValue(mockSpecialists),
      })

      await store.fetchSpecialists()

      expect(store.items).toEqual(mockSpecialists)
      expect(store.loading).toBe(false)
      expect(store.error).toBe(null)
    })

    it('should handle fetch error', async () => {
      const { useApi } = await import('~/composables/useApi')
      vi.mocked(useApi).mockReturnValue({
        $fetch: vi.fn().mockRejectedValue({
          data: { message: 'Network error' },
        }),
      })

      await store.fetchSpecialists()

      expect(store.items).toEqual([])
      expect(store.loading).toBe(false)
      expect(store.error).toBe('Network error')
    })

    it('should set selected specialist', () => {
      store.setSelectedSpecialist('1')
      expect(store.selectedId).toBe('1')
    })

    it('should clear selected specialist', () => {
      store.setSelectedSpecialist('1')
      store.setSelectedSpecialist(null)
      expect(store.selectedId).toBe(null)
    })

    it('should set search query and reset pagination', () => {
      store.pagination.currentPage = 5
      store.setSearchQuery('test')
      expect(store.searchQuery).toBe('test')
      expect(store.pagination.currentPage).toBe(1)
    })

    it('should set page size and reset to first page', () => {
      store.pagination.currentPage = 3
      store.setPageSize(50)
      expect(store.pagination.pageSize).toBe(50)
      expect(store.pagination.currentPage).toBe(1)
    })

    it('should go to previous page', () => {
      store.pagination.currentPage = 2
      store.previousPage()
      expect(store.pagination.currentPage).toBe(1)
    })

    it('should not go below page 1', () => {
      store.pagination.currentPage = 1
      store.previousPage()
      expect(store.pagination.currentPage).toBe(1)
    })

    it('should go to next page', () => {
      store.pagination.currentPage = 1
      store.pagination.total = 50
      store.pagination.pageSize = 10
      store.nextPage()
      expect(store.pagination.currentPage).toBe(2)
    })

    it('should reset state', () => {
      store.items = mockSpecialists
      store.selectedId = '1'
      store.searchQuery = 'test'
      store.$reset()

      expect(store.items).toEqual([])
      expect(store.selectedId).toBe(null)
      expect(store.searchQuery).toBe('')
    })
  })
})
