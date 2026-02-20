import { useRuntimeConfig } from 'nuxt/app'

// Types for login
interface LoginResponse {
  userId: string
  login: string
  email: string
  role: string
  sessionId: string
  accessToken?: string
}

// Utility composable for login API
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
      // Store session data
      sessionStorage.setItem('userId', data.userId)
      sessionStorage.setItem('login', data.login)
      sessionStorage.setItem('email', data.email)
      sessionStorage.setItem('role', data.role)
      sessionStorage.setItem('sessionId', data.sessionId)
      return { success: true }
    }

    return { success: false, error: 'Invalid response' }
  } catch (e: any) {
    return { success: false, error: e.data?.message || e.message || 'Login failed' }
  }
}
