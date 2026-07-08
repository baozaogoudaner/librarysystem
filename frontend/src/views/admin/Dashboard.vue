<template>
  <div class="dashboard">
    <!-- KPI 指标卡片 -->
    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-value">{{ kpi.todayReservations }}</div>
          <div class="kpi-label">今日预约总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-value" style="color: #67c23a">{{ kpi.currentInUse }}</div>
          <div class="kpi-label">当前在座人数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-value" style="color: #e6a23c">{{ kpi.monthViolations }}</div>
          <div class="kpi-label">本月违规总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-value" style="color: #409eff">{{ kpi.todayDate }}</div>
          <div class="kpi-label">统计日期</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区：第一行 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>每日预约量趋势（近30天）</template>
          <div ref="trendChart" style="height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>各阅览室实时占用率</template>
          <div ref="occupancyChart" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区：第二行 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>24小时预约热度分布</template>
          <div ref="heatmapChart" style="height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>违规类型分布</template>
          <div ref="pieChart" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import request from '../../utils/request'

const trendChart = ref(null)
const occupancyChart = ref(null)
const heatmapChart = ref(null)
const pieChart = ref(null)

const kpi = ref({ todayReservations: 0, currentInUse: 0, monthViolations: 0, todayDate: '' })

// 加载 KPI 数据
const loadKpi = async () => {
  try {
    const res = await request.get('/statistics/kpi')
    kpi.value = res.data
  } catch (e) { /* ignore */ }
}

// 初始化图表
const initCharts = async () => {
  // 从 node_modules 动态加载 echarts
  const echartsMod = await import('echarts')
  const echarts = echartsMod.default || echartsMod
  if (!echarts || !echarts.init) return
  const chartInstances = []

  // 1. 每日预约趋势折线图
  try {
    const trendRes = await request.get('/statistics/daily-trend?days=30')
    const trendData = trendRes.data || []
    const trendChartInst = echarts.init(trendChart.value)
    chartInstances.push(trendChartInst)
    trendChartInst.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trendData.map(d => d.date), axisLabel: { rotate: 45, fontSize: 10 } },
      yAxis: { type: 'value', name: '预约数' },
      series: [{ data: trendData.map(d => d.count), type: 'line', smooth: true, areaStyle: { opacity: 0.3 },
                 itemStyle: { color: '#409eff' } }],
      grid: { left: 50, right: 20, top: 20, bottom: 60 }
    })
  } catch (e) { /* 趋势图加载失败 */ }

  // 2. 阅览室占用率柱状图
  try {
    const occRes = await request.get('/statistics/room-occupancy')
    const occData = occRes.data || []
    const occChartInst = echarts.init(occupancyChart.value)
    chartInstances.push(occChartInst)
    occChartInst.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: occData.map(d => d.roomName) },
      yAxis: { type: 'value', name: '占用率(%)', max: 100 },
      series: [{
        data: occData.map(d => d.capacity > 0 ? Math.round(d.inUseCount / d.capacity * 100) : 0),
        type: 'bar',
        itemStyle: { color: (p) => p.value >= 80 ? '#f56c6c' : p.value >= 50 ? '#e6a23c' : '#67c23a' },
        label: { show: true, position: 'top', formatter: '{c}%' }
      }],
      grid: { left: 50, right: 20, top: 20, bottom: 40 }
    })
  } catch (e) { /* 占用率图加载失败 */ }

  // 3. 预约热度热力图
  try {
    const heatRes = await request.get('/statistics/hourly-heatmap')
    const heatData = heatRes.data || []
    const heatChartInst = echarts.init(heatmapChart.value)
    chartInstances.push(heatChartInst)
    const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    const hours = Array.from({ length: 15 }, (_, i) => i + 8)
    const heatSeries = heatData.map(d => [d.hour - 8, d.dayOfWeek - 1, d.count])
    heatChartInst.setOption({
      tooltip: { formatter: (p) => `${weekDays[p.value[1]]} ${p.value[0] + 8}:00 预约${p.value[2]}次` },
      xAxis: { type: 'category', data: hours.map(h => h + ':00'), axisLabel: { fontSize: 10 } },
      yAxis: { type: 'category', data: weekDays },
      visualMap: { min: 0, max: Math.max(...heatData.map(d => d.count), 1),
                   inRange: { color: ['#f0f9ff', '#409eff', '#1a3c6e'] }, calculable: true, orient: 'vertical', right: 10 },
      series: [{ type: 'heatmap', data: heatSeries, label: { show: false } }],
      grid: { left: 60, right: 80, top: 10, bottom: 30 }
    })
  } catch (e) { /* 热力图加载失败 */ }

  // 4. 违规类型饼图
  try {
    const pieRes = await request.get('/statistics/violation-type')
    const pieData = pieRes.data || []
    const pieChartInst = echarts.init(pieChart.value)
    chartInstances.push(pieChartInst)
    pieChartInst.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', right: 10, top: 'center' },
      series: [{
        type: 'pie', radius: ['40%', '70%'], center: ['40%', '50%'],
        data: pieData.map(d => ({ name: d.violationType, value: d.count })),
        label: { formatter: '{b}\n{d}%' },
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 }
      }]
    })
  } catch (e) { /* 饼图加载失败 */ }

  // 响应式
  window.addEventListener('resize', () => chartInstances.forEach(c => c.resize()))
}

onMounted(async () => {
  await loadKpi()
  await nextTick()
  await initCharts()
})
</script>

<style scoped>
.dashboard { padding: 0; }
.kpi-card { text-align: center; }
.kpi-value { font-size: 32px; font-weight: bold; color: #303133; }
.kpi-label { font-size: 14px; color: #909399; margin-top: 8px; }
</style>
