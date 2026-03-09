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
 * Enhanced error handling composable with proper error types
 */

export enum ErrorCode {
  NOT_FOUND = "NOT_FOUND",
  UNAUTHORIZED = "UNAUTHORIZED",
  FORBIDDEN = "FORBIDDEN",
  VALIDATION_ERROR = "VALIDATION_ERROR",
  INVALID_CREDENTIALS = "INVALID_CREDENTIALS",
  CONFLICT = "CONFLICT",
  UNAVAILABLE = "UNAVAILABLE",
  DATABASE_ERROR = "DATABASE_ERROR",
  INTERNAL_ERROR = "INTERNAL_ERROR",
  MISSING_AUTH = "MISSING_AUTH",
  PERMISSION_DENIED = "PERMISSION_DENIED",
  SLOT_NOT_AVAILABLE = "SLOT_NOT_AVAILABLE",
  LEGACY_AUTH_DISABLED = "LEGACY_AUTH_DISABLED",
  REFRESH_ERROR = "REFRESH_ERROR",
  REGISTRATION_ERROR = "REGISTRATION_ERROR",
  LOGIN_ERROR = "LOGIN_ERROR",
  LOGOUT_ERROR = "LOGOUT_ERROR",
}

export class ApiError extends Error {
  constructor(
    public code: ErrorCode | string,
    public status: number,
    message: string,
    public details?: any,
  ) {
    super(message);
    this.name = "ApiError";
  }

  get isNotFound() {
    return this.status === 404 || this.code === ErrorCode.NOT_FOUND;
  }

  get isUnauthorized() {
    return this.status === 401 || this.code === ErrorCode.UNAUTHORIZED;
  }

  get isForbidden() {
    return this.status === 403 || this.code === ErrorCode.FORBIDDEN;
  }

  get isValidationError() {
    return this.status === 400 || this.code === ErrorCode.VALIDATION_ERROR;
  }

  get isConflict() {
    return this.status === 409 || this.code === ErrorCode.CONFLICT;
  }
}

export function useApiError() {
  // Note: useI18n() can only be called in Vue component setup context
  // We make it optional to allow this composable to work from Pinia stores
  let t: ((key: string) => string) | undefined;
  let te: ((key: string) => boolean) | undefined;
  try {
    const i18n = useI18n();
    t = i18n.t;
    te = i18n.te;
  } catch (e) {
    // i18n not available (e.g., called from Pinia store)
    t = undefined;
    te = undefined;
  }

  /**
   * Translate an error code to a localized message
   */
  const translateError = (
    error: ApiError | { error?: string; message?: string } | string,
  ): string => {
    if (typeof error === "string") {
      return error;
    }

    if (error instanceof ApiError) {
      const key = `errors.${error.code}`;
      return (te?.(key) && t?.(key)) ? t(key) : error.message;
    }

    const code = error.error;
    if (code) {
      const key = `errors.${code}`;
      if (te?.(key)) {
        const translated = t?.(key);
        if (translated) return translated;
      }
    }

    return error.message || t?.("errors.ERROR") || "An error occurred";
  };

  /**
   * Extract error data from a caught exception and create ApiError
   */
  const handleApiError = (e: any): ApiError => {
    const data = e?.data || e?.response?._data;
    const status = e?.status || e?.response?.status || 500;

    const code = data?.error || data?.code || mapStatusToErrorCode(status);
    const message = data?.message || e?.message || "An error occurred";

    return new ApiError(code, status, message, data);
  };

  /**
   * Map HTTP status codes to error codes
   */
  const mapStatusToErrorCode = (status: number): ErrorCode => {
    switch (status) {
      case 400:
        return ErrorCode.VALIDATION_ERROR;
      case 401:
        return ErrorCode.UNAUTHORIZED;
      case 403:
        return ErrorCode.FORBIDDEN;
      case 404:
        return ErrorCode.NOT_FOUND;
      case 409:
        return ErrorCode.CONFLICT;
      case 500:
        return ErrorCode.DATABASE_ERROR;
      default:
        return ErrorCode.INTERNAL_ERROR;
    }
  };

  /**
   * Get a user-friendly error message from an exception
   */
  const getErrorMessage = (e: any): string => {
    if (e instanceof ApiError) {
      return translateError(e);
    }

    const data = e?.data || e?.response?._data;
    if (data?.error) {
      return translateError({ error: data.error, message: data.message });
    }

    return e?.message || t?.("errors.ERROR") || "An error occurred";
  };

  return {
    translateError,
    handleApiError,
    getErrorMessage,
    ApiError,
    ErrorCode,
  };
}
