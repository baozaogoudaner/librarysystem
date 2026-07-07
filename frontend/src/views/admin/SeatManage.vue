<template>
  <div class="admin-seats">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>💺 座位管理</span>
          <div>
            <el-select v-model="selectedRoom" placeholder="选择阅览室" @change="loadSeats" style="margin-right:10px">
              <el-option v-for="room in rooms" :key="room.id" :label="room.name" :value="room.id" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table :data="seats" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="seatNo" label="座位编号" width="100" />
        <el-table-column prop="roomName" label="阅览室" width="150" />
        <el-table-column label="位置" width="100">
          <template #default="{ row }">第{{ row.rowNum }}行 第{{ row.colNum }}列</template>
        </el-table-column>
        <el-table-column label="电源" width="80">
          <template #default="{ row }">
            <el-tag :type="row.hasPower ? 'success' : 'info'" size="small">{{ row.hasPower ? '有' : '无' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '可用' : '维护中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="warning" size="small" @click="toggleStatus(row, 1)">设为维护</el-button>
            <el-button v-else type="success" size="small" @click="toggleStatus(row, 0)">恢复可用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listRooms, listSeatsByRoom, updateSeatStatus } from '../../api/seat'
import { ElMessage } from 'element-plus'

const rooms = ref([])
const seats = ref([])
const selectedRoom = ref(null)

const loadRooms = async () => {
  try {
    const res = await listRooms()
    rooms.value = res.data || []
    if (rooms.value.length) {
      selectedRoom.value = rooms.value[0].id
      loadSeats()
    }
  } catch (e) { /* ignore */ }
}

const loadSeats = async () => {
  if (!selectedRoom.value) return
  try {
    const res = await listSeatsByRoom(selectedRoom.value)
    seats.value = res.data || []
  } catch (e) { /* ignore */ }
}

const toggleStatus = async (row, status) => {
  try {
    await updateSeatStatus(row.id, status)
    ElMessage.success('状态已更新')
    loadSeats()
  } catch (e) { /* ignore */ }
}

onMounted(loadRooms)
</script>

<style scoped>
.admin-seats { max-width: 1000px; margin: 0 auto; }
</style>
