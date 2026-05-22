<script>
import { useAuthStore } from '../../stores/auth'
import { usersApi } from '../../api/users_api'
import AppIcon from '../AppIcon.vue'

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
      return this.profilePicture
    },
  },
  async mounted() {
    document.addEventListener('click', this.onDocumentClick)
    window.addEventListener('scroll', this.onScroll, { passive: true })
    this.onScroll()
    await this.refreshProfilePicture()
    window.addEventListener('profile-updated', this.onProfileUpdated)
  },
  beforeUnmount() {
    document.removeEventListener('click', this.onDocumentClick)
    window.removeEventListener('scroll', this.onScroll)
    window.removeEventListener('profile-updated', this.onProfileUpdated)
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
      try {
        const auth = useAuthStore()
        if (!auth.userId) {
          this.profilePicture = ''
          return
        }
        const res = await usersApi.getById(auth.userId)
        this.profilePicture = res.data.profilePicture || ''
      } catch (e) {
        this.profilePicture = ''
      }
    },
    onProfileUpdated() {
      // Re-fetch profile picture when profile is updated elsewhere
      this.refreshProfilePicture()
    },
  },
  components: {
    AppIcon,
  },
}
</script>

<template>
  <header class="site-header">
    <nav class="header-nav" :style="{ '--header-line-scale': headerLineScale }">
      <div class="header-group">
        <router-link class="header-button" type="logo" to="/">
          <AppIcon name="logo" class="logo-image" />
        </router-link>
        <router-link class="header-button" to="/about">О нас</router-link>
        <router-link class="header-button" to="/users/search">Поиск пользователей</router-link>
        <router-link class="header-button" to="/courses/search">Поиск курсов</router-link>
      </div>

      <div class="header-group">
        <div v-if="isAuthenticated">
          <button class="avatar-button" type="button" @click="toggleMenu">
            <img v-if="avatarSrc" class="avatar-image" :src="avatarSrc" alt="Пользователь" />
            <AppIcon v-else name="user_icon" class="avatar-image" :size="'100%'" />
          </button>
        </div>
        <div v-else>
          <router-link class="header-button" to="/login">Войти</router-link>
          <router-link class="header-button" to="/register">Регистрация</router-link>
        </div>
      </div>

      <div v-if="menuOpen" class="user-dropdown">
        <router-link class="header-button" type="button" to="/profile" @click="closeMenu">Профиль</router-link>
        <router-link class="header-button" type="button" to="/settings" @click="closeMenu">Настройки</router-link>
      </div>
    </nav>
  </header>
</template>


