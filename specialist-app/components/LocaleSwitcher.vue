<template>
  <div class="locale-switcher">
    <select v-model="currentLocale" @change="onLocaleChange" class="locale-select">
      <option v-for="loc in availableLocales" :key="loc.code" :value="loc.code">
        {{ loc.name }}
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
const { locale, locales } = useI18n()
const localeCookie = useCookie('i18n_locale')

const availableLocales = computed(() =>
  (locales.value as Array<{ code: string; name: string }>).map(l => ({
    code: l.code,
    name: l.name
  }))
)

const currentLocale = ref(locale.value)

function onLocaleChange() {
  locale.value = currentLocale.value
  localeCookie.value = currentLocale.value
  if (import.meta.client) {
    localStorage.setItem('user_locale', currentLocale.value)
  }
}
</script>

<style scoped>
.locale-switcher {
  display: inline-flex;
  align-items: center;
}

.locale-select {
  padding: 0.35rem 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  background: white;
  font-size: 0.85rem;
  color: #374151;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
}

.locale-select:hover {
  border-color: #9ca3af;
}

.locale-select:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}
</style>
