<template>
  <div class="borrows-page">
    <h2>借阅管理</h2>

    <!-- Quick Stats -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <el-card shadow="never">
          <el-statistic :title="s.label" :value="s.value" :value-style="{ color: s.color }" />
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-form :inline="true">
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" @change="loadBorrows">
            <el-option label="借出中" :value="0" />
            <el-option label="已归还" :value="1" />
            <el-option label="逾期" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="filters.userId" clearable @keyup.enter="loadBorrows" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadBorrows">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="borrows" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column prop="bookTitle" label="书名" min-width="180" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column label="借阅日期" width="160">
          <template #default="{ row }">{{ formatDate(row.borrowTime) }}</template>
        </el-table-column>
        <el-table-column label="应还日期" width="160">
          <template #default="{ row }">
            <span :style="{ color: isOverdue(row.dueDate) && row.status !== 1 ? 'red' : 'inherit' }">
              {{ formatDate(row.dueDate) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="归还日期" width="160">
          <template #default="{ row }">{{ formatDate(row.returnTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                       layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50]" @change="loadBorrows" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAllBorrows, getOverdueBorrows } from '../../api/borrowing'

const borrows = ref([])
const loading = ref(false)
const pageNum = ref(1), pageSize = ref(20), total = ref(0)
const filters = reactive({ status: null, userId: '' })
const stats = ref([])

onMounted(() => { loadBorrows(); loadStats() })

async function loadBorrows() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filters.status !== null && filters.status !== '') params.status = filters.status
    if (filters.userId) params.userId = filters.userId
    const res = await listAllBorrows(params)
    borrows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const overdueRes = await getOverdueBorrows()
    const overdueCount = (overdueRes.data || []).length
    stats.value = [
      { label: '当前借出', value: '-', color: '#409EFF' },
      { label: '逾期未还', value: overdueCount, color: '#F56C6C' },
      { label: '今日借阅', value: '-', color: '#67C23A' },
      { label: '今日归还', value: '-', color: '#E6A23C' }
    ]
  } catch (e) { /* silent */ }
}

const formatDate = (d) => d ? new Date(d).toLocaleString('zh-CN') : '-'
const isOverdue = (d) => new Date(d) < new Date()
const statusType = (s) => ({ 0: 'warning', 1: 'info', 2: 'danger', 3: 'primary' }[s] || 'info')
const statusText = (s) => ({ 0: '借出中', 1: '已归还', 2: '逾期', 3: '续借中' }[s] || '未知')
</script>

<style scoped>
.borrows-page { padding: 20px; }
h2 { margin-bottom: 16px; }
</style>
