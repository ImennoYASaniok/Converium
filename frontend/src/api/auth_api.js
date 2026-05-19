import api from './config'

export const authApi = {
  login: (data) => api.post('/auth/login', data),
}
