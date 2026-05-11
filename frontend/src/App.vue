<script>
import { usersApi } from './api/users_api'
import AppHeader from './components/layout/AppHeader.vue'
import AppFooter from './components/layout/AppFooter.vue'
import { useAuthStore } from './stores/auth'
import { useThemeStore } from './stores/theme'
import { RouterView } from 'vue-router'

export default {
  components: {
    AppHeader,
    AppFooter,
    RouterView,
  },
  async created() {
    const auth = useAuthStore()
    const themeStore = useThemeStore()
    themeStore.initTheme()

    if (!auth.userId) {
      return
    }

    try {
      const res = await usersApi.getById(auth.userId)
      if (res?.data?.theme) {
        themeStore.applyTheme(res.data.theme)
      }
    } catch {
      themeStore.initTheme()
    }
  },
}
</script>

<template>
  <AppHeader />
  <main>
    <RouterView />
  </main>
  <AppFooter />
</template>

