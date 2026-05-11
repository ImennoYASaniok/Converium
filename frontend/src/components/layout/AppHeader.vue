<script>
import { useAuthStore } from '../../stores/auth'
import { usersApi } from '../../api/users_api'
import userIcon from '../../assets/imgs/user_icon.png'

export default {
  data() {
    return {
      menuOpen: false,
      profilePicture: '',
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
  },
  async mounted() {
    document.addEventListener('click', this.onDocumentClick)
    await this.refreshProfilePicture()
  },
  beforeUnmount() {
    document.removeEventListener('click', this.onDocumentClick)
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
    <nav class="header-nav">
      <div class="header-group">
        <router-link class="header-button" to="/">Главная</router-link>
        <router-link class="header-button" to="/about">О нас</router-link>
      </div>

      <div class="header-group">
        <div v-if="isAuthenticated" class="user-menu">
          <button class="avatar-button" type="button" @click="toggleMenu">
            <img class="avatar-image" :src="avatarSrc" alt="Пользователь" />
          </button>

          <div v-if="menuOpen" class="user-dropdown">
            <router-link class="header-button" to="/profile" @click="closeMenu">Профиль</router-link>
            <router-link class="header-button" to="/settings" @click="closeMenu">Настройки</router-link>
            <button class="header-button" type="button" @click="logout">Выйти</button>
          </div>
        </div>

        <template v-else>
          <router-link class="header-button" to="/login">Войти</router-link>
          <router-link class="header-button" to="/register">Регистрация</router-link>
        </template>
      </div>
    </nav>
  </header>
</template>

<style scoped>
.user-menu {
  position: relative;
}

.avatar-button {
  width: 40px;
  height: 40px;
  padding: 0;
  border: 2px solid var(--line);
  background: var(--accent);
  cursor: pointer;
  overflow: hidden;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.user-dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 160px;
  padding: 10px;
  border: 1px solid var(--border-color, #e5e7eb);
  background: var(--surface-color, #ffffff);
  z-index: 50;
}
</style>
