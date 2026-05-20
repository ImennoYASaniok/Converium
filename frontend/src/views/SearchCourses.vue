<script>
import { courseApi } from '../api/course_api'
import AppIcon from '../components/AppIcon.vue'

export default {
  name: 'SearchCourses',
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
      this.loading = true
      this.error = ''
      try {
        const res = await courseApi.search(this.q.trim(), this.by || undefined)
        this.results = res.data
      } catch (e) {
        this.error = e?.response?.data?.message || 'Ошибка поиска'
      } finally {
        this.loading = false
      }
    },
    openCourse(id) {
      this.$router.push(`/course/${id}`)
    }
  },
  created() {
    this.search()
  }
}
</script>

<template>
  <section class="page">
    <h1 class="page-title">Поиск курсов</h1>
    <div class="split-line" />

    <div style="display:flex; gap:0.5rem; margin-bottom:1rem; align-items:center;">
      <select v-model="by" class="course-select" style="width:180px">
        <option value="">По всему</option>
        <option value="title">По названию</option>
        <option value="description">По описанию</option>
        <option value="owner">По владельцу</option>
      </select>
      <input v-model="q" @keyup.enter="search" placeholder="Поиск курсов" class="course-input" />
      <button class="path-button" @click="search">Поиск</button>
    </div>

    <p v-if="loading">Поиск...</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <div v-else-if="results.length===0" style="color:#888;">Нет результатов</div>

    <div v-else class="courses-list">
      <div v-for="course in results" :key="course.id" class="course-card">
        <div class="course-card-body" @click="openCourse(course.id)">
          <strong>{{ course.title }}</strong>
          <span style="font-size: 0.85rem; color: #aaa;">{{ course.description }}</span>
          <span style="font-size: 0.8rem; color: #777;">Владелец: <router-link :to="`/users/${course.owner.id}`">{{ course.owner.login }}</router-link></span>
        </div>
        <div class="course-card-actions">
          <button class="path-button small" type="button" @click.stop="openCourse(course.id)">Открыть</button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.courses-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.course-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--panel);
  border: var(--global-line-width) solid var(--line);
  border-radius: var(--global-radius);
  padding: 0.75rem 1rem;
}
.course-card-body {
  display: flex;
  flex-direction: column;
  cursor: pointer;
  flex: 1;
}
.course-card-actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}
</style>
