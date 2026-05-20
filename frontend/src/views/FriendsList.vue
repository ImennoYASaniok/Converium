<script>
import { useAuthStore } from '../stores/auth'
import { usersApi } from '../api/users_api'
import AppIcon from '../components/AppIcon.vue'

export default {
  name: 'FriendsList',
  components: { AppIcon },
  data() {
    return {
      loading: false,
      error: '',
      friends: []
    }
  },
  methods: {
    async load() {
      this.loading = true
      this.error = ''
      try {
        const auth = useAuthStore()
        const userId = auth.userId
        if (!userId) {
          this.error = 'Не авторизован'
          return
        }
        const res = await usersApi.getFriends(userId)
        // API returns array of { user: UserDto }
        this.friends = res.data.map(f => f.user)
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось загрузить список друзей'
      } finally {
        this.loading = false
      }
    }
  },
  created() {
    this.load()
  }
}
</script>

<template>
  <section class="page">
    <h1 class="page-title">Список друзей</h1>
    <div class="split-line" />

    <p v-if="loading">Загрузка...</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <div v-else-if="friends.length === 0" style="color: #888;">У вас пока нет друзей</div>

    <div v-else class="friends-grid">
      <div v-for="friend in friends" :key="friend.id" class="friend-card">
        <div class="friend-avatar">
          <img v-if="friend.profilePicture" :src="friend.profilePicture" alt="avatar" />
          <AppIcon v-else name="user_icon" />
        </div>
        <div class="friend-info">
          <div class="friend-name">{{ friend.name || friend.login }}</div>
          <div class="friend-login">@{{ friend.login }}</div>
        </div>
      </div>
    </div>

    <div style="margin-top: 1.5rem;">
      <router-link class="path-button" to="/profile">← Назад в профиль</router-link>
    </div>
  </section>
</template>

<style scoped>
.friends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}
.friend-card {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  padding: 0.75rem;
  background: var(--panel);
  border: 1px solid var(--line);
}
.friend-avatar img, .friend-avatar AppIcon {
  width: 56px;
  height: 56px;
  border-radius: var(--global-radius);
  object-fit: cover;
}
.friend-avatar {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.friend-info .friend-name {
  font-weight: 600;
}
.friend-info .friend-login {
  color: var(--muted);
  font-size: 0.9rem;
}
</style>
