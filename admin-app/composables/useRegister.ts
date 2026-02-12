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

/** Response from the admin-only registration endpoint (no auto-login) */
interface AdminRegisterResponse {
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
 * Public self-registration via POST /api/auth/register.
 * Only Client and Specialist roles are allowed.
 */
export async function registerRequest(
  params: RegisterParams
): Promise<{ success: boolean; error?: string }> {
  const config = useRuntimeConfig()
  try {
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
      sessionStorage.setItem('role', data.role)
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

/**
 * Admin-only registration via POST /api/auth/register-by-admin.
 * Requires admin JWT. Can create any role (Client, Specialist, Admin).
 */
export async function adminRegisterRequest(
  params: RegisterParams
): Promise<{ success: boolean; user?: AdminRegisterResponse; error?: string }> {
  const config = useRuntimeConfig()
  try {
    const accessToken = sessionStorage.getItem('accessToken')
    const callerRole = sessionStorage.getItem('role') || ''

    const data = await $fetch<AdminRegisterResponse>(
      `${config.public.apiBase}/auth/register-by-admin`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
          'X-User-Role': callerRole,
        },
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

    if (data && data.userId) {
      return { success: true, user: data }
    }

    return { success: false, error: 'Invalid response' }
  } catch (e: any) {
    if (process.dev) console.error('Admin register error:', e)
    const msg =
      e.data?.message || e.data?.error || e.message || 'Registration failed'
    return { success: false, error: msg }
  }
}
