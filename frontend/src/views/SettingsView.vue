<script>
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'

export default {
  name: 'SettingsView',
  data() {
    return {
      selectedTheme: 'light',
      loading: false,
      error: '',
      success: '',
    }
  },
  created() {
    const themeStore = useThemeStore()
    this.selectedTheme = themeStore.currentTheme
  },
  computed: {
    themeOptions() {
      return [
        { value: 'light', label: 'Светлая' },
        { value: 'dark', label: 'Тёмная' },
        { value: 'graphite', label: 'Графитовая' },
        { value: 'amber', label: 'Янтарная' },
      ]
    },
  },
  methods: {
    async saveSettings() {
      this.error = ''
      this.success = ''
      this.loading = true
      try {
        const auth = useAuthStore()
        const themeStore = useThemeStore()
        await themeStore.saveThemeToProfile(auth.userId, this.selectedTheme)
        this.success = 'Тема успешно сохранена'
      } catch (e) {
        this.error = e?.response?.data?.message || e?.message || 'Не удалось сохранить настройки'
      } finally {
        this.loading = false
      }
    },
  },
}
</script>

<template>
  <section class="page">
    <h1 class="page-title">Настройки</h1>
    <div class="split-line" />

    <form class="auth-form" @submit.prevent="saveSettings">
      <div class="field-row">
        <label for="theme">Тема интерфейса</label>
        <select id="theme" v-model="selectedTheme">
          <option v-for="option in themeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>

      <div v-if="error" class="error-text">
        {{ error }}
      </div>

      <div v-if="success" class="success-text">
        {{ success }}
      </div>

      <div class="action-row">
        <button class="path-button" type="submit" :disabled="loading">Сохранить</button>
      </div>
    </form>
  </section>
</template>
