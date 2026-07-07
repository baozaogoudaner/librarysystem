<template>
  <div class="reports-page">
    <h2>统计报表与预警</h2>

    <!-- KPI Cards -->
    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :span="6" v-for="kpi in kpis" :key="kpi.label">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-value" :style="{ color: kpi.color }">{{ kpi.value }}</div>
          <div class="kpi-label">{{ kpi.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Hot Books Ranking -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header><h3><el-icon><TrendCharts /></el-icon> 热门借阅排行 TOP 20</h3></template>
          <el-table :data="hotBooks" size="small" v-loading="loadingHot">
            <el-table-column type="index" label="# " width="50">
              <template #default="{ $index }">
                <el-tag v-if="$index < 3" :type="['danger', 'warning', 'primary'][$index]" size="small">{{ $index + 1 }}</el-tag>
                <span v-else>{{ $index + 1 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="书名" min-width="160" />
            <el-table-column prop="author" label="作者" width="100" />
            <el-table-column prop="borrowCount" label="借阅次数" width="90" />
            <el-table-column label="库存" width="80">
              <template #default="{ row }">{{ row.availableStock }}/{{ row.totalStock }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <!-- Low Stock Alert -->
        <el-card style="margin-bottom: 16px">
          <template #header>
            <h3><el-icon><Warning /></el-icon> 库存预警（低库存图书）</h3>
          </template>
          <el-table :data="lowStockBooks" size="small" v-loading="loadingLow">
            <el-table-column prop="title" label="书名" min-width="150" />
            <el-table-column prop="author" label="作者" width="90" />
            <el-table-column label="剩余库存" width="80">
              <template #default="{ row }">
                <span style="color:red;font-weight:bold">{{ row.availableStock }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="borrowCount" label="借阅次数" width="80" />
          </el-table>
        </el-card>

        <!-- Category Distribution -->
        <el-card>
          <template #header><h3><el-icon><PieChart /></el-icon> 分类统计</h3></template>
          <el-table :data="categoryStats" size="small" v-loading="loadingCat">
            <el-table-column prop="category" label="分类" />
            <el-table-column prop="count" label="藏书数量" width="100" />
            <el-table-column label="占比" width="100">
              <template #default="{ row }">
                <el-progress :percentage="row.percentage" :show-text="false" />
                <span style="margin-left:4px;font-size:12px">{{ row.percentage }}%</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getHotBooks, getLowStockBooks, getCategoryStats } from '../../api/book'
import { getOverdueBorrows } from '../../api/borrowing'
import { TrendCharts, Warning, PieChart } from '@element-plus/icons-vue'

const kpis = ref([])
const hotBooks = ref([])
const lowStockBooks = ref([])
const categoryStats = ref([])
const loadingHot = ref(false), loadingLow = ref(false), loadingCat = ref(false)

onMounted(() => loadAll())

async function loadAll() {
  loadingHot.value = loadingLow.value = loadingCat.value = true
  try {
    const [hotRes, lowRes, catRes, overdueRes] = await Promise.all([
      getHotBooks(20), getLowStockBooks(), getCategoryStats(), getOverdueBorrows()
    ])
    hotBooks.value = hotRes.data || []
    lowStockBooks.value = lowRes.data || []
    const overdueCount = (overdueRes.data || []).length

    // KPI
    kpis.value = [
      { label: '总图书量', value: '-', color: '#409EFF' },
      { label: '热门借阅', value: hotBooks.value.length + ' 种', color: '#67C23A' },
      { label: '低库存预警', value: lowStockBooks.value.length + ' 种', color: '#F56C6C' },
      { label: '逾期未还', value: overdueCount + ' 册', color: '#E6A23C' }
    ]

    // Category stats with percentage
    const totalBooks = (catRes.data || []).reduce((sum, c) => sum + (c.count || 0), 0)
    categoryStats.value = (catRes.data || []).map(c => ({
      ...c,
      percentage: totalBooks > 0 ? Math.round((c.count / totalBooks) * 100) : 0
    }))
  } catch (e) { /* silent */ }
  finally {
    loadingHot.value = loadingLow.value = loadingCat.value = false
  }
}
</script>

<style scoped>
.reports-page { padding: 20px; }
h2 { margin-bottom: 16px; }
h3 { font-size: 15px; display: flex; align-items: center; gap: 6px; }
.kpi-card { text-align: center; }
.kpi-value { font-size: 32px; font-weight: bold; margin-bottom: 4px; }
.kpi-label { font-size: 13px; color: #999; }
</style>
