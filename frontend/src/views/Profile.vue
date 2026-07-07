<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card>
          <template #header><span>👤 个人信息</span></template>
          <el-form :model="form" label-width="80px">
            <el-form-item label="用户名">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave" :loading="saving">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card>
          <template #header><span>📊 账号状态</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="账号状态">
              <el-tag :type="userStore.userInfo?.status === 0 ? 'success' : 'danger'">
                {{ userStore.userInfo?.status === 0 ? '正常' : '冻结中' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="违规次数">
              <span :style="{ color: (userStore.userInfo?.violationCount || 0) >= 3 ? '#f56c6c' : '#303133' }">
                {{ userStore.userInfo?.violationCount || 0 }} / 3
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="用户角色">
              <el-tag :type="userStore.isAdmin ? 'danger' : ''">
                {{ userStore.isAdmin ? '管理员' : '普通用户' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-alert v-if="(userStore.userInfo?.violationCount || 0) >= 3" type="error" title="账号已被冻结" description="违规累计3次，预约权限已被冻结7天。请在冻结期满后再进行预约。" show-icon style="margin-top:16px" />
          <el-alert v-else-if="(userStore.userInfo?.violationCount || 0) > 0" type="warning" :title="`注意：您已有${userStore.userInfo?.violationCount}次违规记录`" description="违规累计达3次将冻结预约权限7天，请按时签到。" show-icon style="margin-top:16px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../store/user'
import { getUserInfo, updateUser } from '../api/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const saving = ref(false)
const form = ref({ username: '', realName: '', phone: '', email: '' })

const loadProfile = async () => {
  try {
    const res = await getUserInfo()
    const data = res.data
    form.value = { username: data.username, realName: data.realName, phone: data.phone, email: data.email }
    userStore.setUserInfo(data)
  } catch (e) { /* ignore */ }
}

const handleSave = async () => {
  saving.value = true
  try {
    await updateUser({ realName: form.value.realName, phone: form.value.phone, email: form.value.email })
    ElMessage.success('修改成功')
    loadProfile()
  } catch (e) { /* ignore */ }
  finally { saving.value = false }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page { max-width: 900px; margin: 0 auto; }
</style>
