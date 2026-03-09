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

  /**
   * Make authenticated API request with Bearer token
   * Uses JWT accessToken as the bearer token for authorization
   */
  async function $fetch<T>(
    url: string,
    options?: any
  ): Promise<T> {
    // Guard sessionStorage access behind process.client to prevent SSR errors
    // sessionStorage is only available in the browser, not on the server
    let accessToken: string | null = null
    if (process.client) {
      accessToken = sessionStorage.getItem('accessToken')
    }
    
    // Merge headers: start with defaults, merge custom headers, then add Authorization
    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    }

    // Merge any custom headers from options
    if (options?.headers) {
      Object.assign(headers, options.headers)
    }

    // Add Authorization header if we have a token (this takes precedence)
    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`
    }

    if (process.dev) {
      console.log(`[API] ${options?.method || 'GET'} ${url}`)
    }

    try {
      const response = await ofetch<T>(url, {
        ...options,
        headers
      })
      return response
    } catch (error: any) {
      if (process.dev) {
        console.error(`[API Error] ${options?.method || 'GET'} ${url}:`, error?.message || error)
      }
      throw error
    }
  }

  return { $fetch }
}
