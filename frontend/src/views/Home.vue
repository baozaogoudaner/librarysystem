<template>
  <div class="home-page">
    <!-- 欢迎卡片 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-content">
        <div>
          <h2>{{ userStore.userInfo?.realName || userStore.userInfo?.username }}，你好 👋</h2>
          <p>{{ today }}</p>
        </div>
        <el-button type="primary" size="large" round @click="$router.push('/seats')">
          <el-icon><Grid /></el-icon>
          预约座位
        </el-button>
      </div>
    </el-card>

    <!-- 当前预约 -->
    <el-card v-if="currentReservation" class="mb-4" shadow="hover">
      <template #header><span style="font-weight:600">📌 当前预约</span></template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="座位">{{ currentReservation.seatNo }}</el-descriptions-item>
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

    <!-- KPI 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="item in kpis" :key="item.label">
        <el-card shadow="hover" class="stat-card" @click="item.link && $router.push(item.link)">
          <div class="stat-icon" :style="{ color: item.color }">{{ item.icon }}</div>
          <div class="stat-value">{{ animatedValue(item) }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ECharts 图表区域 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header><span style="font-weight:600">📈 近7日预约趋势</span></template>
          <div ref="trendChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header><span style="font-weight:600">📊 我的借阅统计</span></template>
          <div ref="pieChart" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card shadow="hover" style="margin-top:20px">
      <template #header><span style="font-weight:600">⚡ 快捷入口</span></template>
      <el-row :gutter="16">
        <el-col :span="6" v-for="item in quickLinks" :key="item.label">
          <el-button :type="item.type" class="quick-btn" @click="$router.push(item.path)">
            <el-icon><component :is="item.icon" /></el-icon>
            {{ item.label }}
          </el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { getCurrentReservation, getMyReservations, checkIn, checkOut, cancelReservation } from '../api/reservation'
import { getMyBorrows } from '../api/borrowing'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Grid, Search, Notebook, Goods } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const currentReservation = ref(null)
const trendChart = ref(null)
const pieChart = ref(null)

const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const statusText = (s) => ({ 0: '待签到', 1: '使用中', 2: '已完成', 3: '已取消', 4: '违规' }[s] || '未知')
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info', 3: '', 4: 'danger' }[s] || '')

// KPI 数据
const kpis = ref([
  { label: '当前借阅', icon: '📚', color: '#409eff', value: 0, target: 0 },
  { label: '累计借阅', icon: '📖', color: '#67c23a', value: 0, target: 0 },
  { label: '待还书', icon: '⏰', color: '#f5a623', value: 0, target: 0 },
  { label: '我的预约', icon: '🎯', color: '#f56c6c', value: 0, target: 0 },
])

const quickLinks = [
  { label: '资源检索', path: '/opac', icon: 'Search', type: 'primary' },
  { label: '座位预约', path: '/seats', icon: 'Grid', type: 'success' },
  { label: '我的借阅', path: '/my-borrows', icon: 'Notebook', type: 'warning' },
  { label: '图书荐购', path: '/my-recommends', icon: 'Goods', type: 'info' },
]

// 数字动画
const animating = ref({})
function animatedValue(item) {
  return animating.value[item.label] ?? 0
}

function animateNumber(item) {
  const duration = 800
  const start = 0
  const end = item.target
  const startTime = Date.now()
  animating.value[item.label] = start
  function step() {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    animating.value[item.label] = Math.round(start + (end - start) * eased)
    if (progress < 1) requestAnimationFrame(step)
  }
  step()
}

