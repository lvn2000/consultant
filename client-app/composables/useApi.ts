import { useRuntimeConfig } from 'nuxt/app'
import { $fetch as ofetch } from 'ofetch'

export function useApi() {
  const config = useRuntimeConfig()

  /**
   * Make authenticated API request with Bearer token
   * Uses JWT accessToken as the bearer token for authorization
   */
  async function $fetch<T>(
    url: string,
    options?: any
  ): Promise<T> {
    const accessToken = sessionStorage.getItem('accessToken')
    
    // If we have an accessToken, add it as Bearer token
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...options?.headers
    }

    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`
    }

    console.log(`[API] ${options?.method || 'GET'} ${url}`, { headers })

    try {
      const response = await ofetch<T>(url, {
        ...options,
        headers
      })
      return response
    } catch (error: any) {
      console.error(`[API Error] ${options?.method || 'GET'} ${url}:`, error)
      throw error
    }
  }

  return { $fetch }
}
