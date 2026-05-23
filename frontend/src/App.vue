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
    RouterView
  },
  async created() {
    const auth = useAuthStore()
    const themeStore = useThemeStore()
    themeStore.initTheme()
  },
}
</script>

<template>
  <div style="margin-bottom: 30px;"></div>
  <AppHeader />
  <main>
    <RouterView />
  </main>
  <AppFooter v-if="$route.name !== 'CourseView'" />
</template>