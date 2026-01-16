import { defineNuxtRouteMiddleware, navigateTo } from 'nuxt/app'

export default defineNuxtRouteMiddleware((to: any) => {
  if (process.client) {
    const isLoggedIn = !!localStorage.getItem('client_session')
    if (!isLoggedIn && to.path !== '/login') {
      return navigateTo('/login')
    }
    if (isLoggedIn && to.path === '/login') {
      return navigateTo('/main')
    }
  }
})
