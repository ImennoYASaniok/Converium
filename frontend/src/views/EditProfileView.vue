<script>
import { useAuthStore } from '../stores/auth'
import { usersApi } from '../api/users_api'

export default {
  name: 'EditProfileView',
  data() {
    return {
      loading: false,
      saving: false,
      error: '',
      success: '',
      profile: null,
      form: {
        name: '',
        surname: '',
        description: '',
        profilePicture: '',
      },
      maxAvatarSizeBytes: 2 * 1024 * 1024,
    }
  },
  computed: {
    hasAvatar() {
      return Boolean(this.form.profilePicture && String(this.form.profilePicture).trim().length > 0)
    },
    avatarSrc() {
      return this.form.profilePicture
    },
  },
  async created() {
    await this.loadProfile()
  },
  methods: {
    async loadProfile() {
      this.loading = true
      this.error = ''
      try {
        const auth = useAuthStore()
        if (!auth.userId) {
          this.error = 'Не удалось определить пользователя'
          return
        }
        const res = await usersApi.getById(auth.userId)
        this.profile = res.data
        this.form = {
          name: res.data.name || '',
          surname: res.data.surname || '',
          description: res.data.description || '',
          profilePicture: res.data.profilePicture || '',
        }
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось загрузить профиль'
      } finally {
        this.loading = false
      }
    },
    openAvatarPicker() {
      this.$refs.avatarInput?.click()
    },
    async onAvatarSelected(event) {
      const file = event.target.files?.[0]
      event.target.value = ''
      if (!file) return

      if (!file.type.startsWith('image/')) {
        this.error = 'Можно загрузить только изображение'
        return
      }

      if (file.size > this.maxAvatarSizeBytes) {
        this.error = 'Изображение должно быть не больше 2 МБ'
        return
      }

      this.form.profilePicture = await this.fileToDataUrl(file)
      this.error = ''
    },
    removeAvatar() {
      this.form.profilePicture = ''
    },
    async saveProfile() {
      this.saving = true
      this.error = ''
      this.success = ''
      try {
        const auth = useAuthStore()
        await usersApi.update(auth.userId, {
          name: this.form.name,
          surname: this.form.surname,
          description: this.form.description,
          profilePicture: this.form.profilePicture,
        })
        this.success = 'Профиль успешно обновлён'
        // notify header and other listeners that profile was updated
        try { window.dispatchEvent(new CustomEvent('profile-updated')) } catch {}
      } catch (e) {
        this.error = e?.response?.data?.message || 'Не удалось сохранить профиль'
      } finally {
        this.saving = false
      }
    },
    fileToDataUrl(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => resolve(reader.result)
        reader.onerror = () => reject(new Error('Не удалось прочитать файл'))
        reader.readAsDataURL(file)
      })
    },
  },
}
</script>

<template>
  <section class="page">
    <h1 class="page-title">Изменить профиль</h1>
    <div class="split-line" />

    <p v-if="loading">Загрузка...</p>
    <p v-else-if="error" class="error-text">{{ error }}</p>

    <div v-else class="profile-grid">
      <input ref="avatarInput" type="file" accept="image/*" class="avatar-file-input" @change="onAvatarSelected" />

      <div class="avatar-square">
        <img v-if="hasAvatar" :src="avatarSrc" alt="Аватар" class="avatar-image" />
        <button v-if="!hasAvatar" type="button" class="path-button avatar-center-button" @click="openAvatarPicker">
          Установить аватарку
        </button>
        <div v-if="hasAvatar" class="avatar-bottom-actions">
          <button type="button" class="path-button" @click="openAvatarPicker">Изменить</button>
          <button type="button" class="path-button" @click="removeAvatar">Удалить</button>
        </div>
      </div>

      <form class="auth-form" @submit.prevent="saveProfile">
        <div class="field-row">
          <label for="name">Имя</label>
          <input id="name" v-model="form.name" type="text" maxlength="30" />
        </div>
        <div class="field-row">
          <label for="surname">Фамилия</label>
          <input id="surname" v-model="form.surname" type="text" maxlength="30" />
        </div>
        <div class="field-row">
          <label for="description">Описание</label>
          <textarea id="description" v-model="form.description" rows="4" maxlength="5000" />
        </div>

        <p v-if="success" class="success-text">{{ success }}</p>
        <div class="action-row">
          <button type="submit" class="path-button" :disabled="saving">Сохранить изменения</button>
          <router-link class="path-button" to="/profile">Назад</router-link>
        </div>
      </form>
    </div>
  </section>
</template>
