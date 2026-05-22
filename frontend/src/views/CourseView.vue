<script>
import { courseApi } from '../api/course_api'
import { useAuthStore } from '../stores/auth'
import CourseEditor from '../components/course/CourseEditor.vue'

export default {
  name: 'CourseView',
  components: { CourseEditor },
  props: {
    courseId: {
      type: [String, Number],
      required: true
    }
  },
  data() {
    return {
      course: null,
      loading: true,
      error: '',
      showEditor: false,
      editForm: {
        title: '',
        description: '',
        visibility: ''
      },
      editLoading: false,
      sidebarOpen: true
    }
  },
  computed: {
    userId() {
      return useAuthStore().userId
    },
    isOwner() {
      return this.course && this.userId && this.course.owner?.id === this.userId
    }
  },
  methods: {
    async loadCourse() {
      this.loading = true
      this.error = ''
      try {
        const res = await courseApi.getInfo(this.courseId)
        this.course = res.data
        this.editForm = {
          title: this.course.title,
          description: this.course.description || '',
          visibility: this.course.visibility
        }
      } catch (e) {
        this.error = 'Не удалось загрузить курс'
      } finally {
        this.loading = false
      }
    },
    goToEditor() {
      this.$router.push(`/editor/${this.courseId}`)
    },
    goToStep(stepId) {
      this.$router.push(`/course/${this.courseId}/step/${stepId}`)
    },
    startEdit() {
      this.showEditor = true
    },
    cancelEdit() {
      this.showEditor = false
      this.editForm = {
        title: this.course.title,
        description: this.course.description || '',
        visibility: this.course.visibility
      }
    },
    async saveCourse() {
      if (!this.editForm.title.trim()) {
        alert('Название не может быть пустым')
        return
      }
      this.editLoading = true
      try {
        const res = await courseApi.update(this.courseId, {
          title: this.editForm.title,
          description: this.editForm.description || null,
          visibility: this.editForm.visibility
        })
        this.course = res.data
        this.showEditor = false
      } catch (e) {
        alert('Ошибка сохранения: ' + (e?.response?.data?.message || e.message))
      } finally {
        this.editLoading = false
      }
    },
    goBack() {
      this.$router.push('/profile')
    }
    ,
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen
    }
  },
  async created() {
    await this.loadCourse()
  }
}
</script>

<template>
  <div class="course-layout" :class="{ 'sidebar-closed': !sidebarOpen }">
    <button class="sidebar-toggle" @click="toggleSidebar" :aria-expanded="sidebarOpen">
      <span v-if="sidebarOpen">⟨</span>
      <span v-else>⟩</span>
    </button>
    <!-- Левая боковая панель -->
    <aside class="course-sidebar" :aria-hidden="!sidebarOpen" :class="{ closed: !sidebarOpen }">
      <button class="path-button small" type="button" @click="goBack" style="margin-bottom: 1rem;">← Назад</button>

      <div v-if="loading" style="color: #888;">Загрузка...</div>
      <div v-else-if="error" class="error-text">{{ error }}</div>

      <template v-else-if="course">
        <!-- Режим редактирования -->
        <div v-if="showEditor && course.canEdit">
          <h2>Редактировать курс</h2>
          <input v-model="editForm.title" placeholder="Название" class="sidebar-input" />
          <textarea v-model="editForm.description" placeholder="Описание" class="sidebar-textarea" rows="4"></textarea>
          <select v-model="editForm.visibility" class="sidebar-select">
            <option value="FRIENDS_ONLY">Только друзья</option>
            <option value="PUBLIC">Публичный</option>
            <option value="CERTAIN_PEOPLE">Определённые люди</option>
          </select>
          <div style="display: flex; gap: 0.5rem; margin-top: 0.5rem;">
            <button class="path-button small" type="button" :disabled="editLoading" @click="saveCourse">
              {{ editLoading ? 'Сохранение...' : 'Сохранить' }}
            </button>
            <button class="path-button small" type="button" @click="cancelEdit">Отмена</button>
          </div>
        </div>

        <!-- Режим просмотра -->
        <div v-else>
          <h1 class="sidebar-title">{{ course.title }}</h1>
          <p class="sidebar-desc">{{ course.description || 'Нет описания' }}</p>
          <div class="sidebar-meta">
            <span>Владелец: {{ course.owner?.login }}</span>
            <span>Видимость: {{ course.visibility }}</span>
            <span>Участников: {{ course.memberCount }}</span>
            <span>Создан: {{ new Date(course.createdAt).toLocaleDateString() }}</span>
          </div>
          <div style="display: flex; flex-direction: column; gap: 0.5rem; margin-top: 1rem;">
            <button v-if="course.canEdit" class="path-button small" type="button" @click="startEdit">Редактировать курс</button>
            <button class="path-button small" type="button" @click="goToEditor">Редактор дерева</button>
          </div>
        </div>
      </template>
    </aside>

    <!-- Правая часть — дерево -->
    <main class="course-main">
      <CourseEditor :course-id="Number(courseId)" :course-name="course?.title || 'Курс'" />
    </main>
  </div>
</template>

<style scoped>
.course-layout {
  position: fixed;
  left: 0;
  right: 0;
  top: var(--site-header-height, 64px);
  bottom: var(--site-footer-height, 72px);
  display: flex;
  gap: 1rem;
}
.course-sidebar {
  width: 300px;
  flex-shrink: 0;
  background: #1e1e2e;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 1rem;
  overflow-y: auto;
  transition: transform 220ms ease, opacity 220ms ease;
}
.course-main {
  flex: 1;
  overflow: hidden;
  border: 1px solid #333;
  border-radius: 8px;
  background: #1a1a2a;
  transition: margin-left 220ms ease;
}
.course-layout.sidebar-closed .course-sidebar {
  transform: translateX(-8px);
  opacity: 0;
  width: 0 !important;
  padding: 0 !important;
  border: 0 !important;
  overflow: hidden !important;
}
.sidebar-toggle {
  position: absolute;
  left: 8px;
  top: 12px;
  z-index: 60;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--panel);
  color: var(--text);
  border: 1px solid var(--line);
  cursor: pointer;
  border-radius: 4px;
}
.course-layout.sidebar-closed .sidebar-toggle {
  left: 8px;
}
.sidebar-title {
  font-size: 1.3rem;
  margin: 0 0 0.5rem;
}
.sidebar-desc {
  color: #aaa;
  font-size: 0.9rem;
  margin: 0 0 1rem;
}
.sidebar-meta {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.85rem;
  color: #888;
}
.sidebar-input, .sidebar-textarea, .sidebar-select {
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
.sidebar-textarea {
  resize: vertical;
}
.path-button.small {
  padding: 0.3rem 0.7rem;
  font-size: 0.85rem;
}
</style>