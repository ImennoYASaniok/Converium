<script>
import { useAuthStore } from '../stores/auth'
import { usersApi } from '../api/users_api'
import userIcon from '../assets/imgs/user_icon.png'

export default {
  name: 'ProfileView',
  data() {
    return {
      loading: false,
      error: '',
      profile: null,
    }
  },
  computed: {
    avatarSrc() {
      return this.profile?.profilePicture || userIcon
    },
  },
  methods: {
    logout() {
      const auth = useAuthStore()
      auth.logout()
      this.$router.push('/login')
    },
  },
  async created() {
    this.loading = true
    try {
      const auth = useAuthStore()
      if (!auth.userId) {
        this.error = 'Не удалось определить пользователя'
        return
      }
      const res = await usersApi.getById(auth.userId)
      this.profile = res.data
    } catch (e) {
      this.error = e?.response?.data?.message || 'Не удалось загрузить профиль'
    } finally {
      this.loading = false
    }
  },
}
</script>

<template>
  <section class="page">
    <h1 class="page-title">Профиль</h1>
    <div class="split-line" />

    <p v-if="loading">Загрузка...</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <div v-else class="profile-grid">
      <div class="avatar-square profile-item">
        <img :src="avatarSrc" alt="Аватар" class="avatar-image" />
      </div>

      <p class="profile-item"><strong>Логин:</strong> {{ profile.login }}</p>
      <p class="profile-item"><strong>Email:</strong> {{ profile.email }}</p>
      <p class="profile-item"><strong>Имя:</strong> {{ profile.name }}</p>
      <p class="profile-item"><strong>Фамилия:</strong> {{ profile.surname }}</p>
      <p class="profile-item"><strong>Описание:</strong> {{ profile.description }}</p>

      <div class="action-row">
        <router-link class="path-button" to="/profile/edit">Изменить профиль</router-link>
      </div>

      <div class="action-row">
        <button class="path-button" type="button" @click="logout">Выйти</button>
      </div>
    </div>
  </section>
</template>

<style scoped src="../assets/styles/profile/profile.css"></style>
