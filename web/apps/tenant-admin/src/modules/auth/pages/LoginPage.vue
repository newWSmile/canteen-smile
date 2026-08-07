<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { passwordLogin } from '../api/authApi'
import { getOrCreateDeviceId } from '../device'
import { encryptPassword, PasswordEnvelopeError } from '../passwordEnvelope'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'
import { saveTenantAdminToken } from '@/shared/http/client'

const route = useRoute()
const router = useRouter()
const form = reactive({
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: '',
  rememberMe: false,
})
const ready = computed(() => form.username.trim().length > 0 && form.password.length > 0)

const loginFlight = useSingleFlight(async () => {
  if (!ready.value) {
    feedback.warning('请输入用户名和密码')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(form.password, 'TENANT_PASSWORD_LOGIN')
    form.password = ''
    const result = await passwordLogin({
      appCode: 'TENANT_ADMIN',
      username: form.username.trim(),
      passwordEnvelope,
      rememberMe: form.rememberMe,
      device: {
        deviceId: getOrCreateDeviceId(),
        deviceType: 'WEB',
        deviceName: '租户管理端浏览器',
        userAgentSummary: navigator.userAgent.slice(0, 256),
      },
    })
    if (result.nextStep !== 'AUTHENTICATED' || !result.session) {
      feedback.error('登录响应缺少有效会话')
      return
    }
    saveTenantAdminToken(result.session.tokenValue, form.rememberMe)
    feedback.success('登录成功')
    await router.replace({ name: 'home' })
  } catch (error) {
    if (error instanceof PasswordEnvelopeError) feedback.error(error.message)
  }
})
</script>

<template>
  <main class="login-shell">
    <section class="brand-panel">
      <p class="eyebrow">CANTEEN SMILE · TENANT</p>
      <h1>机构、账号与权限，始终在租户边界内治理。</h1>
      <p>租户身份与平台身份严格隔离，登录会话按租户安全策略建立。</p>
    </section>
    <section class="form-panel">
      <form class="login-card" @submit.prevent="loginFlight.run()">
        <p class="eyebrow">TENANT ADMINISTRATION</p>
        <h2>登录租户管理端</h2>
        <label><span>用户名</span><el-input v-model="form.username" autocomplete="username" maxlength="64" size="large" /></label>
        <label><span>密码</span><el-input v-model="form.password" type="password" show-password autocomplete="current-password" maxlength="128" size="large" /></label>
        <el-checkbox v-model="form.rememberMe">记住我</el-checkbox>
        <el-button native-type="submit" type="primary" size="large" :disabled="!ready" :loading="loginFlight.pending.value">登录</el-button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-shell { min-height: 100vh; display: grid; grid-template-columns: 1.05fr .95fr; background: #f4f7f5; }
.brand-panel { padding: clamp(48px,8vw,110px); display: flex; flex-direction: column; justify-content: center; color: #effbfb; background: radial-gradient(circle at 15% 15%, rgba(130,224,218,.25), transparent 34%), #123f50; }
.eyebrow { color: #65d2cc; font-size: 11px; font-weight: 700; letter-spacing: .15em; }
.brand-panel h1 { max-width: 650px; margin: 12px 0 24px; font-size: clamp(38px,4.5vw,66px); line-height: 1.13; }
.brand-panel > p:last-child { max-width: 560px; color: rgba(239,251,251,.7); line-height: 1.8; }
.form-panel { padding: 28px; display: grid; place-items: center; }
.login-card { width: min(100%,430px); padding: 44px; display: grid; gap: 20px; border: 1px solid #dce5e1; border-radius: 24px; background: #fff; box-shadow: 0 28px 70px rgba(23,67,75,.1); }
.login-card h2 { margin: -8px 0 8px; font-size: 30px; }
label { display: grid; gap: 8px; font-size: 14px; font-weight: 600; }
@media (max-width: 820px) { .login-shell { grid-template-columns: 1fr; } .brand-panel { min-height: 280px; padding: 48px 28px; } }
</style>
