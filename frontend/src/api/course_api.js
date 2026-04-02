import api from './config'

export const courseApi = {
  // Получить структуру курса
  getStructure: (courseId) => api.get(`/courses/${courseId}/structure`),
  
  // Сохранить структуру
  saveStructure: (courseId, data) => api.post(`/courses/${courseId}/structure`, data),
  
  // Получить инфу о курсе
  getInfo: (courseId) => api.get(`/courses/${courseId}`),
  
  // Создать курс
  create: (data) => api.post('/courses', data),
  
  // Удалить курс
  delete: (courseId) => api.delete(`/courses/${courseId}`)
}