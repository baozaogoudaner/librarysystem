import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'opac',
        name: 'OPAC',
        component: () => import('../views/OPAC.vue'),
        meta: { title: '资源检索', requiresAuth: false }
      },
      {
        path: 'seats',
        name: 'Seats',
        component: () => import('../views/Seats.vue'),
        meta: { title: '座位预约' }
      },
      {
        path: 'my-reservations',
        name: 'MyReservations',
        component: () => import('../views/MyReservations.vue'),
        meta: { title: '我的预约' }
      },
      {
        path: 'my-borrows',
        name: 'MyBorrows',
        component: () => import('../views/MyBorrows.vue'),
        meta: { title: '我的借阅' }
      },
      {
        path: 'my-recommends',
        name: 'MyRecommends',
        component: () => import('../views/MyRecommends.vue'),
        meta: { title: '图书荐购' }
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('../views/Notifications.vue'),
        meta: { title: '消息中心' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人中心' }
      },
      // Admin Routes
      {
        path: 'admin',
        name: 'AdminDashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { title: '数据大屏', requiresAdmin: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('../views/admin/Users.vue'),
        meta: { title: '读者管理', requiresAdmin: true }
      },
      {
        path: 'admin/reservations',
        name: 'AdminReservations',
        component: () => import('../views/admin/Reservations.vue'),
        meta: { title: '预约管理', requiresAdmin: true }
      },
      {
        path: 'admin/seats',
        name: 'AdminSeats',
        component: () => import('../views/admin/SeatManage.vue'),
        meta: { title: '座位管理', requiresAdmin: true }
      },
      {
        path: 'admin/books',
        name: 'AdminBooks',
        component: () => import('../views/admin/Books.vue'),
        meta: { title: '图书管理', requiresAdmin: true }
      },
      {
        path: 'admin/borrows',
        name: 'AdminBorrows',
        component: () => import('../views/admin/Borrows.vue'),
        meta: { title: '借阅管理', requiresAdmin: true }
      },
      {
        path: 'admin/recommends',
        name: 'AdminRecommends',
        component: () => import('../views/admin/Recommends.vue'),
        meta: { title: '荐购管理', requiresAdmin: true }
      },
      {
        path: 'admin/reports',
        name: 'AdminReports',
        component: () => import('../views/admin/Reports.vue'),
        meta: { title: '统计报表', requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || '图书馆管理平台'} - 智慧图书馆`

  const token = localStorage.getItem('token')

  // OPAC 搜索允许未登录访问
  if (to.name === 'OPAC') {
    next()
    return
  }

  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
    return
  }

  if (to.meta.requiresAdmin) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.role !== 1) {
      next('/')
      return
    }
  }

  next()
})

export default router
