<script>
import { useAuthStore } from '../stores/auth'
import { usersApi } from '../api/users_api'
import AppIcon from '../components/AppIcon.vue'

export default {
  name: 'SearchUsers',
  components: { AppIcon },
  data() {
    return {
      q: '',
      by: '',
      loading: false,
      results: [],
      error: ''
    }
  },
  methods: {
    async search() {
      // Allow empty query: backend will return first 10 users when query is empty
      this.loading = true
      this.error = ''
      try {
        const res = await usersApi.search(this.q.trim(), this.by || undefined)
        const auth = useAuthStore()
        const meId = auth.userId
        this.results = res.data.filter(u => (meId ? u.id !== meId : true))
      } catch (e) {
        this.error = e?.response?.data?.message || 'Ошибка поиска'
      } finally {
        this.loading = false
      }
    },
    viewUser(id) {
      this.$router.push(`/users/${id}`)
    },
    async sendRequest(targetId) {
      const auth = useAuthStore()
      const userId = auth.userId
      if (!userId) { alert('Не авторизован'); return }
      try {
        await usersApi.sendFriendRequest(userId, targetId)
        alert('Заявка отправлена')
      } catch (e) {
        alert('Ошибка отправки заявки: ' + (e?.response?.data?.message || e.message))
      }
    }
    ,
    async cancelRequest(targetId) {
      const auth = useAuthStore()
      const userId = auth.userId
      if (!userId) { alert('Не авторизован'); return }
      try {
        await usersApi.cancelFriendRequest(userId, targetId)
        const idx = this.results.findIndex(u => u.id === targetId)
        if (idx >= 0) this.results[idx].hasOutgoingRequest = false
        alert('Заявка отменена')
      } catch (e) {
        alert('Ошибка отмены заявки: ' + (e?.response?.data?.message || e.message))
      }
    }
  }
  ,
  created() {
    this.search()
  }
}
</script>

<template>
  <section class="page">
    <h1 class="page-title">Поиск пользователей</h1>
    <div class="split-line" />

    <div style="display:flex; gap:0.5rem; margin-bottom:1rem; align-items:center;">
      <select v-model="by" class="course-select" style="width:160px">
        <option value="">По всему</option>
        <option value="login">По логину</option>
        <option value="name">По имени/фамилии</option>
        <option value="email">По email</option>
      </select>
      <input v-model="q" @keyup.enter="search" placeholder="Поиск" class="course-input" />
      <button class="path-button" @click="search">Поиск</button>
    </div>

    <p v-if="loading">Поиск...</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <div v-else-if="results.length===0" style="color:#888;">Нет результатов</div>

    <div v-else class="results-list">
      <div v-for="user in results" :key="user.id" class="result-card">
        <div class="result-left">
          <img v-if="user.profilePicture" :src="user.profilePicture" alt="avatar" class="avatar-image" />
          <AppIcon v-else name="user_icon" class="avatar-image" />
        </div>
        <div class="result-body">
          <div class="result-name"><router-link :to="`/users/${user.id}`">{{ user.name || user.login }}</router-link></div>
          <div class="result-login">@<router-link :to="`/users/${user.id}`">{{ user.login }}</router-link></div>
        </div>
        <div class="result-actions">
          <button v-if="!user.isFriend && !user.hasOutgoingRequest && !user.hasIncomingRequest" class="path-button" @click="sendRequest(user.id)">Добавить в друзья</button>
          <button v-else-if="user.hasOutgoingRequest" class="path-button" @click="cancelRequest(user.id)">Отменить заявку</button>
          <button v-else-if="user.hasIncomingRequest" class="path-button" @click="viewUser(user.id)">Просмотреть</button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.result-card { display:flex; gap:0.75rem; align-items:center; padding:0.75rem; border:var(--global-line-width) solid var(--line); background:var(--panel); }
.result-left .avatar-image { width:48px; height:48px; border-radius:var(--global-radius); object-fit:cover }
.result-body { flex:1 }
.result-actions { display:flex; gap:0.5rem }
</style>
