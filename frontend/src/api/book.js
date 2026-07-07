import request from '../utils/request'

// 图书管理
export function addBook(data) {
  return request.post('/book/add', data)
}

export function updateBook(bookId, data) {
  return request.put(`/book/${bookId}`, data)
}

export function offlineBook(bookId) {
  return request.put(`/book/offline/${bookId}`)
}

export function reOnlineBook(bookId) {
  return request.put(`/book/reonline/${bookId}`)
}

export function getBook(bookId) {
  return request.get(`/book/${bookId}`)
}

export function listBooks(params) {
  return request.get('/book/list', { params })
}

// OPAC 检索
export function searchBooks(keyword) {
  return request.get('/book/search', { params: { keyword } })
}

export function getHotBooks(limit = 20) {
  return request.get('/book/hot', { params: { limit } })
}

export function getLowStockBooks() {
  return request.get('/book/low-stock')
}

export function getCategoryStats() {
  return request.get('/book/category-stats')
}

// 荐购管理
export function recommendBook(data) {
  return request.post('/book/recommend', data)
}

export function listRecommends(params) {
  return request.get('/book/recommend/list', { params })
}

export function reviewRecommend(id, status, comment) {
  return request.put(`/book/recommend/review/${id}`, null, { params: { status, comment } })
}
