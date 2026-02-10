import { $fetch as ofetch } from 'ofetch'

export function useApi() {
  async function $fetch<T>(url: string, options?: any): Promise<T> {
    // Guard sessionStorage access behind process.client to prevent SSR errors
    // sessionStorage is only available in the browser, not on the server
    let token: string | null = null
    if (process.client) {
      const accessToken = sessionStorage.getItem('accessToken')
      const sessionId = sessionStorage.getItem('sessionId')
      token = accessToken || sessionId
    }

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...options?.headers
    }

    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    try {
      return await ofetch<T>(url, {
        ...options,
        headers
      })
    } catch (error: any) {
      if (process.dev) {
        console.error(`[API Error] ${options?.method || 'GET'} ${url}:`, error?.message || error)
      }
      throw error
    }
  }

  return { $fetch }
}
