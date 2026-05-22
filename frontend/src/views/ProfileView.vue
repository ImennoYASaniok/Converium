<script>
import { useAuthStore } from '../stores/auth'
import { usersApi } from '../api/users_api'
import { courseApi } from '../api/course_api'
import AppIcon from '../components/AppIcon.vue'

export default {
  name: 'ProfileView',
  props: ['id'],
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
      isOwn: true,
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
        // if viewing someone else's profile, load their courses
        if (this.isOwn) {
          const res = await courseApi.getMy()
          this.courses = res.data
        } else {
          const res = await courseApi.getByUser(this.id)
          this.courses = res.data
        }
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
    async sendRequest() {
      const auth = useAuthStore()
      const me = auth.userId
      if (!me) { alert('Не авторизован'); return }
      try {
        await usersApi.sendFriendRequest(me, this.profile.id)
        alert('Заявка отправлена')
        await this.reloadProfile()
      } catch (e) {
        alert('Ошибка: ' + (e?.response?.data?.message || e.message))
      }
    },
    async cancelRequest() {
      const auth = useAuthStore()
      const me = auth.userId
      if (!me) { alert('Не авторизован'); return }
      try {
        await usersApi.cancelFriendRequest(me, this.profile.id)
        alert('Заявка отменена')
        await this.reloadProfile()
      } catch (e) {
        alert('Ошибка: ' + (e?.response?.data?.message || e.message))
      }
    },
    async accept(requesterId) {
      const auth = useAuthStore()
      const me = auth.userId
      if (!me) { alert('Не авторизован'); return }
      try {
        await usersApi.acceptFriendRequest(me, requesterId)
        alert('Добавлен в друзья')
        await this.reloadProfile()
      } catch (e) {
        alert('Ошибка: ' + (e?.response?.data?.message || e.message))
      }
    },
    async reject(requesterId) {
      const auth = useAuthStore()
      const me = auth.userId
      if (!me) { alert('Не авторизован'); return }
      try {
        await usersApi.rejectFriendRequest(me, requesterId)
        alert('Отклонено')
        await this.reloadProfile()
      } catch (e) {
        alert('Ошибка: ' + (e?.response?.data?.message || e.message))
      }
    },
    async reloadProfile() {
      try {
        const targetId = this.id ? Number(this.id) : useAuthStore().userId
        const res = await usersApi.getById(targetId)
        this.profile = res.data
      } catch (e) {
        // ignore
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
      // Determine if viewing own profile or another user's
      const targetId = this.id ? Number(this.id) : auth.userId
      this.isOwn = targetId === auth.userId
      const res = await usersApi.getById(targetId)
      this.profile = res.data
      await this.loadCourses()
    } catch (e) {
      this.error = e?.response?.data?.message || 'Не удалось загрузить профиль'
    } finally {
      this.loading = false
    }
  },
  watch: {
    id(newId, oldId) {
      if (newId !== oldId) {
        // reload component data when route param changed
        this.loading = true
        this.error = ''
        this.created()
      }
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

      <div class="row-container">
        <div class="avatar-square profile-item">
          <img v-if="profile?.profilePicture" :src="profile.profilePicture" alt="Аватар" class="avatar-image" />
          <AppIcon v-else name="user_icon" class="avatar-image" type="profile-avatar" :size="'100%'" />
        </div>
        <div class="col-container">
          <div class="row-container">
            <p class="profile-item"><strong>Логин:</strong> {{ profile.login }}</p>
            <p class="profile-item"><strong>Email:</strong> {{ profile.email }}</p>
          </div>
          <p class="profile-item"><strong>Имя:</strong> {{ profile.name }}</p>
          <p class="profile-item"><strong>Фамилия:</strong> {{ profile.surname }}</p>
          <p class="profile-item"><strong>Описание:</strong> {{ profile.description }}</p>
        </div>
      </div>

      

      <div class="action-row" v-if="isOwn">
        <router-link class="path-button" to="/profile/edit">Изменить профиль</router-link>
      </div>

      <div class="action-row" v-if="isOwn && profile?.canEdit">
        <router-link class="path-button" to="/friends">Список друзей</router-link>
        <router-link class="path-button" to="/profile/requests">Заявки в друзья</router-link>
      </div>

      <div class="action-row" v-if="isOwn">
        <button class="path-button" type="button" @click="logout">Выйти</button>
      </div>

      <div class="action-row" v-else>
        <button v-if="!profile.isFriend && !profile.hasIncomingRequest" class="path-button" @click="sendRequest">Отправить запрос в друзья</button>
        <button v-else-if="!profile.isFriend && profile.hasIncomingRequest" class="path-button" @click="cancelRequest">Отменить запрос в друзья</button>
        <p v-else-if="profile.isFriend" style="margin:0; color: #4caf50;">Вы в друзьях</p>
      </div>
    </div>

    <!-- Мои курсы -->
    <div style="margin-top: 2rem;">
      <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem;">
        <h2 style="margin: 0;">{{ isOwn ? 'Мои курсы' : 'Курсы' }}</h2>
        <button v-if="isOwn" class="path-button" type="button" @click="showCreateCourse = !showCreateCourse">
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
  border: var(--global-line-width) solid #333;
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
.course-input, .course-textarea, .course-select {
  display: block;
  width: 100%;
  margin-bottom: 0.5rem;
  padding: 0.5rem;
  background: #2a2a3e;
  border: var(--global-line-widt) solid #444;
  color: #eee;
  border-radius: var(--global-radius);
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
  border: var(--global-line-width) solid var(--line);
  padding: 1rem;
  border-radius: var(--global-radius);
  margin-bottom: 1rem;
  max-width: 700px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.04);
}

.create-course-container .course-input,
.create-course-container .course-textarea,
.create-course-container .course-select {
  background: var(--surface);
  border: var(--global-line-width) solid var(--line);
  color: var(--text);
  padding: 0.6rem 0.75rem;
  border-radius: var(--global-radius);
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
  border: var(--global-line-width) solid var(--line);
  padding: 0.6rem 1rem;
  font-size: 1rem;
  border-radius: var(--global-radius);
}
.path-button.primary:hover { background: var(--text); color: var(--accent); }

.path-button.secondary {
  background: transparent;
  color: var(--text);
  border: var(--global-line-width) solid var(--line);
  padding: 0.6rem 1rem;
  font-size: 1rem;
  border-radius: var(--global-radius);
}
.path-button.secondary:hover { background: rgba(0,0,0,0.04); }
</style>