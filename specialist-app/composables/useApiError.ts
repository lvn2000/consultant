/**
 * Composable for translating API error codes to localized messages.
 * Maps backend error codes (e.g. NOT_FOUND, VALIDATION_ERROR) to i18n keys.
 */
export function useApiError() {
  const { t, te } = useI18n();

  /**
   * Translate an API error response to a localized message.
   * Falls back to the original message if no translation key exists.
   */
  function translateError(
    error: { error?: string; message?: string } | string,
  ): string {
    if (typeof error === "string") {
      return error;
    }

    const code = error.error;
    if (code) {
      const key = `errors.${code}`;
      if (te(key)) {
        return t(key);
      }
    }

    return error.message || t("errors.ERROR");
  }

  /**
   * Extract and translate error from a caught exception (e.g. from $fetch).
   */
  function getErrorMessage(e: unknown): string {
    const data =
      (e as Record<string, unknown>)?.data ||
      (e as Record<string, unknown>)?.response?._data;
    if (data && typeof data === "object" && "error" in data) {
      return translateError(data as { error?: string; message?: string });
    }
    return (e as Error)?.message || t("errors.ERROR");
  }

  return { translateError, getErrorMessage };
}
