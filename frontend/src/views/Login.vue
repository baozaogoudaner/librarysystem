<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="40" color="#409eff"><Reading /></el-icon>
        <h1>图书馆座位预约系统</h1>
        <p>Library Seat Reservation System</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @submit.prevent="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" placeholder="请输入密码" prefix-icon="Lock" type="password" show-password size="large" />
            </el-form-item>
            <el-form-item prop="captchaCode">
              <div style="display: flex; gap: 10px;">
                <el-input v-model="loginForm.captchaCode" placeholder="验证码" prefix-icon="Key" size="large" style="flex: 1" />
                <img :src="loginCaptchaImg" @click="refreshCaptcha('login')"
                     style="height: 40px; width: 120px; cursor: pointer; border: 1px solid #dcdfe6; border-radius: 4px;" title="点击刷新验证码" />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleLogin" :loading="loading" size="large" style="width: 100%">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" @submit.prevent="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" placeholder="请输入密码" prefix-icon="Lock" type="password" show-password size="large" />
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" prefix-icon="Postcard" size="large" />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" placeholder="请输入手机号" prefix-icon="Phone" size="large" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="请输入邮箱" prefix-icon="Message" size="large" />
            </el-form-item>
            <el-form-item prop="captchaCode">
              <div style="display: flex; gap: 10px;">
                <el-input v-model="registerForm.captchaCode" placeholder="验证码" prefix-icon="Key" size="large" style="flex: 1" />
                <img :src="registerCaptchaImg" @click="refreshCaptcha('register')"
                     style="height: 40px; width: 120px; cursor: pointer; border: 1px solid #dcdfe6; border-radius: 4px;" title="点击刷新验证码" />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleRegister" :loading="loading" size="large" style="width: 100%">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { login, register, getCaptcha } from '../api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = ref({ username: '', password: '', captchaId: '', captchaCode: '' })
const registerForm = ref({ username: '', password: '', realName: '', phone: '', email: '', captchaId: '', captchaCode: '' })

const loginCaptchaImg = ref('')
const registerCaptchaImg = ref('')
const loginCaptchaId = ref('')
const registerCaptchaId = ref('')

// 刷新验证码
const refreshCaptcha = async (type) => {
  try {
    const res = await getCaptcha()
    if (type === 'login') {
      loginCaptchaImg.value = res.data.captchaImg
      loginCaptchaId.value = res.data.captchaId
      loginForm.value.captchaId = res.data.captchaId
    } else {
      registerCaptchaImg.value = res.data.captchaImg
      registerCaptchaId.value = res.data.captchaId
      registerForm.value.captchaId = res.data.captchaId
    }
  } catch (e) {
    // ignore
  }
}

onMounted(() => {
  refreshCaptcha('login')
  refreshCaptcha('register')
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(loginForm.value)
    userStore.setToken(res.data.token)
    userStore.setUserInfo({
      userId: res.data.userId,
      username: res.data.username,
      realName: res.data.realName,
      role: res.data.role,
      status: res.data.status,
      violationCount: res.data.violationCount,
    })
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    refreshCaptcha('login')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register(registerForm.value)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.value.username = registerForm.value.username
    refreshCaptcha('login')
    refreshCaptcha('register')
  } catch (e) {
    refreshCaptcha('register')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  font-size: 22px;
  color: #303133;
  margin: 12px 0 4px;
}

.login-header p {
  font-size: 13px;
  color: #909399;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}
</style>