// 加载数据
const loadData = async () => {
  try {
    const res = await getCurrentReservation()
    currentReservation.value = res.data
  } catch (e) { /* silent */ }

  try {
    const borrowRes = await getMyBorrows({ status: 0, pageSize: 50 })
    const activeBorrows = borrowRes.data?.records || []
    kpis.value[0].target = activeBorrows.length

    const historyRes = await getMyBorrows({ status: 1, pageSize: 200 })
    const historyBorrows = historyRes.data?.records || []
    kpis.value[1].target = historyBorrows.length

    const todayRes = await getMyBorrows({ pageSize: 200 })
    const all = todayRes.data?.records || []
    kpis.value[2].target = all.filter(b => b.status === 0 && new Date(b.dueDate) < new Date()).length

    const reserveRes = await getMyReservations()
    kpis.value[3].target = reserveRes.data?.length || 0
  } catch (e) { /* silent */ }

  kpis.value.forEach(animateNumber)
}

// ECharts 图表
const initCharts = async () => {
  const echarts = await loadECharts()
  if (!echarts || !trendChart.value) return

  // 折线图
  const days = ['07/01', '07/02', '07/03', '07/04', '07/05', '07/06', '07/07']
  const trend = echarts.init(trendChart.value)
  trend.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: days, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', min: 0 },
    series: [{
      data: [3, 5, 2, 6, 8, 4, 7], type: 'line', smooth: true,
      areaStyle: { color: 'rgba(64,158,255,0.15)' },
      lineStyle: { color: '#409eff', width: 2 },
      itemStyle: { color: '#409eff' }
    }]
  })

  // 饼图
  if (pieChart.value) {
    const pie = echarts.init(pieChart.value)
    pie.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, itemWidth: 10, itemHeight: 10 },
      series: [{
        type: 'pie', radius: ['40%', '65%'], center: ['50%', '45%'],
        data: [
          { name: '借阅中', value: kpis.value[0].target, itemStyle: { color: '#409eff' } },
          { name: '已归还', value: kpis.value[1].target, itemStyle: { color: '#67c23a' } },
          { name: '逾期', value: kpis.value[2].target, itemStyle: { color: '#f5a623' } },
        ],
        label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 }
      }]
    })
  }
}

// 动态加载 echarts
async function loadECharts() {
  if (typeof window.echarts !== 'undefined') return window.echarts
  try {
    const mod = await import('echarts')
    return mod.default || mod
  } catch {
    // 降级：CDN 加载
    return new Promise((resolve) => {
      const s = document.createElement('script')
      s.src = 'https://cdn.jsdelivr.net/npm/echarts@5.5.1/dist/echarts.min.js'
      s.onload = () => resolve(window.echarts)
      s.onerror = () => resolve(null)
      document.head.appendChild(s)
    })
  }
}

const handleCheckIn = async () => {
  try { await checkIn(currentReservation.value.id); ElMessage.success('签到成功'); loadData() }
  catch (e) { /* silent */ }
}
const handleCheckOut = async () => {
  try { await checkOut(currentReservation.value.id); ElMessage.success('签退成功'); loadData() }
  catch (e) { /* silent */ }
}
const handleCancel = async () => {
  await ElMessageBox.confirm('确定取消预约？', '提示', { type: 'warning' })
  try { await cancelReservation(currentReservation.value.id); ElMessage.success('取消成功'); loadData() }
  catch (e) { /* silent */ }
}

onMounted(async () => {
  await loadData()
  await nextTick()
  initCharts()
})
</script>

<style scoped>
.home-page { max-width: 1100px; margin: 0 auto; }

.welcome-card {
  margin-bottom: 20px;
  background: rgba(255,255,255,0.92) !important;
  backdrop-filter: blur(8px);
  border: none;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-content h2 {
  color: var(--text-primary);
  font-size: 22px;
  margin-bottom: 6px;
}

.welcome-content p {
  color: var(--text-secondary);
  font-size: 13px;
}

.mb-4 { margin-bottom: 20px; }

.stat-row { margin-top: 20px; }

.stat-card {
  text-align: center;
  padding: 20px 12px;
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.stat-icon { font-size: 32px; margin-bottom: 8px; }

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.quick-btn {
  width: 100%;
  height: 48px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 10px;
}
</style>
