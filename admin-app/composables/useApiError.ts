/**
 * Composable for translating API error codes to localized messages.
 * Maps backend error codes (e.g. NOT_FOUND, VALIDATION_ERROR) to i18n keys.
 */
export function useApiError() {
  const { t, te } = useI18n()

  /**
   * Translate an API error response to a localized message.
   * Falls back to the original message if no translation key exists.
   */
  function translateError(error: { error?: string; message?: string } | string): string {
    if (typeof error === 'string') {
      return error
    }

    const code = error.error
    if (code) {
      const key = `errors.${code}`
      if (te(key)) {
        return t(key)
      }
    }

    return error.message || t('errors.ERROR')
  }

  /**
   * Extract and translate error from a caught exception (e.g. from $fetch).
   */
  function getErrorMessage(e: any): string {
    const data = e?.data || e?.response?._data
    if (data?.error) {
      return translateError(data)
    }
    return e?.message || t('errors.ERROR')
  }

  return { translateError, getErrorMessage }
}
