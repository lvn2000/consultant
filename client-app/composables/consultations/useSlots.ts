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

/**
 * Composable for fetching and managing available consultation slots.
 * Provides methods to load slots for a specialist and category, and exposes loading/error state.
 */

import { ref } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "../useApi";

export interface AvailableSlot {
  id: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
}

export function useSlots() {
  const slots = ref<AvailableSlot[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const config = useRuntimeConfig();
  const { $fetch } = useApi();

  /**
   * Loads available slots for a given specialist and category.
   * @param specialistId - ID of the specialist
   * @param categoryId - ID of the category
   * @param date - Date string (optional, defaults to today)
   */
  async function loadSlots(
    specialistId: string,
    categoryId: string,
    date?: string,
  ) {
    loading.value = true;
    error.value = null;
    try {
      // Use today's date if no date provided
      const slotDate = date || new Date().toISOString().split("T")[0];

      const query = [`date=${encodeURIComponent(slotDate)}`];
      if (categoryId)
        query.push(`categoryId=${encodeURIComponent(categoryId)}`);
      const queryString = query.length ? `?${query.join("&")}` : "";
      const url = `${config.public.apiBase}/specialists/${specialistId}/availability/slots${queryString}`;
      const response = await $fetch<{ slots: AvailableSlot[] }>(url);
      // Map response to include id (using startTime as unique id)
      slots.value = (response?.slots || []).map((slot) => ({
        ...slot,
        id: slot.startTime,
      }));
    } catch (e: any) {
      error.value = e.data?.message || e.message || "Failed to load slots";
      slots.value = [];
    } finally {
      loading.value = false;
    }
  }

  return {
    slots,
    loading,
    error,
    loadSlots,
  };
}
