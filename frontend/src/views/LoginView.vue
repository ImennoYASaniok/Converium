<script>
import { authApi } from '../api/auth_api'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'

export default {
  name: 'LoginView',
  data() {
    return {
      login: '',
      password: '',
      error: '',
      loading: false,
    }
  },
  methods: {
    async onSubmit() {
      this.error = ''
      this.loading = true
      try {
        const auth = useAuthStore()
        const res = await authApi.login({
          login: this.login,
          password: this.password,
        })
        auth.setToken(res.data.accessToken)
        const themeStore = useThemeStore()
        await themeStore.syncThemeFromProfile(auth.userId)
        this.$router.push('/')
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось войти'
      } finally {
        this.loading = false
      }
    },
  },
}
</script>

<template>
  <section class="page">
    <h1 class="page-title">Вход</h1>

    <form class="auth-form" @submit.prevent="onSubmit">
      <div class="field-row">
        <label for="login">Логин</label>
        <input id="login" v-model="login" type="text" autocomplete="username" />
      </div>

      <div class="field-row">
        <label for="password">Пароль</label>
        <input id="password" v-model="password" type="password" autocomplete="current-password" />
      </div>

      <div v-if="error" class="error-text">
        <p>{{ error }}</p>
      </div>

      <div class="action-row">
        <button class="path-button" type="submit" :disabled="loading">Войти</button>
      </div>
    </form>
  </section>
</template>
