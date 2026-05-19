import api from './config'

export const courseApi = {
  // Мои курсы
  getMy: () => api.get('/courses/my'),
  
  // Получить структуру курса
  getStructure: (courseId) => api.get(`/courses/${courseId}/structure`),
  
  // Сохранить структуру
  saveStructure: (courseId, data) => api.post(`/courses/${courseId}/structure`, data),
  
  // Получить инфу о курсе
  getInfo: (courseId) => api.get(`/courses/${courseId}`),
  
  // Создать курс
  create: (data) => api.post('/courses', data),
  
  // Обновить курс
  update: (courseId, data) => api.put(`/courses/${courseId}`, data),
  
  // Удалить курс
  delete: (courseId) => api.delete(`/courses/${courseId}`),
  
  // Получить шаг
  getStep: (courseId, stepId) => api.get(`/courses/${courseId}/steps/${stepId}`),
  
  // Обновить шаг
  updateStep: (courseId, stepId, data) => api.put(`/courses/${courseId}/steps/${stepId}`, data),
  
  // Удалить шаг
  deleteStep: (courseId, stepId) => api.delete(`/courses/${courseId}/steps/${stepId}`)
}
