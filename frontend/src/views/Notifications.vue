<template>
  <div class="notifications-page">
    <div class="page-header">
      <h2>消息中心</h2>
      <el-button @click="handleMarkAll" :disabled="unreadCount === 0">全部已读</el-button>
    </div>

    <el-card>
      <el-empty v-if="notifications.length === 0" description="暂无通知" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in notifications" :key="item.id"
          :timestamp="formatDate(item.createTime)"
          :color="item.status === 0 ? '#409EFF' : '#ccc'"
          placement="top">
          <el-card shadow="hover" :class="{ unread: item.status === 0 }">
            <div class="notify-header">
              <el-tag :type="notifyTypeColor(item.type)" size="small">{{ notifyTypeText(item.type) }}</el-tag>
              <span class="title">{{ item.title }}</span>
              <el-badge v-if="item.status === 0" is-dot style="margin-left:auto" />
            </div>
            <p class="content">{{ item.content }}</p>
            <el-button v-if="item.status === 0" size="small" type="text" @click="handleRead(item)">标为已读</el-button>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                       layout="total,prev,pager,next" @change="loadNotifications" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead } from '../api/notification'

const notifications = ref([])
const unreadCount = ref(0)
const pageNum = ref(1), pageSize = ref(20), total = ref(0)

onMounted(() => { loadNotifications(); loadUnreadCount() })

async function loadNotifications() {
  try {
    const res = await getNotifications({ pageNum: pageNum.value, pageSize: pageSize.value })
    notifications.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) { /* silent */ }
}

async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data || 0
  } catch (e) { /* silent */ }
}

function notifyTypeColor(t) {
  return { 0: 'warning', 1: 'danger', 2: 'success', 3: 'primary', 4: 'info' }[t] || 'info'
}
function notifyTypeText(t) {
  return { 0: '到期提醒', 1: '逾期警告', 2: '预约可取', 3: '荐购进度', 4: '库存预警' }[t] || '通知'
}
const formatDate = (d) => d ? new Date(d).toLocaleString('zh-CN') : '-'

async function handleRead(item) {
  try {
    await markAsRead(item.id)
    item.status = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (e) { ElMessage.error('操作失败') }
}

async function handleMarkAll() {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => n.status = 1)
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (e) { ElMessage.error('操作失败') }
}
</script>

<style scoped>
.notifications-page { padding: 20px; max-width: 1000px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.notify-header { display: flex; align-items: center; gap: 8px; }
.title { font-weight: bold; }
.content { color: #666; margin: 8px 0; }
.unread { border-left: 3px solid #409EFF; }
</style>
