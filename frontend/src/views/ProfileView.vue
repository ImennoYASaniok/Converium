<script>
import { useAuthStore } from '../stores/auth'
import { usersApi } from '../api/users_api'
import { courseApi } from '../api/course_api'
import AppIcon from '../components/AppIcon.vue'

export default {
  name: 'ProfileView',
  data() {
    return {
      loading: false,
      error: '',
      profile: null,
      courses: [],
      coursesLoading: false,
      coursesError: '',
      showCreateCourse: false,
      createForm: {
        title: '',
        description: '',
        visibility: 'FRIENDS_ONLY'
      },
      createLoading: false,
    }
  },
  computed: {
  },
  components: {
    AppIcon,
  },
  methods: {
    logout() {
      const auth = useAuthStore()
      auth.logout()
      this.$router.push('/login')
    },
    async loadCourses() {
      this.coursesLoading = true
      this.coursesError = ''
      try {
        const res = await courseApi.getMy()
        this.courses = res.data
      } catch (e) {
        this.coursesError = 'Не удалось загрузить курсы'
      } finally {
        this.coursesLoading = false
      }
    },
    async createCourse() {
      if (!this.createForm.title.trim()) {
        alert('Введите название курса')
        return
      }
      this.createLoading = true
      try {
        const res = await courseApi.create({
          title: this.createForm.title,
          description: this.createForm.description || null,
          visibility: this.createForm.visibility
        })
        const course = res.data
        this.showCreateCourse = false
        this.createForm = { title: '', description: '', visibility: 'FRIENDS_ONLY' }
        this.$router.push(`/course/${course.id}`)
      } catch (e) {
        alert('Ошибка создания курса: ' + (e?.response?.data?.message || e.message))
      } finally {
        this.createLoading = false
      }
    },
    goToCourse(courseId) {
      this.$router.push(`/course/${courseId}`)
    },
    goToEditor(courseId) {
      this.$router.push(`/editor/${courseId}`)
    },
    async deleteCourse(courseId) {
      if (!confirm('Удалить курс?')) return
      try {
        await courseApi.delete(courseId)
        this.courses = this.courses.filter(c => c.id !== courseId)
      } catch (e) {
        alert('Ошибка удаления курса')
      }
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
      this.loadCourses()
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
        <img v-if="profile?.profilePicture" :src="profile.profilePicture" alt="Аватар" class="avatar-image" />
        <AppIcon v-else name="user_icon" class="avatar-image" :size="'100%'" />
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

    <!-- Мои курсы -->
    <div style="margin-top: 2rem;">
      <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem;">
        <h2 style="margin: 0;">Мои курсы</h2>
        <button class="path-button" type="button" @click="showCreateCourse = !showCreateCourse">
          {{ showCreateCourse ? 'Отмена' : '+ Создать курс' }}
        </button>
      </div>

      <!-- Форма создания -->
      <div v-if="showCreateCourse" class="create-course-container">
        <input v-model="createForm.title" placeholder="Название курса" class="course-input" />
        <textarea v-model="createForm.description" placeholder="Описание (необязательно)" class="course-textarea" rows="3"></textarea>
        <select v-model="createForm.visibility" class="course-select">
          <option value="FRIENDS_ONLY">Только друзья</option>
          <option value="PUBLIC">Публичный</option>
          <option value="CERTAIN_PEOPLE">Определённые люди</option>
        </select>
        <div class="create-actions">
          <button class="path-button secondary" type="button" @click="showCreateCourse = false" :disabled="createLoading">Отмена</button>
          <button class="path-button primary" type="button" :disabled="createLoading" @click="createCourse">
            {{ createLoading ? 'Создание...' : 'Создать' }}
          </button>
        </div>
      </div>

      <p v-if="coursesLoading">Загрузка курсов...</p>
      <p v-else-if="coursesError" class="error-text">{{ coursesError }}</p>

      <div v-else-if="courses.length === 0" style="color: #888;">
        У вас пока нет курсов
      </div>

      <div v-else class="courses-list">
        <div v-for="course in courses" :key="course.id" class="course-card">
          <div class="course-card-body" @click="goToCourse(course.id)">
            <strong>{{ course.title }}</strong>
            <span style="font-size: 0.85rem; color: #aaa;">{{ course.description }}</span>
            <span style="font-size: 0.8rem; color: #777;">
              Создан: {{ new Date(course.createdAt).toLocaleDateString() }}
            </span>
          </div>
          <div class="course-card-actions">
            <button class="path-button small" type="button" @click.stop="goToCourse(course.id)">Открыть</button>
            <button v-if="course.canEdit" class="path-button small" type="button" @click.stop="goToEditor(course.id)">Редактор</button>
            <button v-if="course.canEdit" class="path-button small danger" type="button" @click.stop="deleteCourse(course.id)">Удалить</button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped src="../assets/styles/profile/profile.css"></style>
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
  background: #1e1e2e;
  border: 1px solid #333;
  border-radius: 8px;
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
.course-input, .course-textarea, .course-select {
  display: block;
  width: 100%;
  margin-bottom: 0.5rem;
  padding: 0.5rem;
  background: #2a2a3e;
  border: 1px solid #444;
  color: #eee;
  border-radius: 4px;
  font-family: inherit;
  box-sizing: border-box;
}
.course-textarea {
  resize: vertical;
}
.path-button.small {
  padding: 0.3rem 0.7rem;
  font-size: 0.85rem;
}
.path-button.danger {
  background: #a33;
  border-color: #a33;
}
.path-button.danger:hover {
  background: #c44;
}

/* Create course form styles using design tokens */
.create-course-container {
  background: var(--panel);
  color: var(--text);
  border: 2px solid var(--line);
  padding: 1rem;
  border-radius: 0;
  margin-bottom: 1rem;
  max-width: 700px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.04);
}

.create-course-container .course-input,
.create-course-container .course-textarea,
.create-course-container .course-select {
  background: var(--surface);
  border: 1px solid var(--line);
  color: var(--text);
  padding: 0.6rem 0.75rem;
  border-radius: 6px;
  width: 100%;
  margin-bottom: 0.75rem;
}

.create-course-container .course-textarea { min-height: 100px; }

.create-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 0.25rem;
}

.path-button.primary {
  background: var(--accent);
  color: var(--text);
  border: 2px solid var(--line);
  padding: 0.6rem 1rem;
  font-size: 1rem;
  border-radius: 0;
}
.path-button.primary:hover { background: var(--text); color: var(--accent); }

.path-button.secondary {
  background: transparent;
  color: var(--text);
  border: 2px solid var(--line);
  padding: 0.6rem 1rem;
  font-size: 1rem;
  border-radius: 0;
}
.path-button.secondary:hover { background: rgba(0,0,0,0.04); }
</style>