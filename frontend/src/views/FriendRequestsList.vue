<script>
import { useAuthStore } from '../stores/auth'
import { usersApi } from '../api/users_api'
import AppIcon from '../components/AppIcon.vue'

export default {
  name: 'FriendRequestsList',
  components: { AppIcon },
  data() {
    return {
      loading: false,
      error: '',
      requests: []
    }
  },
  methods: {
    async load() {
      this.loading = true
      this.error = ''
      try {
        const auth = useAuthStore()
        const userId = auth.userId
        if (!userId) { this.error = 'Не авторизован'; return }
        const res = await usersApi.getIncomingRequests(userId)
        // returns array of { requester: UserDto }
        this.requests = res.data.map(r => r.requester)
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось загрузить заявки'
      } finally {
        this.loading = false
      }
    },
    async accept(requesterId) {
      const auth = useAuthStore()
      const userId = auth.userId
      if (!userId) { alert('Не авторизован'); return }
      try {
        await usersApi.acceptFriendRequest(userId, requesterId)
        this.load()
      } catch (e) {
        alert('Ошибка: ' + (e?.response?.data?.message || e.message))
      }
    },
    async reject(requesterId) {
      const auth = useAuthStore()
      const userId = auth.userId
      if (!userId) { alert('Не авторизован'); return }
      try {
        await usersApi.rejectFriendRequest(userId, requesterId)
        this.load()
      } catch (e) {
        alert('Ошибка: ' + (e?.response?.data?.message || e.message))
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
    <h1 class="page-title">Заявки в друзья</h1>
    <div class="split-line" />

    <p v-if="loading">Загрузка...</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <div v-else-if="requests.length===0" style="color:#888;">У вас нет входящих заявок</div>

    <div v-else class="requests-list">
      <div v-for="user in requests" :key="user.id" class="request-card">
        <div class="left">
          <img v-if="user.profilePicture" :src="user.profilePicture" alt="avatar" class="avatar-image" />
          <AppIcon v-else name="user_icon" class="avatar-image" />
        </div>
        <div class="body">
          <div class="name">{{ user.name || user.login }}</div>
          <div class="login">@{{ user.login }}</div>
        </div>
        <div class="actions">
          <button class="path-button" @click="accept(user.id)">Принять</button>
          <button class="path-button" @click="reject(user.id)">Отклонить</button>
        </div>
      </div>
    </div>

    <div style="margin-top:1rem;"><router-link class="path-button" to="/profile">← Назад в профиль</router-link></div>
  </section>
</template>

<style scoped>
.request-card { display:flex; gap:0.75rem; align-items:center; padding:0.75rem; border:1px solid var(--line); background:var(--panel); }
.left .avatar-image { width:48px; height:48px; border-radius: var(--global-radius); object-fit:cover }
.body { flex:1 }
.actions { display:flex; gap:0.5rem }
</style>
