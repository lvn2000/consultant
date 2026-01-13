// https://nuxt.com/docs/api/configuration/nuxt-config
import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },
  
  modules: [
    '@nuxtjs/tailwindcss',
    '@pinia/nuxt',
    '@nuxt/icon'
  ],

  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080/api'
    }
  },

  typescript: {
    strict: true,
    typeCheck: true
  },

  app: {
    head: {
      title: 'Consultant Admin',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'Admin panel for Consultant platform' }
      ]
    }
  },

  vite: {
    server: {
      hmr: {
        port: 24678
      }
    }
  },

  css: ['~/assets/css/main.css']
})

