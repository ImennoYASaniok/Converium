<script>
import { authApi } from '../api/auth_api'
import { usersApi } from '../api/users_api'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'

export default {
  name: 'RegisterView',
  data() {
    return {
      login: '',
      email: '',
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
        await usersApi.register({
          login: this.login,
          email: this.email,
          password: this.password,
          profilePicture: null,
          name: null,
          surname: null,
          description: null,
          contacts: [],
        })
        const auth = useAuthStore()
        const loginRes = await authApi.login({
          login: this.login,
          password: this.password,
        })
        auth.setToken(loginRes.data.accessToken)
        const themeStore = useThemeStore()
        await themeStore.syncThemeFromProfile(auth.userId)
        this.$router.push('/profile')
      } catch (e) {
        const backendMessage =
          e?.response?.data?.message ||
          (typeof e?.response?.data === 'string' ? e.response.data : '')
        this.error = backendMessage || 'Не удалось зарегистрироваться'
      } finally {
        this.loading = false
      }
    },
  },
}
</script>

<template>
  <section class="page">
    <h1 class="page-title">Регистрация</h1>

    <form class="auth-form" @submit.prevent="onSubmit">
      <div class="field-row">
        <label for="login">Логин</label>
        <input id="login" v-model="login" type="text" autocomplete="username" minlength="5" required />
      </div>

      <div class="field-row">
        <label for="email">Email</label>
        <input id="email" v-model="email" type="email" autocomplete="email" required />
      </div>

      <div class="field-row">
        <label for="password">Пароль</label>
        <input
          id="password"
          v-model="password"
          type="password"
          autocomplete="new-password"
          minlength="5"
          required
        />
      </div>

      <div v-if="error" class="error-text">
        <p>{{ error }}</p>
      </div>

      <div class="action-row">
        <button class="path-button" type="submit" :disabled="loading">Зарегистрироваться</button>
      </div>
    </form>
  </section>
</template>
