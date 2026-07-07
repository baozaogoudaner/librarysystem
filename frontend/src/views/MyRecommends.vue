<template>
  <div class="recommend-page">
    <div class="page-header">
      <h2>图书荐购</h2>
      <el-button type="primary" @click="showDialog"><el-icon><Plus /></el-icon> 推荐新书</el-button>
    </div>

    <el-card>
      <el-table :data="recommends" stripe v-loading="loading">
        <el-table-column prop="title" label="书名" min-width="160" />
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="publisher" label="出版社" width="140" />
        <el-table-column prop="reason" label="推荐理由" min-width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" label="审核意见" min-width="120" />
        <el-table-column prop="createTime" label="推荐时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                       layout="total,prev,pager,next" @change="loadRecommends" />
      </div>
    </el-card>

    <!-- Add Dialog -->
    <el-dialog v-model="dialogVisible" title="推荐新书" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="ISBN"><el-input v-model="form.isbn" placeholder="选填" /></el-form-item>
        <el-form-item label="书名"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="作者"><el-input v-model="form.author" /></el-form-item>
        <el-form-item label="出版社"><el-input v-model="form.publisher" placeholder="选填" /></el-form-item>
        <el-form-item label="推荐理由"><el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请简述推荐理由" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRecommend" :loading="submitting">提交推荐</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listRecommends, recommendBook } from '../api/book'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const recommends = ref([])
const loading = ref(false)
const pageNum = ref(1), pageSize = ref(20), total = ref(0)
const dialogVisible = ref(false)
const submitting = ref(false)
const form = reactive({ isbn: '', title: '', author: '', publisher: '', reason: '' })

onMounted(() => loadRecommends())

async function loadRecommends() {
  loading.value = true
  try {
    // 只加载当前用户的荐购记录
    const res = await listRecommends({ pageNum: pageNum.value, pageSize: pageSize.value, userId: userStore.userInfo?.userId })
    recommends.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const statusType = (s) => ({ 0: 'info', 1: 'success', 2: 'danger', 3: 'primary' }[s] || 'info')
const statusText = (s) => ({ 0: '待审核', 1: '已采纳', 2: '已拒绝', 3: '已采购' }[s] || '未知')
const formatDate = (d) => d ? new Date(d).toLocaleString('zh-CN') : '-'

function showDialog() {
  Object.assign(form, { isbn: '', title: '', author: '', publisher: '', reason: '' })
  dialogVisible.value = true
}

async function submitRecommend() {
  if (!form.title || !form.author) {
    ElMessage.warning('请填写书名和作者')
    return
  }
  submitting.value = true
  try {
    await recommendBook(form)
    ElMessage.success('推荐成功，请等待审核')
    dialogVisible.value = false
    loadRecommends()
  } catch (e) { ElMessage.error(e.response?.data?.message || '推荐失败') }
  finally { submitting.value = false }
}
</script>

<style scoped>
.recommend-page { padding: 20px; max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
