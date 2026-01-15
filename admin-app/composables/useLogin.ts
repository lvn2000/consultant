import { useRuntimeConfig, useFetch } from 'nuxt/app'

// Types for login
interface LoginResponse {
  userId: string
  login: string
  email: string
  role: string
}

// Utility composable for login API
export async function loginRequest(login: string, password: string): Promise<{ success: boolean; error?: string }> {
  const config = useRuntimeConfig()
  try {
    const { data, error } = await useFetch<LoginResponse>(`${config.public.apiBase}/users/login`, {
      method: 'POST',
      body: JSON.stringify({ login, password }),
      headers: {
        'Content-Type': 'application/json',
      },
    })

    if (error.value) {
      return { success: false, error: 'Login failed' }
    }

    if (data.value) {
      // Store session data
      sessionStorage.setItem('userId', data.value.userId)
      sessionStorage.setItem('login', data.value.login)
      sessionStorage.setItem('email', data.value.email)
      sessionStorage.setItem('role', data.value.role)
      return { success: true }
    }

    return { success: false, error: 'Invalid response' }
  } catch (e) {
    return { success: false, error: 'Network error' }
  }
}
