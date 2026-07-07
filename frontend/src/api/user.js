import request from '../utils/request'

// 获取图形验证码
export function getCaptcha() {
  return request.get('/user/captcha')
}

// 用户登录
export function login(data) {
  return request.post('/user/login', data)
}

// 用户注册
export function register(data) {
  return request.post('/user/register', data)
}

// 获取当前用户信息
export function getUserInfo() {
  return request.get('/user/info')
}

// 修改用户信息
export function updateUser(data) {
  return request.put('/user/update', data)
}

// 获取所有用户（管理员）
export function listUsers() {
  return request.get('/user/list')
}

// 修改用户状态（管理员）
export function updateUserStatus(userId, status) {
  return request.put(`/user/status/${userId}?status=${status}`)
}
