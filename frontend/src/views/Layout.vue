<template>
  <el-container class="layout-container">
    <!-- 顶部导航栏 -->
    <el-header class="layout-header">
      <div class="header-left">
        <div class="logo" @click="$router.push('/')">
          <el-icon :size="24" color="#f5a623"><Reading /></el-icon>
          <span>智慧图书馆</span>
        </div>
      </div>

      <el-menu
        :default-active="$route.path"
        mode="horizontal"
        :router="true"
        class="top-menu"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon> 首页
        </el-menu-item>

        <el-menu-item index="/opac">
          <el-icon><Search /></el-icon> 资源检索
        </el-menu-item>

        <el-sub-menu v-if="!userStore.isAdmin" index="seat">
          <template #title><el-icon><Grid /></el-icon> 座位预约</template>
          <el-menu-item index="/seats">座位预约</el-menu-item>
          <el-menu-item index="/my-reservations">我的预约</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="!userStore.isAdmin" index="borrow">
          <template #title><el-icon><Notebook /></el-icon> 借阅服务</template>
          <el-menu-item index="/my-borrows">我的借阅</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="book">
          <template #title><el-icon><Goods /></el-icon> 图书服务</template>
          <el-menu-item index="/my-recommends">图书荐购</el-menu-item>
          <el-menu-item index="/notifications">
            消息中心
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" style="margin-left:4px" />
          </el-menu-item>
          <el-menu-item index="/profile">个人中心</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="userStore.isAdmin" index="admin">
          <template #title><el-icon><DataAnalysis /></el-icon> 管理后台</template>
          <el-menu-item index="/admin">数据大屏</el-menu-item>
          <el-menu-item index="/admin/users">读者管理</el-menu-item>
          <el-menu-item index="/admin/books">图书管理</el-menu-item>
          <el-menu-item index="/admin/borrows">借阅管理</el-menu-item>
          <el-menu-item index="/admin/reservations">预约管理</el-menu-item>
          <el-menu-item index="/admin/seats">座位管理</el-menu-item>
          <el-menu-item index="/admin/recommends">荐购管理</el-menu-item>
          <el-menu-item index="/admin/reports">统计报表</el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="30" icon="UserFilled" style="background:#409eff" />
            <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
            <el-tag v-if="userStore.isAdmin" size="small" color="#f5a623" style="color:#fff;border:none;margin-left:4px">管理员</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <!-- 主内容区域 -->
    <el-main class="layout-main">
      <router-view v-slot="{ Component }">
        <transition name="slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'
import { getUnreadCount } from '../api/notification'

const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)

onMounted(() => {
  if (userStore.isLoggedIn) {
    loadUnreadCount()
    setInterval(loadUnreadCount, 60000)
  }
})

async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data || 0
  } catch (e) { /* silent */ }
}

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-header {
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 24px;
  height: 60px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
  gap: 8px;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  margin-right: 24px;
  white-space: nowrap;
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background: var(--primary-light);
}

.username {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

/* 顶部菜单样式 */
.top-menu {
  flex: 1;
  border-bottom: none !important;
  background: transparent;
}

.top-menu :deep(.el-menu-item),
.top-menu :deep(.el-sub-menu__title) {
  border-bottom: 2px solid transparent !important;
  height: 60px;
  line-height: 60px;
  font-size: 14px;
  transition: all 0.2s;
}

.top-menu :deep(.el-menu-item.is-active) {
  border-bottom-color: var(--gold) !important;
  color: var(--gold) !important;
}

.top-menu :deep(.el-menu-item:hover),
.top-menu :deep(.el-sub-menu__title:hover) {
  background: transparent !important;
  color: var(--gold) !important;
}

/* 下拉菜单 */
.top-menu :deep(.el-menu--horizontal .el-menu--horizontal) {
  margin-top: 0;
}

.layout-main {
  background: transparent;
  padding: 24px;
  flex: 1;
}
</style>
