import en from './locales/en.json'
import ua from './locales/ua.json'
import ru from './locales/ru.json'
import es from './locales/es.json'
import fr from './locales/fr.json'
import de from './locales/de.json'

export default defineI18nConfig(() => ({
  legacy: false,
  locale: 'en',
  fallbackLocale: 'en',
  messages: {
    en,
    ua,
    ru,
    es,
    fr,
    de
  }
}))
