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
