<script>
const ERROR_MAP = {
  400: {
    title: 'Ошибка 400',
    description: 'Некорректный запрос. Проверьте введённые данные и повторите попытку.',
  },
  401: {
    title: 'Ошибка 401',
    description: 'Требуется авторизация. Выполните вход и повторите действие.',
  },
  403: {
    title: 'Ошибка 403',
    description: 'Доступ запрещён. У вас нет прав для этого действия.',
  },
  404: {
    title: 'Ошибка 404',
    description: 'Страница или ресурс не найден.',
  },
  500: {
    title: 'Ошибка 500',
    description: 'Внутренняя ошибка сервера. Попробуйте позже.',
  },
  502: {
    title: 'Ошибка 502',
    description: 'Шлюз недоступен. Сервер временно не отвечает.',
  },
  503: {
    title: 'Ошибка 503',
    description: 'Сервис временно недоступен. Попробуйте позже.',
  },
}

export default {
  name: 'ErrorStatusView',
  computed: {
    statusCode() {
      const parsed = Number(this.$route.params.code)
      return Number.isFinite(parsed) ? parsed : 500
    },
    content() {
      return (
        ERROR_MAP[this.statusCode] || {
          title: `Ошибка ${this.statusCode}`,
          description: 'Произошла непредвиденная ошибка.',
        }
      )
    },
  },
}
</script>

<template>
  <section class="page">
    <h1 class="page-title">{{ content.title }}</h1>
    <div class="split-line" />
    <p>{{ content.description }}</p>
    <p></p>
    <div class="action-row">
      <router-link class="path-button" to="/">На главную</router-link>
    </div>
  </section>
</template>
