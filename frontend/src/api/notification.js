import request from '../utils/request'

export function getNotifications(params) {
  return request.get('/notification/list', { params })
}

export function getUnreadCount() {
  return request.get('/notification/unread-count')
}

export function markAsRead(notificationId) {
  return request.put(`/notification/read/${notificationId}`)
}

export function markAllAsRead() {
  return request.put('/notification/read-all')
}
