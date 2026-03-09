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

interface RegisterResponse {
  accessToken: string
  refreshToken: string
  expiresAt: string
  userId: string
  login: string
  email: string
  name: string
  role: string
}

export interface RegisterParams {
  login: string
  email: string
  password: string
  name: string
  phone?: string
  role: string
}

/**
 * Register a new user via POST /api/auth/register (public endpoint).
 * On success the response contains JWT tokens — we store them and return success.
 */
export async function registerRequest(
  params: RegisterParams
): Promise<{ success: boolean; error?: string }> {
  const config = useRuntimeConfig()
  try {
    // Register user
    const data = await $fetch<RegisterResponse>(
      `${config.public.apiBase}/auth/register`,
      {
        method: 'POST',
        body: {
          login: params.login,
          email: params.email,
          password: params.password,
          name: params.name,
          phone: params.phone || null,
          role: params.role,
        },
      }
    )

    if (data && data.accessToken) {
      sessionStorage.setItem('accessToken', data.accessToken)
      sessionStorage.setItem('userId', data.userId)
      sessionStorage.setItem('login', data.login)
      sessionStorage.setItem('email', data.email)
      sessionStorage.setItem('role', data.role.toLowerCase())

      // Backend creates specialist profile for specialist role during registration.
      // Specialist id is aligned with user id in backend authorization flow.
      if (params.role.toLowerCase() === 'specialist') {
        sessionStorage.setItem('specialistId', data.userId)
      }

      return { success: true }
    }

    return { success: false, error: 'Invalid response' }
  } catch (error: unknown) {
    if (process.dev) console.error('Register error:', e)
    const msg =
      error instanceof Error ? error.message : 'Registration failed'
    return { success: false, error: msg }
  }
}
