// https://nuxt.com/docs/api/configuration/nuxt-config
import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  // Disable Nuxt DevTools to avoid default port 24678 conflicts during multi-app dev
  devtools: { enabled: false },
  
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
        protocol: 'ws',
        host: 'localhost',
        port: 24683,
        clientPort: 24683
      }
    }
  },

  nitro: {
    devServer: {
      watch: ['./src']
    }
  },

  css: ['~/assets/css/main.css'],

  // @ts-ignore - PrimeVue module configuration
  primevue: {
    options: {
      ripple: true,
      theme: {
        preset: 'Aura'
      }
    }
  }
})
