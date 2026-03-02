/**
 * Composable for handling consultation booking logic.
 * Provides functions to create a new consultation and manage booking state.
 */

import { ref } from "vue";
import { useRuntimeConfig } from "nuxt/app";
import { useApi } from "../useApi";

export interface BookingParams {
  specialistId: string;
  categoryId: string;
  description: string;
  scheduledAt: string; // ISO datetime string
  duration: number;
}

export interface BookingResult {
  success: boolean;
  error?: string;
}

export function useBooking() {
  const bookingLoading = ref(false);
  const bookingError = ref<string | null>(null);
  const bookingSuccess = ref(false);
  const bookingMessage = ref<string>("");
  const { $fetch } = useApi();
  const config = useRuntimeConfig();

  /**
   * Create a new consultation booking.
   */
  async function createBooking(params: BookingParams): Promise<BookingResult> {
    bookingLoading.value = true;
    bookingError.value = null;
    bookingSuccess.value = false;
    bookingMessage.value = "";

    try {
      const userId = sessionStorage.getItem("userId");
      if (!userId) {
        bookingError.value = "User ID not found";
        bookingMessage.value = bookingError.value;
        return { success: false, error: bookingError.value };
      }

      // Build request body - only include required fields
      const requestBody: any = {
        userId,
        specialistId: params.specialistId,
        categoryId: params.categoryId,
        description: params.description,
        scheduledAt: params.scheduledAt,
      };

      // Only include duration if it's provided and valid
      if (params.duration && params.duration > 0) {
        requestBody.duration = params.duration;
      }

      await $fetch(`${config.public.apiBase}/consultations`, {
        method: "POST",
        body: requestBody,
      });

      bookingSuccess.value = true;
      bookingMessage.value = "Consultation booked successfully!";
      return { success: true };
    } catch (error: any) {
      const errorMsg = error.data?.message || error.message || "Booking failed";
      bookingError.value = errorMsg;
      bookingMessage.value = errorMsg;
      return { success: false, error: errorMsg };
    } finally {
      bookingLoading.value = false;
    }
  }

  return {
    bookingLoading,
    bookingError,
    bookingSuccess,
    bookingMessage,
    createBooking,
  };
}
