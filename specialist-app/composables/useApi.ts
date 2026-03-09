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

import { $fetch as ofetch } from 'ofetch'

export function useApi() {
  async function $fetch<T>(url: string, options?: Record<string, unknown>): Promise<T> {
    // Guard sessionStorage access behind process.client to prevent SSR errors
    // sessionStorage is only available in the browser, not on the server
    let token: string | null = null
    if (process.client) {
      const accessToken = sessionStorage.getItem('accessToken')
      const sessionId = sessionStorage.getItem('sessionId')
      token = accessToken || sessionId
    }

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...options?.headers
    }

    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    try {
      return await ofetch<T>(url, {
        ...options,
        headers
      })
    } catch (error: unknown) {
      if (process.dev) {
        console.error(`[API Error] ${(options as Record<string, unknown>)?.method || 'GET'} ${url}:`, error instanceof Error ? error.message : error)
      }
      throw error
    }
  }

  return { $fetch }
}
