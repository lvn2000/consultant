/**
 * Plugin to eagerly load i18n messages before hydration.
 * This ensures translations are available on the client before Vue hydrates,
 * preventing raw keys from flashing.
 */
import en from '../i18n/locales/en.json'
import ua from '../i18n/locales/ua.json'
import ru from '../i18n/locales/ru.json'
import es from '../i18n/locales/es.json'
import fr from '../i18n/locales/fr.json'
import de from '../i18n/locales/de.json'

export default defineNuxtPlugin({
  name: 'i18n:eager-messages',
  dependsOn: ['i18n:plugin'],
  enforce: 'pre',
  setup(nuxtApp) {
    const i18n = nuxtApp.$i18n as any
    if (i18n) {
      const messages: Record<string, Record<string, unknown>> = { en, ua, ru, es, fr, de }
      for (const [locale, msgs] of Object.entries(messages)) {
        i18n.mergeLocaleMessage(locale, msgs)
      }

      // Restore user's language preference from localStorage
      if (import.meta.client) {
        const savedLocale = localStorage.getItem('user_locale')
        if (savedLocale && messages[savedLocale]) {
          i18n.locale.value = savedLocale
          // Sync cookie so @nuxtjs/i18n detectBrowserLanguage doesn't override
          document.cookie = `i18n_locale=${savedLocale};path=/;max-age=31536000;SameSite=Lax`
        }
      }
    }
  }
})
