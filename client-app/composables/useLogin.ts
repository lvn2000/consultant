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
    // Call /api/users/login which returns both sessionId (legacy) and accessToken (JWT)
    const data = await $fetch<LoginResponse>(`${config.public.apiBase}/users/login`, {
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
