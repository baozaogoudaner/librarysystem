<template>
  <div class="reservations-page">
    <el-card>
      <template #header><span>📋 我的预约记录</span></template>

      <el-table :data="reservations" stripe style="width: 100%">
        <el-table-column prop="seatNo" label="座位编号" width="100" />
        <el-table-column prop="roomName" label="阅览室" width="150" />
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column label="时段" width="150">
          <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="签到时间" width="170">
          <template #default="{ row }">{{ row.checkInTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="签退时间" width="170">
          <template #default="{ row }">{{ row.checkOutTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="success" size="small" @click="handleCheckIn(row)">签到</el-button>
            <el-button v-if="row.status === 1" type="warning" size="small" @click="handleCheckOut(row)">签退</el-button>
            <el-button v-if="row.status === 0" type="danger" size="small" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyReservations, checkIn, checkOut, cancelReservation } from '../api/reservation'
import { ElMessage, ElMessageBox } from 'element-plus'

const reservations = ref([])

const statusText = (s) => ({ 0: '待签到', 1: '使用中', 2: '已完成', 3: '已取消', 4: '违规' })[s] || '未知'
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info', 3: '', 4: 'danger' })[s] || ''

const loadData = async () => {
  try {
    const res = await getMyReservations()
    reservations.value = res.data?.records || []
  } catch (e) { /* ignore */ }
}

const handleCheckIn = async (row) => {
  try { await checkIn(row.id); ElMessage.success('签到成功'); loadData() } catch (e) { /* ignore */ }
}

const handleCheckOut = async (row) => {
  try { await checkOut(row.id); ElMessage.success('签退成功'); loadData() } catch (e) { /* ignore */ }
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确定取消预约？', '提示', { type: 'warning' })
  try { await cancelReservation(row.id); ElMessage.success('已取消'); loadData() } catch (e) { /* ignore */ }
}

onMounted(loadData)
</script>

<style scoped>
.reservations-page { max-width: 1100px; margin: 0 auto; }
</style>
