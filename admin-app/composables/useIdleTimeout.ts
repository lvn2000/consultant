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
 * Composable for handling idle timeout and automatic logout
 * Tracks user activity and logs out after configured period of inactivity
 */
import { ref, computed, onMounted, onUnmounted } from "vue";

export const useIdleTimeout = () => {
  const config = useRuntimeConfig();
  const { $fetch } = useApi();
  const router = useRouter();

  const idleTimeoutMinutes = ref<number>(30);
  const idleWarningMinutes = ref<number>(5);
  const remainingSeconds = ref<number>(0);
  const isWarningVisible = ref<boolean>(false);
  const isActive = ref<boolean>(false);
  const countdownInterval = ref<number | null>(null);
  // Separate refs for warning and logout timers
  const warningTimeout = ref<number | null>(null);
  const logoutTimeout = ref<number | null>(null);

  // Activity events to track
  const activityEvents = [
    "mousedown",
    "mousemove",
    "keypress",
    "scroll",
    "touchstart",
    "click",
    "wheel",
  ];

  // Handler for settings updates
  const handleSettingsUpdate = (event: CustomEvent) => {
    const newConfig = event.detail;
    if (newConfig) {
      idleTimeoutMinutes.value = newConfig.idleTimeoutMinutes;
      idleWarningMinutes.value = newConfig.idleWarningMinutes;
      if (isActive.value) {
        resetTimer();
      }
    }
  };

  /**
   * Load idle timeout configuration from API
   */
  const loadConfig = async () => {
    try {
      const response = await $fetch<{
        idleTimeoutMinutes: number;
        idleWarningMinutes: number;
      }>(`${config.public.apiBase}/settings/idle-timeout`);
      idleTimeoutMinutes.value = response.idleTimeoutMinutes;
      idleWarningMinutes.value = response.idleWarningMinutes;
    } catch {
      idleTimeoutMinutes.value = 30;
      idleWarningMinutes.value = 5;
    }
  };

  /**
   * Reset the activity timer
   */
  const resetTimer = () => {
    // Clear previous timers
    if (warningTimeout.value) {
      clearTimeout(warningTimeout.value);
      warningTimeout.value = null;
    }
    if (logoutTimeout.value) {
      clearTimeout(logoutTimeout.value);
      logoutTimeout.value = null;
    }

    // Start countdown for warning
    const warningTime =
      (idleTimeoutMinutes.value - idleWarningMinutes.value) * 60 * 1000;
    const timeoutTime = idleTimeoutMinutes.value * 60 * 1000;

    warningTimeout.value = window.setTimeout(() => {
      showWarning();
    }, warningTime);

    logoutTimeout.value = window.setTimeout(() => {
      performLogout();
    }, timeoutTime);
  };

  /**
   * Show warning modal before logout
   */
  const showWarning = () => {
    isWarningVisible.value = true;
    remainingSeconds.value = idleWarningMinutes.value * 60;

    // Start countdown
    countdownInterval.value = window.setInterval(() => {
      remainingSeconds.value--;

      if (remainingSeconds.value <= 0) {
        performLogout();
      }
    }, 1000);
  };

  /**
   * Hide warning and reset timer (user is still active)
   */
  const hideWarning = () => {
    isWarningVisible.value = false;

    if (countdownInterval.value) {
      clearInterval(countdownInterval.value);
      countdownInterval.value = null;
    }

    resetTimer();
  };

  /**
   * Perform logout
   */
  const performLogout = async () => {
    if (countdownInterval.value) {
      clearInterval(countdownInterval.value);
      countdownInterval.value = null;
    }
    if (warningTimeout.value) {
      clearTimeout(warningTimeout.value);
      warningTimeout.value = null;
    }
    if (logoutTimeout.value) {
      clearTimeout(logoutTimeout.value);
      logoutTimeout.value = null;
    }

    // Clear session and redirect to login
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("sessionId");
    sessionStorage.removeItem("userId");
    sessionStorage.removeItem("login");
    sessionStorage.removeItem("email");
    sessionStorage.removeItem("role");
    localStorage.removeItem("admin_session");

    router.push("/login");
  };

  /**
   * Handle activity event
   */
  const handleActivity = () => {
    if (isActive.value) {
      resetTimer();
    }
  };

  /**
   * Start tracking idle timeout
   */
  const start = async () => {
    // Only track if user is logged in
    const token = sessionStorage.getItem("accessToken");

    if (!token) {
      return;
    }

    await loadConfig();
    isActive.value = true;

    // Add event listeners
    activityEvents.forEach((event) => {
      window.addEventListener(event, handleActivity);
    });

    // Listen for settings updates
    window.addEventListener(
      "idle-timeout-updated",
      handleSettingsUpdate as EventListener,
    );

    resetTimer();
  };

  /**
   * Stop tracking idle timeout
   */
  const stop = () => {
    isActive.value = false;

    // Clear timers
    if (countdownInterval.value) {
      clearInterval(countdownInterval.value);
      countdownInterval.value = null;
    }
    if (warningTimeout.value) {
      clearTimeout(warningTimeout.value);
      warningTimeout.value = null;
    }
    if (logoutTimeout.value) {
      clearTimeout(logoutTimeout.value);
      logoutTimeout.value = null;
    }

    // Remove event listeners
    activityEvents.forEach((event) => {
      window.removeEventListener(event, handleActivity);
    });

    // Remove settings update listener
    window.removeEventListener(
      "idle-timeout-updated",
      handleSettingsUpdate as EventListener,
    );
  };

  /**
   * Format remaining time as MM:SS
   */
  const formatRemainingTime = computed(() => {
    const minutes = Math.floor(remainingSeconds.value / 60);
    const seconds = remainingSeconds.value % 60;
    return `${minutes.toString().padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`;
  });

  // Auto-start on mount if in browser
  // Note: Now started explicitly from main.vue
  if (import.meta.client) {
    // onMounted(() => {
    //   start();
    // });

    onUnmounted(() => {
      stop();
    });
  }

  return {
    isWarningVisible,
    remainingSeconds,
    formatRemainingTime,
    idleTimeoutMinutes,
    idleWarningMinutes,
    hideWarning,
    performLogout,
    reloadConfig: loadConfig,
    start,
    stop,
  };
};
