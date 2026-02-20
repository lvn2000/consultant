<template>
  <div class="consultation-list">
    <div v-if="loading" class="list-state">
      <span>{{ loadingText }}</span>
    </div>
    <div v-else-if="error" class="list-state error">
      <span>{{ error }}</span>
    </div>
    <div v-else-if="consultations.length === 0" class="empty-state">
      <span>{{ emptyText }}</span>
    </div>
    <div v-else>
      <div v-for="consultation in consultations" :key="consultation.id" class="consultation-item">
        <slot name="item" :consultation="consultation">
          <!-- Default rendering if no slot provided -->
          <div class="consultation-header">
            <span class="consultation-title">{{ consultation.description }}</span>
            <span class="consultation-status" :class="consultation.status.toLowerCase()">
              {{ consultation.status }}
            </span>
          </div>
          <div class="consultation-details">
            <span>{{ consultation.clientName }}</span>
            <span>{{ consultation.categoryName }}</span>
            <span>{{ formatDateTime(consultation.scheduledAt) }}</span>
          </div>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue'

interface Consultation {
  id: string
  description: string
  status: string
  clientName?: string
  categoryName?: string
  scheduledAt?: string
}

const props = defineProps<{
  consultations: Consultation[]
  loading: boolean
  error: string
  loadingText?: string
  emptyText?: string
}>()

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString()
}
</script>

<style scoped>
.consultation-list {
  margin-top: 1rem;
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

.empty-state {
  padding: 2rem;
  text-align: center;
  color: #999;
  background: #f3f4f6;
  border-radius: 8px;
}

.consultation-item {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.consultation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.consultation-title {
  font-weight: 600;
  color: #1f2937;
}

.consultation-status {
  padding: 0.375rem 0.75rem;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 500;
  text-transform: uppercase;
  color: white;
}

.consultation-status.requested { background: #fbbf24; }
.consultation-status.scheduled { background: #60a5fa; }
.consultation-status.inprogress { background: #34d399; }
.consultation-status.completed { background: #10b981; }
.consultation-status.missed { background: #ef4444; }
.consultation-status.cancelled { background: #6b7280; }

.consultation-details {
  display: flex;
  gap: 1rem;
  color: #666;
  font-size: 0.9rem;
}
</style>
