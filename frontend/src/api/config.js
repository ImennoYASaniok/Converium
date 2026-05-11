import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const api = axios.create({
  baseURL: 'http://localhost:8081/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const currentPath = router.currentRoute.value.path

    if (status === 401) {
      const auth = useAuthStore()
      auth.logout()
    }

    const redirectStatuses = [401, 403, 404, 500, 502, 503]
    if (redirectStatuses.includes(status) && !currentPath.startsWith('/error/')) {
      router.push(`/error/${status}`)
    }

    return Promise.reject(error)
  }
)

export default api
