import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { defineNuxtPlugin } from 'nuxt/app'

export default defineNuxtPlugin((nuxtApp: any) => {
  nuxtApp.vueApp.use(ElementPlus)
})
