<template>
  <div class="home-page">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card">
      <div class="welcome-content">
        <div>
          <h2>👋 你好，{{ userStore.userInfo?.realName || userStore.userInfo?.username }}！</h2>
          <p>欢迎使用图书馆座位预约系统，今天是 {{ today }}</p>
        </div>
        <el-button type="primary" size="large" @click="$router.push('/seats')">
          <el-icon><Grid /></el-icon>
          立即预约
        </el-button>
      </div>
    </el-card>

    <!-- 当前预约 -->
    <el-card class="current-card" v-if="currentReservation">
      <template #header>
        <span>📌 当前预约</span>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="座位编号">{{ currentReservation.seatNo }}</el-descriptions-item>
        <el-descriptions-item label="阅览室">{{ currentReservation.roomName }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ currentReservation.date }}</el-descriptions-item>
        <el-descriptions-item label="时段">{{ currentReservation.startTime }} - {{ currentReservation.endTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(currentReservation.status)">{{ statusText(currentReservation.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作">
          <el-button v-if="currentReservation.status === 0" type="success" size="small" @click="handleCheckIn">签到</el-button>
          <el-button v-if="currentReservation.status === 1" type="warning" size="small" @click="handleCheckOut">签退</el-button>
          <el-button v-if="currentReservation.status === 0" type="danger" size="small" @click="handleCancel">取消</el-button>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="8">
        <el-card class="stat-card">
          <el-statistic title="违规次数" :value="userStore.userInfo?.violationCount || 0">
            <template #prefix><el-icon color="#f56c6c"><WarningFilled /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <el-statistic title="账号状态" :value="userStore.userInfo?.status === 0 ? '正常' : '冻结'">
            <template #prefix><el-icon :color="userStore.userInfo?.status === 0 ? '#67c23a' : '#f56c6c'"><CircleCheckFilled /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <el-statistic title="我的预约" :value="myReservationCount">
            <template #prefix><el-icon color="#409eff"><Tickets /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 使用须知 -->
    <el-card>
      <template #header><span>📖 预约须知</span></template>
      <el-timeline>
        <el-timeline-item type="primary" timestamp="规则1">预约后需在30分钟内签到，否则释放座位并记违规1次</el-timeline-item>
        <el-timeline-item type="warning" timestamp="规则2">违规累计3次，冻结预约权限7天</el-timeline-item>
        <el-timeline-item type="info" timestamp="规则3">同一时段只能预约1个座位</el-timeline-item>
        <el-timeline-item type="success" timestamp="规则4">每个座位每次最多预约4小时</el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../store/user'
import { getCurrentReservation, getMyReservations, checkIn, checkOut, cancelReservation } from '../api/reservation'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const currentReservation = ref(null)
const myReservationCount = ref(0)

const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const statusText = (status) => {
  const map = { 0: '待签到', 1: '使用中', 2: '已完成', 3: '已取消', 4: '违规' }
  return map[status] || '未知'
}

const statusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: '', 4: 'danger' }
  return map[status] || ''
}

const loadData = async () => {
  try {
    const res = await getCurrentReservation()
    currentReservation.value = res.data

    const res2 = await getMyReservations()
    myReservationCount.value = res2.data?.length || 0
  } catch (e) { /* ignore */ }
}

const handleCheckIn = async () => {
  try {
    await checkIn(currentReservation.value.id)
    ElMessage.success('签到成功')
    loadData()
  } catch (e) { /* ignore */ }
}

const handleCheckOut = async () => {
  try {
    await checkOut(currentReservation.value.id)
    ElMessage.success('签退成功')
    loadData()
  } catch (e) { /* ignore */ }
}

const handleCancel = async () => {
  await ElMessageBox.confirm('确定要取消预约吗？', '提示', { type: 'warning' })
  try {
    await cancelReservation(currentReservation.value.id)
    ElMessage.success('取消成功')
    loadData()
  } catch (e) { /* ignore */ }
}

onMounted(loadData)
</script>

<style scoped>
.home-page { max-width: 1000px; margin: 0 auto; }
.welcome-card { margin-bottom: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.welcome-content { display: flex; justify-content: space-between; align-items: center; }
.welcome-content h2 { color: white; margin-bottom: 8px; }
.welcome-content p { color: rgba(255,255,255,0.8); }
.current-card { margin-bottom: 20px; }
.stat-row { margin-bottom: 20px; }
.stat-card { text-align: center; }
</style>
