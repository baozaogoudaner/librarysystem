<template>
  <div class="seats-page">
    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="阅览室">
          <el-select v-model="selectedRoom" placeholder="选择阅览室" @change="loadSeats">
            <el-option v-for="room in rooms" :key="room.id" :label="room.name" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="selectedDate" type="date" placeholder="选择日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD"
            :disabled-date="disabledDate" @change="loadSeats" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-select v-model="startTime" start="07:00" step="00:30" end="22:00" placeholder="开始" @change="loadSeats" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-select v-model="endTime" :start="startTime || '07:00'" step="00:30" end="23:00" placeholder="结束" @change="loadSeats" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图例 -->
    <el-card class="legend-card">
      <div class="legend">
        <span class="legend-item"><span class="dot available"></span>可预约</span>
        <span class="legend-item"><span class="dot reserved"></span>已预约</span>
        <span class="legend-item"><span class="dot selected"></span>已选中</span>
        <span class="legend-item"><span class="dot maintenance"></span>维护中</span>
        <span class="legend-item"><span class="dot power"></span>⚡ 有电源</span>
      </div>
    </el-card>

    <!-- 座位网格 -->
    <el-card>
      <template #header>
        <span>{{ currentRoom?.name || '请选择阅览室' }} - 座位布局</span>
      </template>

      <div v-if="seats.length" class="seat-grid" :style="{ gridTemplateColumns: `repeat(${maxCol}, 56px)` }">
        <div
          v-for="seat in seats"
          :key="seat.id"
          :class="['seat-item', seatClass(seat)]"
          @click="selectSeat(seat)"
          :title="`${seat.seatNo}${seat.hasPower ? ' ⚡' : ''}`"
        >
          <div>{{ seat.seatNo.split('-')[1] }}</div>
          <div v-if="seat.hasPower" style="font-size:10px">⚡</div>
        </div>
      </div>
      <el-empty v-else description="请先选择阅览室和时间段" />
    </el-card>

    <!-- 预约对话框 -->
    <el-dialog v-model="showDialog" title="确认预约" width="450px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="座位编号">{{ selectedSeat?.seatNo }}</el-descriptions-item>
        <el-descriptions-item label="阅览室">{{ selectedSeat?.roomName }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ selectedDate }}</el-descriptions-item>
        <el-descriptions-item label="时段">{{ startTime }} - {{ endTime }}</el-descriptions-item>
        <el-descriptions-item label="是否有电源">{{ selectedSeat?.hasPower ? '是 ⚡' : '否' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmReservation" :loading="reserving">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { listRooms, listSeatsByRoom, getAvailableSeats } from '../api/seat'
import { createReservation } from '../api/reservation'
import { ElMessage } from 'element-plus'

const rooms = ref([])
const seats = ref([])
const availableSeatIds = ref(new Set())
const selectedRoom = ref(null)
const selectedDate = ref(new Date().toISOString().split('T')[0])
const startTime = ref('08:00')
const endTime = ref('12:00')
const selectedSeat = ref(null)
const showDialog = ref(false)
const reserving = ref(false)

const currentRoom = computed(() => rooms.value.find(r => r.id === selectedRoom.value))
const maxCol = computed(() => {
  if (!seats.value.length) return 6
  return Math.max(...seats.value.map(s => s.colNum))
})

const disabledDate = (date) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

const seatClass = (seat) => {
  if (seat.status === 1) return 'seat-maintenance'
  if (selectedSeat.value?.id === seat.id) return 'seat-selected'
  if (!availableSeatIds.value.has(seat.id)) return 'seat-reserved'
  return 'seat-available'
}

const loadRooms = async () => {
  try {
    const res = await listRooms()
    rooms.value = res.data || []
    if (rooms.value.length > 0) {
      selectedRoom.value = rooms.value[0].id
      loadSeats()
    }
  } catch (e) { /* ignore */ }
}

const loadSeats = async () => {
  if (!selectedRoom.value || !selectedDate.value || !startTime.value || !endTime.value) return

  try {
    // 加载所有座位
    const res = await listSeatsByRoom(selectedRoom.value)
    // 按行、列排序保证布局整齐
    const raw = res.data || []
    raw.sort((a, b) => a.rowNum - b.rowNum || a.colNum - b.colNum)
    seats.value = raw

    // 加载可用座位
    const res2 = await getAvailableSeats({
      roomId: selectedRoom.value,
      date: selectedDate.value,
      startTime: startTime.value,
      endTime: endTime.value,
    })
    availableSeatIds.value = new Set((res2.data || []).map(s => s.id))
  } catch (e) { /* ignore */ }
}

const selectSeat = (seat) => {
  if (seat.status === 1) return
  if (!availableSeatIds.value.has(seat.id)) {
    ElMessage.warning('该座位在此时段已被预约')
    return
  }
  selectedSeat.value = seat
  showDialog.value = true
}

const confirmReservation = async () => {
  reserving.value = true
  try {
    await createReservation({
      seatId: selectedSeat.value.id,
      date: selectedDate.value,
      startTime: startTime.value,
      endTime: endTime.value,
    })
    ElMessage.success('预约成功！请在30分钟内签到')
    showDialog.value = false
    selectedSeat.value = null
    loadSeats()
  } catch (e) { /* ignore */ }
  finally {
    reserving.value = false
  }
}

onMounted(loadRooms)
</script>

<style scoped>
.seats-page { max-width: 1000px; margin: 0 auto; }
.filter-card, .legend-card { margin-bottom: 16px; }
.legend { display: flex; gap: 20px; align-items: center; flex-wrap: wrap; }
.legend-item { display: flex; align-items: center; gap: 6px; font-size: 13px; }
.dot { width: 16px; height: 16px; border-radius: 4px; display: inline-block; }
.dot.available { background: #e8f5e9; border: 2px solid #a5d6a7; }
.dot.reserved { background: #ffebee; border: 2px solid #ef9a9a; }
.dot.selected { background: #e3f2fd; border: 2px solid #42a5f5; }
.dot.maintenance { background: #f5f5f5; border: 2px solid #e0e0e0; }
.dot.power { background: #fff8e1; border: 2px solid #ffcc80; }
.seat-grid { display: grid; gap: 8px; justify-content: center; padding: 20px 0; }
</style>
