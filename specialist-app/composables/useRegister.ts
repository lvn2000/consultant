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
  } catch (e: any) {
    if (process.dev) console.error('Register error:', e)
    const msg =
      e.data?.message || e.data?.error || e.message || 'Registration failed'
    return { success: false, error: msg }
  }
}
