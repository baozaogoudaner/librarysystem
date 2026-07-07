<template>
  <div class="recommends-page">
    <h2>荐购管理</h2>
    <el-card>
      <el-table :data="recommends" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="推荐人" width="100" />
        <el-table-column prop="title" label="书名" min-width="160" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="publisher" label="出版社" width="130" />
        <el-table-column prop="reason" label="推荐理由" min-width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <template v-if="row && row.status === 0">
              <el-button size="small" type="success" @click="review(row.id, 1)">采纳</el-button>
              <el-button size="small" type="danger" @click="review(row.id, 2)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                       layout="total,sizes,prev,pager,next" @change="loadRecommends" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRecommends, reviewRecommend } from '../../api/book'

const recommends = ref([])
const loading = ref(false)
const pageNum = ref(1), pageSize = ref(20), total = ref(0)

onMounted(() => loadRecommends())

async function loadRecommends() {
  loading.value = true
  try {
    const res = await listRecommends({ pageNum: pageNum.value, pageSize: pageSize.value })
    recommends.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const statusType = (s) => ({ 0: 'info', 1: 'success', 2: 'danger', 3: 'primary' }[s] || 'info')
const statusText = (s) => ({ 0: '待审核', 1: '已采纳', 2: '已拒绝', 3: '已采购' }[s] || '未知')

async function review(id, status) {
  const label = status === 1 ? '采纳' : '拒绝'
  const { value: comment } = await ElMessageBox.prompt(`请输入${label}意见`, label, { inputType: 'textarea' })
  try {
    await reviewRecommend(id, status, comment || '')
    ElMessage.success(`${label}成功`)
    loadRecommends()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}
</script>

<style scoped>
.recommends-page { padding: 20px; }
h2 { margin-bottom: 16px; }
</style>
