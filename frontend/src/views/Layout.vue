<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo" @click="$router.push('/')">
        <el-icon :size="24" color="#409eff"><Reading /></el-icon>
        <span v-show="!isCollapse">智慧图书馆</span>
      </div>

      <el-menu
        :default-active="$route.path"
        :collapse="isCollapse"
        :router="true"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <el-menu-item index="/opac">
          <el-icon><Search /></el-icon>
          <template #title>资源检索</template>
        </el-menu-item>

        <el-menu-item index="/seats" v-if="!userStore.isAdmin">
          <el-icon><Grid /></el-icon>
          <template #title>座位预约</template>
        </el-menu-item>

        <el-menu-item index="/my-borrows" v-if="!userStore.isAdmin">
          <el-icon><Notebook /></el-icon>
          <template #title>我的借阅</template>
        </el-menu-item>

        <el-menu-item index="/my-reservations" v-if="!userStore.isAdmin">
          <el-icon><List /></el-icon>
          <template #title>我的预约</template>
        </el-menu-item>

        <el-menu-item index="/my-recommends">
          <el-icon><Goods /></el-icon>
          <template #title>图书荐购</template>
        </el-menu-item>

        <el-menu-item index="/notifications">
          <el-icon>
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
              <Bell />
            </el-badge>
          </el-icon>
          <template #title>消息中心</template>
        </el-menu-item>

        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <template #title>个人中心</template>
        </el-menu-item>

        <!-- 管理员菜单 -->
        <template v-if="userStore.isAdmin">
          <el-menu-item-group>
            <template #title><span style="color:#909399;font-size:12px">管理后台</span></template>
          </el-menu-item-group>
          <el-menu-item index="/admin">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>数据大屏</template>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><UserFilled /></el-icon>
            <template #title>读者管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/books">
            <el-icon><Reading /></el-icon>
            <template #title>图书管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/borrows">
            <el-icon><Tickets /></el-icon>
            <template #title>借阅管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/reservations">
            <el-icon><Calendar /></el-icon>
            <template #title>预约管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/seats">
            <el-icon><OfficeBuilding /></el-icon>
            <template #title>座位管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/recommends">
            <el-icon><ShoppingCart /></el-icon>
            <template #title>荐购管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/reports">
            <el-icon><TrendCharts /></el-icon>
            <template #title>统计报表</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" :size="20">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
              <el-tag v-if="userStore.isAdmin" size="small" type="danger" style="margin-left:4px">管理员</el-tag>
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
        <router-view />
      </el-main>
    </el-container>
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
const isCollapse = ref(false)
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
}

.layout-aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.layout-header {
  background: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
}

.collapse-btn:hover {
  color: #409eff;
}

.header-right .user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #606266;
}

.layout-main {
  background-color: #f0f2f5;
  padding: 20px;
}

.el-menu {
  border-right: none;
}
</style>
