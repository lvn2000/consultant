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
