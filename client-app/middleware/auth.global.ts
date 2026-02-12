import { defineNuxtRouteMiddleware, navigateTo } from 'nuxt/app'

export default defineNuxtRouteMiddleware((to: any) => {
  if (process.client) {
    const publicPages = ['/login', '/register']
    const isLoggedIn = !!localStorage.getItem('client_session')
    if (!isLoggedIn && !publicPages.includes(to.path)) {
      return navigateTo('/login')
    }
    if (isLoggedIn && (to.path === '/login' || to.path === '/register')) {
      return navigateTo('/main')
    }
  }
})
