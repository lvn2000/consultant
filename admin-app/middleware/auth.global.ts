import { defineNuxtRouteMiddleware, navigateTo } from 'nuxt/app'

export default defineNuxtRouteMiddleware((to: any) => {
  if (process.client) {
    const publicPages = ['/login']
    const isLoggedIn = !!localStorage.getItem('admin_session')

    // Admin app auth flow is login-only (no public self-registration page)
    if (to.path === '/register') {
      return navigateTo(isLoggedIn ? '/main' : '/login')
    }

    if (!isLoggedIn && !publicPages.includes(to.path)) {
      return navigateTo('/login')
    }
    if (isLoggedIn && to.path === '/login') {
      return navigateTo('/main')
    }
  }
})
