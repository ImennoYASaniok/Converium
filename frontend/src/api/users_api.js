import api from './config'

export const usersApi = {
  register: (data) => api.post('/users/register', data),
  getById: (id) => api.get(`/users/${id}`),
  update: (id, data) => api.put(`/users/${id}`, data),
  getFriends: (userId) => api.get(`/users/${userId}/friends`),
  search: (q, by) => api.get('/users/search', { params: { q, by } }),
  sendFriendRequest: (fromUserId, targetId) => api.post(`/users/${fromUserId}/friend-requests/${targetId}`),
  acceptFriendRequest: (userId, requesterId) => api.post(`/users/${userId}/friend-requests/${requesterId}/accept`),
  rejectFriendRequest: (userId, requesterId) => api.delete(`/users/${userId}/friend-requests/${requesterId}/reject`),
  cancelFriendRequest: (userId, targetId) => api.delete(`/users/${userId}/friend-requests/${targetId}/cancel`),
  getIncomingRequests: (userId) => api.get(`/users/${userId}/friend-requests/incoming`),
  removeFriend: (userId, friendId) => api.delete(`/users/${userId}/friends/${friendId}`),
}