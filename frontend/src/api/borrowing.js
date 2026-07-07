import request from '../utils/request'

// 借阅
export function borrowBook(data) {
  return request.post('/borrow/borrow', data)
}

export function returnBook(borrowId) {
  return request.put(`/borrow/return/${borrowId}`)
}

export function renewBook(borrowId) {
  return request.put(`/borrow/renew/${borrowId}`)
}

export function getMyBorrows(params) {
  return request.get('/borrow/my', { params })
}

export function listAllBorrows(params) {
  return request.get('/borrow/list', { params })
}

export function getOverdueBorrows() {
  return request.get('/borrow/overdue')
}

// 图书预约
export function reserveBook(data) {
  return request.post('/borrow/reserve', data)
}

export function cancelReserve(reserveId) {
  return request.put(`/borrow/reserve/cancel/${reserveId}`)
}

export function getMyReserves(params) {
  return request.get('/borrow/reserve/my', { params })
}

export function listAllReserves(params) {
  return request.get('/borrow/reserve/list', { params })
}
