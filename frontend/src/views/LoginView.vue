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
      validateLogin() {
        if (this.login.length < 5) {
          this.error = 'Логин должен содержать минимум 5 символов'
          return false
        }
        
        // Проверяем, что логин начинается с буквы (включая русские)
        if (!/^[a-zA-Zа-яА-ЯёЁ]/.test(this.login)) {
          this.error = 'Логин должен начинаться с буквы'
          return false
        }
        
        // Проверяем, что содержатся только буквы (включая русские), цифры, _ и -
        const validChars = /^[a-zA-Zа-яА-ЯёЁ0-9_-]+$/.test(this.login)
        if (!validChars) {
          this.error = 'Логин может содержать только буквы, цифры, символы \'_\' и \'-\''
          return false
        }
        
        return true
      },
      validatePassword() {
        if (this.password.length < 5) {
          this.error = 'Пароль должен содержать минимум 5 символов'
          return false
        }
        
        const hasLetter = /[a-zA-Z]/.test(this.password)
        const hasDigit = /[0-9]/.test(this.password)
        
        if (!hasLetter) {
          this.error = 'Пароль должен содержать хотя бы одну букву'
          return false
        }
        
        if (!hasDigit) {
          this.error = 'Пароль должен содержать хотя бы одну цифру'
          return false
        }
        
        return true
      },
      async onSubmit() {
        this.error = ''
        this.loading = true
        
        if (!this.validateLogin() || !this.validatePassword()) {
          this.loading = false
          return
        }
        
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
