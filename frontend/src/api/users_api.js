import api from './config'

export const usersApi = {
  register: (data) => api.post('/users/register', data),
  getById: (id) => api.get(`/users/${id}`),
  update: (id, data) => api.put(`/users/${id}`, data),
}
