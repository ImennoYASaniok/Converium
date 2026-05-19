<script>
import { authApi } from '../api/auth_api'
import { usersApi } from '../api/users_api'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'
import AppIcon from '../components/AppIcon.vue'

export default {
  name: 'RegisterView',
  data() {
    return {
      // Шаг 1: Основные данные
      login: '',
      email: '',
      password: '',
      confirmPassword: '',
      showPassword: false,
      showConfirmPassword: false,
      
      // Иконки глаз
      
      // Шаг 2: Персональные данные
      name: '',
      surname: '',
      
      // Общие данные
      currentStep: 1,
      error: '',
      loading: false,
    }
  },
  components: {
    AppIcon,
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
    validatePasswordMatch() {
      if (this.password !== this.confirmPassword) {
        this.error = 'Пароли не совпадают'
        return false
      }
      return true
    },
    validateStep1() {
      this.error = ''
      
      if (!this.validateLogin()) return false
      if (!this.validatePassword()) return false
      if (!this.validatePasswordMatch()) return false
      
      return true
    },
    nextStep() {
      if (this.validateStep1()) {
        this.currentStep = 2
        this.error = ''
      }
    },
    prevStep() {
      this.currentStep = 1
      this.error = ''
    },
    async onSubmit() {
      this.error = ''
      this.loading = true
      
      try {
        await usersApi.register({
          login: this.login,
          email: this.email,
          password: this.password,
          profilePicture: null,
          name: this.name || null,
          surname: this.surname || null,
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

    <!-- Шаг 1: Основные данные -->
    <form v-if="currentStep === 1" class="auth-form" @submit.prevent="nextStep">
      <div class="field-row">
        <label for="login">Логин *</label>
        <input 
          id="login" 
          v-model="login" 
          type="text" 
          autocomplete="username" 
          minlength="5" 
          required 
          style="background-color: var(--surface-color, #ffffff) !important;"
        />
      </div>

      <div class="field-row">
        <label for="email">Email *</label>
        <input 
          id="email" 
          v-model="email" 
          type="email" 
          autocomplete="email" 
          required 
          style="background-color: var(--surface-color, #ffffff) !important;"
        />
      </div>

      <div class="field-row">
        <label for="password">Пароль *</label>
        <div class="password-input-container">
          <input
            id="password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="new-password"
            minlength="5"
            required
            style="background-color: var(--surface-color, #ffffff) !important;"
          />
          <button 
            type="button" 
            class="password-toggle"
            @click="showPassword = !showPassword"
          >
            <AppIcon :name="showPassword ? 'eye/eye_open' : 'eye/eye_close'" class="eye-icon" />
          </button>
        </div>
      </div>

      <div class="field-row">
        <label for="confirmPassword">Подтвердить пароль *</label>
        <div class="password-input-container">
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            autocomplete="new-password"
            minlength="5"
            required
            style="background-color: var(--surface-color, #ffffff) !important;"
          />
          <button 
            type="button" 
            class="password-toggle"
            @click="showConfirmPassword = !showConfirmPassword"
          >
            <AppIcon :name="showConfirmPassword ? 'eye/eye_open' : 'eye/eye_close'" class="eye-icon" />
          </button>
        </div>
      </div>

      <div v-if="error" class="error-text">
        <p>{{ error }}</p>
      </div>

      <div class="action-row">
        <button class="path-button" type="submit" :disabled="loading">Далее</button>
      </div>
    </form>

    <!-- Шаг 2: Персональные данные -->
    <form v-if="currentStep === 2" class="auth-form" @submit.prevent="onSubmit">
      <p class="optional-fields-hint">Поля имени и фамилии необязательны</p>
      
      <div class="field-row">
        <label for="name">Имя</label>
        <input 
          id="name" 
          v-model="name" 
          type="text" 
          autocomplete="given-name"
          style="background-color: var(--surface-color, #ffffff) !important;"
        />
      </div>

      <div class="field-row">
        <label for="surname">Фамилия</label>
        <input 
          id="surname" 
          v-model="surname" 
          type="text" 
          autocomplete="family-name"
          style="background-color: var(--surface-color, #ffffff) !important;"
        />
      </div>

      <div v-if="error" class="error-text">
        <p>{{ error }}</p>
      </div>

      <div class="action-row">
        <button 
          type="button" 
          class="path-button secondary" 
          @click="prevStep"
          :disabled="loading"
        >
          Назад
        </button>
        <button 
          class="path-button" 
          type="submit" 
          :disabled="loading"
        >
          Зарегистрироваться
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.password-input-container {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input-container input {
  flex: 1;
  padding-right: 40px;
}

.password-toggle {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.eye-icon {
  width: 20px;
  height: 20px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.password-toggle:hover .eye-icon {
  opacity: 1;
}

.optional-fields-hint {
  color: #888;
  font-size: 14px;
  margin-bottom: 20px;
  text-align: center;
  font-style: italic;
}

.action-row {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.path-button.secondary {
  background: #666;
}

.path-button.secondary:hover {
  background: #555;
}
</style>
