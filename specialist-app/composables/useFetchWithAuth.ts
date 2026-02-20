import { ref, type Ref } from "vue";
import { useApi } from "./useApi";

export interface FetchOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
  body?: Record<string, unknown>;
  headers?: Record<string, string>;
  requireAuth?: boolean;
}

export interface FetchResult<T> {
  data: Ref<T | null>;
  loading: Ref<boolean>;
  error: Ref<string>;
  execute: (options?: FetchOptions) => Promise<T | null>;
  refresh: () => Promise<T | null>;
}

export function useFetchWithAuth<T>(
  url: string | (() => string),
  options: FetchOptions = {},
): FetchResult<T> {
  const { $fetch } = useApi();
  const data = ref<T | null>(null) as Ref<T | null>;
  const loading = ref(false) as Ref<boolean>;
  const error = ref("") as Ref<string>;

  const getAuthHeaders = () => {
    const userId = sessionStorage.getItem("userId");
    const token =
      sessionStorage.getItem("accessToken") ||
      sessionStorage.getItem("sessionId");

    const headers: Record<string, string> = {};
    if (userId) headers["X-User-Id"] = userId;
    if (token) headers["Authorization"] = `Bearer ${token}`;

    return headers;
  };

  const resolveUrl = () => {
    return typeof url === "function" ? url() : url;
  };

  const execute = async (
    fetchOptions: FetchOptions = {},
  ): Promise<T | null> => {
    loading.value = true;
    error.value = "";

    try {
      const {
        method = options.method || "GET",
        body,
        headers = {},
      } = fetchOptions;

      const authHeaders = options.requireAuth !== false ? getAuthHeaders() : {};

      const result = await $fetch<T>(resolveUrl(), {
        method,
        body,
        headers: { ...authHeaders, ...headers },
      });

      data.value = result;
      return result;
    } catch (error: unknown) {
      error.value = error instanceof Error ? error.message : "An error occurred";
      return null;
    } finally {
      loading.value = false;
    }
  };

  const refresh = async (): Promise<T | null> => {
    return execute(options);
  };

  return {
    data,
    loading,
    error,
    execute,
    refresh,
  };
}
