import request from '../utils/request'

// 获取所有阅览室
export function listRooms() {
  return request.get('/seat/room/list')
}

// 获取某阅览室座位
export function listSeatsByRoom(roomId) {
  return request.get(`/seat/room/${roomId}`)
}

// 获取所有座位
export function listAllSeats() {
  return request.get('/seat/list')
}

// 获取可用座位
export function getAvailableSeats(params) {
  return request.get('/seat/available', { params })
}

// 获取座位预约时段
export function getSeatReservations(seatId, date) {
  return request.get(`/seat/reservations/${seatId}`, { params: { date } })
}

// 更新座位状态（管理员）
export function updateSeatStatus(seatId, status) {
  return request.put(`/seat/status/${seatId}?status=${status}`)
}
