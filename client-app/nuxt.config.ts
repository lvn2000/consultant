/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

// https://nuxt.com/docs/api/configuration/nuxt-config
import { defineNuxtConfig } from "nuxt/config";

export default defineNuxtConfig({
  compatibilityDate: "2024-11-01",
  devtools: { enabled: false },

  modules: [
    "@nuxtjs/tailwindcss",
    "@pinia/nuxt",
    "@nuxt/icon",
    "@primevue/nuxt-module",
    "@nuxtjs/i18n",
  ],

  i18n: {
    locales: [
      { code: "en", file: "en.json", name: "English" },
      { code: "ua", file: "ua.json", name: "Українська" },
      { code: "ru", file: "ru.json", name: "Русский" },
      { code: "es", file: "es.json", name: "Español" },
      { code: "fr", file: "fr.json", name: "Français" },
      { code: "de", file: "de.json", name: "Deutsch" },
    ],
    defaultLocale: "en",
    langDir: "locales",
    strategy: "no_prefix",
    detectBrowserLanguage: {
      useCookie: true,
      cookieKey: "i18n_locale",
      fallbackLocale: "en",
    },
  },

  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || "http://localhost:8090/api",
    },
  },

  vite: {
    server: {
      hmr: {
        protocol: "ws",
        host: "localhost",
        port: 24681,
        clientPort: 24681,
      },
    },
  },

  typescript: {
    strict: true,
    typeCheck: false,
  },

  app: {
    head: {
      title: "Consultant Platform",
      meta: [
        { charset: "utf-8" },
        { name: "viewport", content: "width=device-width, initial-scale=1" },
        {
          name: "description",
          content: "Client portal for Consultant platform",
        },
      ],
    },
  },

  nitro: {
    devServer: {
      watch: ["./pages", "./components", "./composables"],
    },
  },

  css: ["~/assets/css/main.css"],
});
