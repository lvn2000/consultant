// https://nuxt.com/docs/api/configuration/nuxt-config
import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },
  
  modules: [
    '@nuxtjs/tailwindcss',
    '@pinia/nuxt',
    '@nuxt/icon',
    '@primevue/nuxt-module'
  ],

  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8090/api'
    }
  },

  typescript: {
    strict: true,
    typeCheck: false
  },

  app: {
    head: {
      title: 'Specialist Portal - Consultant Platform',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'Specialist portal for Consultant platform' }
      ]
    }
  },

  vite: {
    server: {
      hmr: {
        port: 24681
      }
    }
  },

  css: ['~/assets/css/main.css'],

  primevue: {
    options: {
      ripple: true,
      theme: {
        preset: 'Aura'
      }
    }
  }
})
