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

<template>
    <div class="slots-container">
        <div v-if="loading" class="slots-spinner">
            <span>{{ $t("consultations.loadingSlots") }}</span>
        </div>
        <div v-else-if="error" class="slots-error">
            <span>{{ error }}</span>
        </div>
        <div v-else-if="slots.length === 0" class="slots-empty">
            <span>{{ $t("consultations.noSlotsAvailable") }}</span>
        </div>
        <div v-else class="slots-grid">
            <button
                type="button"
                v-for="slot in slots"
                :key="slot.id"
                :class="[
                    'slot-button',
                    { selected: slot.id === selectedSlotId },
                ]"
                @click="selectSlot(slot.id)"
            >
                {{ formatSlot(slot) }}
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
interface Slot {
    id: string;
    startTime: string;
    endTime: string;
    durationMinutes: number;
}

const props = defineProps<{
    slots: Slot[];
    loading: boolean;
    error: string;
    selectedSlotId: string | null;
}>();

const emit = defineEmits<{
    (e: "select", slotId: string): void;
}>();

function selectSlot(slotId: string) {
    emit("select", slotId);
}

function formatSlot(slot: Slot): string {
    // Format: "14:00 - 15:00 (60 min)"
    return `${slot.startTime} - ${slot.endTime} (${slot.durationMinutes} min)`;
}
</script>

<style scoped>
.slots-container {
    margin-top: 1rem;
}

.slots-spinner,
.slots-error,
.slots-empty {
    text-align: center;
    color: #666;
    padding: 1rem;
}

.slots-error {
    color: #dc2626;
}

.slots-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
}

.slot-button {
    padding: 0.5rem 1rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    background: #fff;
    cursor: pointer;
    font-size: 0.95rem;
    transition:
        background 0.15s,
        border-color 0.15s;
}

.slot-button.selected {
    background: #4f46e5;
    color: #fff;
    border-color: #4f46e5;
}

.slot-button:hover:not(.selected) {
    background: #e0e7ff;
    border-color: #a5b4fc;
}
</style>
