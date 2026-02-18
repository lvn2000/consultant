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
  const { t, te } = useI18n();

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
      return te(key) ? t(key) : error.message;
    }

    const code = error.error;
    if (code) {
      const key = `errors.${code}`;
      if (te(key)) {
        return t(key);
      }
    }

    return error.message || t("errors.ERROR");
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

    return e?.message || t("errors.ERROR");
  };

  return {
    translateError,
    handleApiError,
    getErrorMessage,
    ApiError,
    ErrorCode,
  };
}
