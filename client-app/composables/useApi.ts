import { $fetch as ofetch } from 'ofetch'

export function useApi() {

  /**
   * Make authenticated API request with Bearer token
   * Uses JWT accessToken as the bearer token for authorization
   */
  async function $fetch<T>(
    url: string,
    options?: any
  ): Promise<T> {
    const accessToken = sessionStorage.getItem('accessToken')
    
    // Merge headers: start with defaults, merge custom headers, then add Authorization
    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    }

    // Merge any custom headers from options
    if (options?.headers) {
      Object.assign(headers, options.headers)
    }

    // Add Authorization header if we have a token (this takes precedence)
    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`
    }

    if (process.dev) {
      console.log(`[API] ${options?.method || 'GET'} ${url}`)
    }

    try {
      const response = await ofetch<T>(url, {
        ...options,
        headers
      })
      return response
    } catch (error: any) {
      if (process.dev) {
        console.error(`[API Error] ${options?.method || 'GET'} ${url}:`, error?.message || error)
      }
      throw error
    }
  }

  return { $fetch }
}
