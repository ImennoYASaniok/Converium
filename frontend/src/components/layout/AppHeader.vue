<script>
import { useAuthStore } from '../../stores/auth'
import { usersApi } from '../../api/users_api'
import userIcon from '../../assets/imgs/user_icon.png'
import logo from '../../assets/imgs/logo.png'

export default {
  data() {
    return {
      menuOpen: false,
      profilePicture: '',
      headerLineScale: 1,
    }
  },
  computed: {
    auth() {
      return useAuthStore()
    },
    isAuthenticated() {
      return this.auth.isAuthenticated
    },
    avatarSrc() {
      return this.profilePicture || userIcon
    },
    logoSrc() {
      return logo
    },
  },
  async mounted() {
    document.addEventListener('click', this.onDocumentClick)
    window.addEventListener('scroll', this.onScroll, { passive: true })
    this.onScroll()
    await this.refreshProfilePicture()
  },
  beforeUnmount() {
    document.removeEventListener('click', this.onDocumentClick)
    window.removeEventListener('scroll', this.onScroll)
  },
  watch: {
    isAuthenticated: {
      immediate: true,
      handler() {
        this.refreshProfilePicture()
      },
    },
  },
  methods: {
    logout() {
      this.auth.logout()
      this.menuOpen = false
      this.profilePicture = ''
      this.$router.push('/login')
    },
    toggleMenu() {
      this.menuOpen = !this.menuOpen
    },
    closeMenu() {
      this.menuOpen = false
    },
    onDocumentClick(event) {
      if (!this.menuOpen) return
      if (!this.$el) return
      if (!this.$el.contains(event.target)) {
        this.menuOpen = false
      }
    },
    onScroll() {
      const shrinkDistancePx = 120
      const y = window.scrollY || 0
      const scale = 1 - y / shrinkDistancePx
      this.headerLineScale = Math.max(0, Math.min(1, scale))
    },
    async refreshProfilePicture() {
      if (!this.isAuthenticated) {
        this.profilePicture = ''
        return
      }

      const userId = this.auth.userId
      if (!userId) {
        this.profilePicture = ''
        return
      }

      try {
        const res = await usersApi.getById(userId)
        this.profilePicture = res?.data?.profilePicture || ''
      } catch {
        this.profilePicture = ''
      }
    },
  },
}
</script>

<template>
  <header class="site-header">
    <nav class="header-nav" :style="{ '--header-line-scale': headerLineScale }">
      <div class="header-group">
        <router-link class="header-button" to="/">
          <img :src="logoSrc" alt="Главная" class="logo-image" />
        </router-link>
        <router-link class="header-button" to="/about">О нас</router-link>
      </div>

      <div class="header-group">
        <div v-if="isAuthenticated" class="user-menu">
          <button class="avatar-button" type="button" @click="toggleMenu">
            <img class="avatar-image" :src="avatarSrc" alt="Пользователь" />
          </button>

          <div v-if="menuOpen" class="user-dropdown">
            <router-link class="header-button user-dropdown-button" type="button" to="/profile" @click="closeMenu">Профиль</router-link>
            <router-link class="header-button user-dropdown-button" type="button" to="/settings" @click="closeMenu">Настройки</router-link>
          </div>
        </div>

        <div v-else class="header-group">
          <router-link class="header-button" to="/login">Войти</router-link>
          <router-link class="header-button" to="/register">Регистрация</router-link>
        </div>
      </div>
    </nav>
  </header>
</template>

