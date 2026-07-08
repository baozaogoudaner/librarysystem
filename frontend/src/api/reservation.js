import request from '../utils/request'

// 创建预约
export function createReservation(data) {
  return request.post('/reservation/create', data)
}

// 取消预约
export function cancelReservation(id) {
  return request.put(`/reservation/cancel/${id}`)
}

// 签到
export function checkIn(id) {
  return request.put(`/reservation/check-in/${id}`)
}

// 签退
export function checkOut(id) {
  return request.put(`/reservation/check-out/${id}`)
}

// 查询我的预约
export function getMyReservations() {
  return request.get('/reservation/list')
}

// 查询当前有效预约
export function getCurrentReservation() {
  return request.get('/reservation/current')
}

// 管理员查询所有预约
export function listAllReservations(params) {
  return request.get('/reservation/admin/list', { params })
}

// 管理员取消预约
export function adminCancelReservation(id) {
  return request.put(`/reservation/admin/cancel/${id}`)
}
