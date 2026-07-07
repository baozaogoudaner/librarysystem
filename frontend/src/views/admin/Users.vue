<template>
  <div class="admin-users">
    <el-card>
      <template #header><span>👥 用户管理</span></template>

      <el-table :data="users" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column label="角色" width="80">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : ''" size="small">
              {{ row.role === 1 ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : '冻结' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="violationCount" label="违规" width="60" />
        <el-table-column label="操作" fixed="right" width="160">
          <template #default="{ row }">
            <el-button v-if="row.status === 0 && row.role !== 1" type="danger" size="small" @click="freeze(row)">冻结</el-button>
            <el-button v-if="row.status === 1" type="success" size="small" @click="unfreeze(row)">解冻</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listUsers, updateUserStatus } from '../../api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])

const loadUsers = async () => {
  try {
    const res = await listUsers()
    users.value = res.data || []
  } catch (e) { /* ignore */ }
}

const freeze = async (row) => {
  await ElMessageBox.confirm(`确定冻结用户 ${row.username}？`, '提示', { type: 'warning' })
  try { await updateUserStatus(row.id, 1); ElMessage.success('已冻结'); loadUsers() } catch (e) { /* ignore */ }
}

const unfreeze = async (row) => {
  try { await updateUserStatus(row.id, 0); ElMessage.success('已解冻'); loadUsers() } catch (e) { /* ignore */ }
}

onMounted(loadUsers)
</script>

<style scoped>
.admin-users { max-width: 1100px; margin: 0 auto; }
</style>
