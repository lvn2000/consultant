/**
 * Enhanced API composable with better type safety and error handling
 */
import { ofetch } from "ofetch";
import type { FetchOptions } from "ofetch";
import { useApiError } from "./useApiError";

interface ApiFetchOptions<T = any> extends Omit<
  FetchOptions,
  "baseURL" | "headers"
> {
  method?: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
  body?: T;
  query?: Record<string, any>;
  requiresAuth?: boolean;
}

export function useApi() {
  const { handleApiError } = useApiError();
  const config = useRuntimeConfig();

  /**
   * Enhanced fetch with automatic auth token handling and error processing
   */
  async function $fetch<T>(
    url: string,
    options: ApiFetchOptions = {},
  ): Promise<T> {
    const {
      method = "GET",
      body,
      headers = {},
      query,
      requiresAuth = true,
      ...fetchOptions
    } = options;

    // Get auth token from session storage (client-side only)
    let token: string | null = null;
    if (process.client && requiresAuth) {
      token =
        sessionStorage.getItem("accessToken") ||
        sessionStorage.getItem("sessionId");
    }

    const fullUrl = url.startsWith("http")
      ? url
      : `${config.public.apiBase}${url}`;

    try {
      return await ofetch<T>(fullUrl, {
        method,
        headers: {
          "Content-Type": "application/json",
          ...(token && { Authorization: `Bearer ${token}` }),
          ...headers,
        },
        body: body ? JSON.stringify(body) : undefined,
        query,
        ...fetchOptions,
        onResponseError({ response }) {
          // Handle 401 unauthorized - redirect to login
          if (response.status === 401 && process.client) {
            sessionStorage.clear();
            localStorage.removeItem("admin_session");
            window.location.href = "/login";
          }
        },
      });
    } catch (error: any) {
      throw handleApiError(error);
    }
  }

  return { $fetch };
}
