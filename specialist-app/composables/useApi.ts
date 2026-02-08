import { $fetch as ofetch } from 'ofetch'

export function useApi() {
  async function $fetch<T>(url: string, options?: any): Promise<T> {
    const accessToken = sessionStorage.getItem('accessToken')
    const sessionId = sessionStorage.getItem('sessionId')
    const token = accessToken || sessionId

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
      console.error(`[API Error] ${options?.method || 'GET'} ${url}:`, error)
      throw error
    }
  }

  return { $fetch }
}
