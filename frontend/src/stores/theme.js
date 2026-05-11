import { defineStore } from 'pinia'
import { usersApi } from '../api/users_api'

const AVAILABLE_THEMES = ['light', 'dark', 'graphite', 'amber']
const DEFAULT_THEME = 'light'

function normalizeTheme(theme) {
  const normalized = String(theme || '').trim().toLowerCase()
  return AVAILABLE_THEMES.includes(normalized) ? normalized : DEFAULT_THEME
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    currentTheme: normalizeTheme(localStorage.getItem('theme')),
  }),
  getters: {
    themes: () => AVAILABLE_THEMES,
  },
  actions: {
    applyTheme(theme) {
      const normalized = normalizeTheme(theme)
      this.currentTheme = normalized
      localStorage.setItem('theme', normalized)
      document.documentElement.setAttribute('data-theme', normalized)
    },
    initTheme() {
      this.applyTheme(this.currentTheme)
    },
    async syncThemeFromProfile(userId) {
      if (!userId) return
      try {
        const res = await usersApi.getById(userId)
        if (res?.data?.theme) {
          this.applyTheme(res.data.theme)
        }
      } catch {
        this.initTheme()
      }
    },
    async saveThemeToProfile(userId, theme) {
      if (!userId) {
        throw new Error('Пользователь не авторизован')
      }
      const normalized = normalizeTheme(theme)
      await usersApi.update(userId, { theme: normalized })
      this.applyTheme(normalized)
      return normalized
    },
  },
})
