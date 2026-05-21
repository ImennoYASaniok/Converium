<script>
import { courseApi } from '../api/course_api'

export default {
  name: 'StepView',
  props: {
    courseId: {
      type: [String, Number],
      required: true
    },
    stepId: {
      type: [String, Number],
      required: true
    }
  },
  data() {
    return {
      step: null,
      loading: true,
      error: '',
      course: null,
      editMode: false,
      editForm: {
        name: '',
        description: '',
        content: ''
      },
      saveLoading: false
    }
  },
  computed: {
    stepTypeLabel() {
      const labels = { 'THEORY': 'Теория', 'TEST': 'Тест', 'PRACTICE': 'Практика' }
      return labels[this.step?.type] || this.step?.type
    }
  },
  methods: {
    async loadData() {
      this.loading = true
      this.error = ''
      try {
        const [courseRes, stepRes] = await Promise.all([
          courseApi.getInfo(this.courseId),
          courseApi.getStep(this.courseId, this.stepId)
        ])
        this.course = courseRes.data
        this.step = stepRes.data
        this.editForm = {
          name: this.step.name,
          description: this.step.description || '',
          content: this.step.content || ''
        }
      } catch (e) {
        this.error = 'Не удалось загрузить данные шага'
      } finally {
        this.loading = false
      }
    },
    toggleEdit() {
      if (this.editMode) {
        // Cancel - restore
        this.editForm = {
          name: this.step.name,
          description: this.step.description || '',
          content: this.step.content || ''
        }
      }
      this.editMode = !this.editMode
    },
    async saveStep() {
      if (!this.editForm.name.trim()) {
        alert('Название не может быть пустым')
        return
      }
      this.saveLoading = true
      try {
        const res = await courseApi.updateStep(this.courseId, this.stepId, {
          name: this.editForm.name,
          description: this.editForm.description || null,
          content: this.editForm.content || null
        })
        this.step = res.data
        this.editMode = false
      } catch (e) {
        alert('Ошибка сохранения: ' + (e?.response?.data?.message || e.message))
      } finally {
        this.saveLoading = false
      }
    },
    goBack() {
      this.$router.push(`/course/${this.courseId}`)
    }
  },
  async created() {
    await this.loadData()
  }
}
</script>

<template>
  <section class="page">
    <div class="step-layout">
      <aside class="step-sidebar">
        <button class="path-button small" type="button" @click="goBack" style="margin-bottom: 1rem;">← Назад к курсу</button>

        <div v-if="loading" style="color: #888;">Загрузка...</div>
        <div v-else-if="error" class="error-text">{{ error }}</div>

        <template v-else-if="step">
          <h2>{{ step.name }}</h2>
          <div class="step-meta">
            <span>Тип: {{ stepTypeLabel }}</span>
            <span>Курс: {{ course?.title }}</span>
          </div>
          <p class="step-desc">{{ step.description || 'Нет описания' }}</p>

          <button v-if="course?.canEdit && !editMode" class="path-button small" type="button" @click="toggleEdit">
            Редактировать
          </button>
          <button v-if="editMode" class="path-button small" type="button" @click="toggleEdit">
            Отмена
          </button>
        </template>
      </aside>

      <main class="step-main">
        <div v-if="loading" style="color: #888; padding: 2rem; text-align: center;">Загрузка...</div>
        <div v-else-if="error" class="error-text" style="padding: 2rem;">{{ error }}</div>

        <template v-else-if="step">
          <!-- Режим редактирования -->
          <div v-if="editMode" class="step-editor-form">
            <h2>Редактирование шага</h2>
            <label class="form-label">Название</label>
            <input v-model="editForm.name" class="step-input" />

            <label class="form-label">Описание</label>
            <textarea v-model="editForm.description" class="step-textarea" rows="4"></textarea>

            <label class="form-label">Содержимое (контент)</label>
            <textarea v-model="editForm.content" class="step-textarea step-content" rows="12"></textarea>

            <button class="path-button" type="button" :disabled="saveLoading" @click="saveStep">
              {{ saveLoading ? 'Сохранение...' : 'Сохранить изменения' }}
            </button>
          </div>

          <!-- Режим просмотра -->
          <div v-else class="step-view-content">
            <h2>Содержимое</h2>
            <div class="step-content-preview" v-if="step.content">
              <pre>{{ step.content }}</pre>
            </div>
            <div v-else style="color: #888; font-style: italic;">
              Контент отсутствует
            </div>
          </div>
        </template>
      </main>
    </div>
  </section>
</template>

<style scoped>
.step-layout {
  display: flex;
  gap: 1rem;
  height: calc(100vh - 120px);
}
.step-sidebar {
  width: 300px;
  flex-shrink: 0;
  background: #1e1e2e;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 1rem;
  overflow-y: auto;
}
.step-main {
  flex: 1;
  background: #1e1e2e;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 1.5rem;
  overflow-y: auto;
}
.step-meta {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.85rem;
  color: #888;
  margin-bottom: 1rem;
}
.step-desc {
  color: #aaa;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}
.step-input, .step-textarea {
  display: block;
  width: 100%;
  margin-bottom: 1rem;
  padding: 0.5rem;
  background: #2a2a3e;
  border: 1px solid #444;
  color: #eee;
  border-radius: 4px;
  font-family: inherit;
  box-sizing: border-box;
}
.step-textarea {
  resize: vertical;
}
.step-content {
  min-height: 200px;
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
}
.form-label {
  display: block;
  color: #aaa;
  font-size: 0.85rem;
  margin-bottom: 0.25rem;
}
.step-content-preview {
  background: #2a2a3e;
  padding: 1rem;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}
.step-content-preview pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  color: #ddd;
}
.path-button.small {
  padding: 0.3rem 0.7rem;
  font-size: 0.85rem;
}
</style>