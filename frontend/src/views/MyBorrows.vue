<template>
  <div class="my-borrows-page">
    <h2>我的借阅</h2>
    <el-tabs v-model="activeTab" @tab-change="tabChange">
      <el-tab-pane label="当前借阅" name="active" />
      <el-tab-pane label="借阅历史" name="history" />
      <el-tab-pane label="我的预约" name="reserves" />
    </el-tabs>

    <!-- Active Borrows -->
    <el-table v-if="activeTab === 'active'" :data="activeBorrows" stripe v-loading="loadingA">
      <el-table-column prop="bookTitle" label="书名" min-width="180" />
      <el-table-column prop="isbn" label="ISBN" width="140" />
      <el-table-column prop="borrowTime" label="借阅日期" width="160">
        <template #default="{ row }">{{ formatDate(row.borrowTime) }}</template>
      </el-table-column>
      <el-table-column prop="dueDate" label="应还日期" width="160">
        <template #default="{ row }">
          <span :style="{ color: isOverdue(row.dueDate) ? 'red' : 'inherit' }">
            {{ formatDate(row.dueDate) }}
            <el-tag v-if="isOverdue(row.dueDate)" type="danger" size="small">逾期</el-tag>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="borrowStatusType(row.status)">{{ borrowStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="续借次数" width="80">
        <template #default="{ row }">{{ row.renewCount }}/2</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="handleRenew(row)" :disabled="row.renewCount >= 2 || isOverdue(row.dueDate)">续借</el-button>
          <el-button size="small" type="primary" @click="handleReturn(row)">还书</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- History Borrows -->
    <el-table v-if="activeTab === 'history'" :data="historyBorrows" stripe v-loading="loadingH">
      <el-table-column prop="bookTitle" label="书名" min-width="180" />
      <el-table-column prop="isbn" label="ISBN" width="140" />
      <el-table-column prop="borrowTime" label="借阅日期" width="160">
        <template #default="{ row }">{{ formatDate(row.borrowTime) }}</template>
      </el-table-column>
      <el-table-column prop="returnTime" label="归还日期" width="160">
        <template #default="{ row }">{{ formatDate(row.returnTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag type="info">已归还</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <!-- Book Reserves -->
    <el-table v-if="activeTab === 'reserves'" :data="reserves" stripe v-loading="loadingR">
      <el-table-column prop="bookTitle" label="书名" min-width="180" />
      <el-table-column prop="isbn" label="ISBN" width="140" />
      <el-table-column prop="reserveTime" label="预约时间" width="160">
        <template #default="{ row }">{{ formatDate(row.reserveTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="reserveStatusType(row.status)">{{ reserveStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" size="small" type="danger" @click="handleCancelReserve(row)">取消</el-button>
          <el-button v-if="row.status === 1" size="small" type="primary" @click="handlePickup(row)">取书</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div style="margin-top:16px;text-align:right">
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                     layout="total,prev,pager,next" @change="loadData" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyBorrows, returnBook, renewBook, getMyReserves, cancelReserve } from '../api/borrowing'

const activeTab = ref('active')
const activeBorrows = ref([])
const historyBorrows = ref([])
const reserves = ref([])
const loadingA = ref(false), loadingH = ref(false), loadingR = ref(false)
const pageNum = ref(1), pageSize = ref(20), total = ref(0)

function tabChange() { pageNum.value = 1; loadData() }

async function loadData() {
  switch (activeTab.value) {
    case 'active': loadingA.value = true; break
    case 'history': loadingH.value = true; break
    case 'reserves': loadingR.value = true; break
  }
  try {
    if (activeTab.value === 'active') {
      const res = await getMyBorrows({ pageNum: pageNum.value, pageSize: pageSize.value, status: 0 })
      activeBorrows.value = (res.data?.records || []).concat(
        (await getMyBorrows({ pageNum: pageNum.value, pageSize: pageSize.value, status: 3 })).data?.records || []
      )
      total.value = res.data?.total || 0
    } else if (activeTab.value === 'history') {
      const res = await getMyBorrows({ pageNum: pageNum.value, pageSize: pageSize.value, status: 1 })
      historyBorrows.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      const res = await getMyReserves({ pageNum: pageNum.value, pageSize: pageSize.value })
      reserves.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } catch (e) { ElMessage.error('加载失败') }
  finally {
    loadingA.value = loadingH.value = loadingR.value = false
  }
}

loadData()

const isOverdue = (dateStr) => new Date(dateStr) < new Date()
const formatDate = (dateStr) => dateStr ? new Date(dateStr).toLocaleString('zh-CN') : '-'
const borrowStatusType = (s) => ({ 0: 'warning', 1: 'info', 2: 'danger', 3: 'primary' }[s] || 'info')
const borrowStatusText = (s) => ({ 0: '借出中', 1: '已归还', 2: '逾期', 3: '续借中' }[s] || '未知')
const reserveStatusType = (s) => ({ 0: 'info', 1: 'success', 2: 'primary', 3: 'warning', 4: 'danger' }[s] || 'info')
const reserveStatusText = (s) => ({ 0: '等待中', 1: '可取', 2: '已取', 3: '已取消', 4: '已过期' }[s] || '未知')

async function handleReturn(borrow) {
  await ElMessageBox.confirm(`确认归还《${borrow.bookTitle}》？`, '还书确认', { type: 'info' })
  try {
    await returnBook(borrow.id)
    ElMessage.success('归还成功')
    loadData()
  } catch (e) { ElMessage.error(e.response?.data?.message || '归还失败') }
}

async function handleRenew(borrow) {
  await ElMessageBox.confirm(`确认续借《${borrow.bookTitle}》？（续借${15}天）`, '续借确认')
  try {
    await renewBook(borrow.id)
    ElMessage.success('续借成功')
    loadData()
  } catch (e) { ElMessage.error(e.response?.data?.message || '续借失败') }
}

async function handleCancelReserve(reserve) {
  await ElMessageBox.confirm('确认取消预约？', '取消确认', { type: 'warning' })
  try {
    await cancelReserve(reserve.id)
    ElMessage.success('已取消')
    loadData()
  } catch (e) { ElMessage.error(e.response?.data?.message || '取消失败') }
}

async function handlePickup(reserve) {
  ElMessage.info('请到服务台办理取书手续')
}
</script>

<style scoped>
.my-borrows-page { padding: 20px; max-width: 1200px; margin: 0 auto; }
h2 { margin-bottom: 16px; }
</style>
