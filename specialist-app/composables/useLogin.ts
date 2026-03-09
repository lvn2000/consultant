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

import { useRuntimeConfig } from 'nuxt/app'

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
    const normalizedLogin = login.trim().toLowerCase()
    const normalizedPassword = password.trim()

    if (!normalizedLogin || !normalizedPassword) {
      return { success: false, error: 'Please enter login and password' }
    }

    const data = await $fetch<LoginResponse>(`${config.public.apiBase}/auth/login`, {
      method: 'POST',
      body: { login: normalizedLogin, password: normalizedPassword },
    })

    if (data) {
      const token = data.accessToken || data.sessionId
      if (token) {
        sessionStorage.setItem('accessToken', token)
      }
      sessionStorage.setItem('userId', data.userId)
      sessionStorage.setItem('login', data.login)
      sessionStorage.setItem('email', data.email)
      const normalizedRole = data.role.toLowerCase()
      sessionStorage.setItem('role', normalizedRole)
      sessionStorage.setItem('sessionId', data.sessionId)

      // Keep specialist identity in sync per active session.
      if (normalizedRole === 'specialist') {
        sessionStorage.setItem('specialistId', data.userId)
      } else {
        sessionStorage.removeItem('specialistId')
      }

      return { success: true }
    }

    return { success: false, error: 'Invalid response' }
  } catch (error: unknown) {
    console.error('Login error:', e)
    return { success: false, error: error instanceof Error ? error.message : 'Login failed' }
  }
}
