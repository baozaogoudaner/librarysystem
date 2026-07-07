<template>
  <div class="admin-reservations">
    <el-card>
      <template #header><span>📋 预约管理</span></template>

      <el-form :inline="true" style="margin-bottom: 16px">
        <el-form-item label="日期">
          <el-date-picker v-model="filterDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" placeholder="选择日期" clearable @change="loadData" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable @change="loadData">
            <el-option label="待签到" :value="0" />
            <el-option label="使用中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
            <el-option label="违规" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="reservations" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="userId" label="用户ID" width="70" />
        <el-table-column prop="seatNo" label="座位" width="80" />
        <el-table-column prop="roomName" label="阅览室" width="140" />
        <el-table-column prop="date" label="日期" width="110" />
        <el-table-column label="时段" width="140">
          <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkInTime" label="签到时间" width="160">
          <template #default="{ row }">{{ row.checkInTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status === 0 || row.status === 1" type="danger" size="small" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listAllReservations, adminCancelReservation } from '../../api/reservation'
import { ElMessage, ElMessageBox } from 'element-plus'

const reservations = ref([])
const filterDate = ref('')
const filterStatus = ref(null)

const statusText = (s) => ({ 0: '待签到', 1: '使用中', 2: '已完成', 3: '已取消', 4: '违规' })[s]
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info', 3: '', 4: 'danger' })[s]

const loadData = async () => {
  try {
    const params = {}
    if (filterDate.value) params.date = filterDate.value
    if (filterStatus.value !== null && filterStatus.value !== '') params.status = filterStatus.value
    const res = await listAllReservations(params)
    reservations.value = res.data || []
  } catch (e) { /* ignore */ }
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确定取消该预约？', '提示', { type: 'warning' })
  try { await adminCancelReservation(row.id); ElMessage.success('已取消'); loadData() } catch (e) { /* ignore */ }
}

onMounted(loadData)
</script>

<style scoped>
.admin-reservations { max-width: 1200px; margin: 0 auto; }
</style>
