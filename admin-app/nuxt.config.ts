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
    '@nuxtjs/i18n'
  ],

  i18n: {
    locales: [
      { code: 'en', file: 'en.json', name: 'English' },
      { code: 'ua', file: 'ua.json', name: 'Українська' },
      { code: 'ru', file: 'ru.json', name: 'Русский' },
      { code: 'es', file: 'es.json', name: 'Español' },
      { code: 'fr', file: 'fr.json', name: 'Français' },
      { code: 'de', file: 'de.json', name: 'Deutsch' }
    ],
    defaultLocale: 'en',
    langDir: 'locales',
    strategy: 'no_prefix',
    detectBrowserLanguage: {
      useCookie: true,
      cookieKey: 'i18n_locale',
      fallbackLocale: 'en'
    }
  },

  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8090/api'
    }
  },

  typescript: {
    strict: true,
    // Disabled runtime type checking due to vite-plugin-checker ENOTEMPTY bug on Linux.
    // Type checking is handled by the IDE and `nuxi typecheck` command instead.
    typeCheck: false
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
        protocol: 'ws',
        host: 'localhost',
        port: 24682,
        clientPort: 24682
      }
    }
  },

  css: ['element-plus/dist/index.css', '~/assets/css/main.css']
})

