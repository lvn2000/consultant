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

import { useRuntimeConfig, useFetch } from 'nuxt/app'

interface LoginResponse {
  userId: string
  login: string
  email: string
  role: string
  sessionId: string
  accessToken?: string
}

export async function loginRequest(login: string, password: string): Promise<{ success: boolean; error?: string }> {
  const config = useRuntimeConfig()
  try {
    // Call /api/auth/login which returns both sessionId (legacy) and accessToken (JWT)
    const data = await $fetch<LoginResponse>(`${config.public.apiBase}/auth/login`, {
      method: 'POST',
      body: { login, password },
    })

    if (data) {
      // Use JWT accessToken if available, otherwise fall back to sessionId
      const token = data.accessToken || data.sessionId
      sessionStorage.setItem('accessToken', token)
      // Keep sessionId for backward compatibility
      sessionStorage.setItem('sessionId', data.sessionId)
      sessionStorage.setItem('userId', data.userId)
      sessionStorage.setItem('login', data.login)
      sessionStorage.setItem('email', data.email)
      sessionStorage.setItem('role', data.role)
      return { success: true }
    }

    return { success: false, error: 'Invalid response' }
  } catch (e: any) {
    if (process.dev) console.error('Login error:', e.message)
    return { success: false, error: e.data?.message || e.message || 'Login failed' }
  }
}
