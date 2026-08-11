<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import type { SmsChallenge } from '@canteen-smile/contracts'
import { useRoute, useRouter } from 'vue-router'
import {
  accountSelectionLogin,
  createSmsLoginChallenge,
  passwordLogin,
  smsLogin,
} from '../api/authApi'
import type { DeviceRequest, LoginResult, MobileLoginCandidate } from '../types'
import { getOrCreateDeviceId } from '../device'
import { isMainlandMobile, normalizeMobileInput } from '../mobile'
import { encryptPassword, PasswordEnvelopeError } from '../passwordEnvelope'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'
import { saveTenantAdminToken } from '@/shared/http/client'

type LoginMode = 'password' | 'sms'

const route = useRoute()
const router = useRouter()
const loginMode = ref<LoginMode>('password')
const passwordForm = reactive({
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: '',
  rememberMe: false,
})
const smsForm = reactive({ mobile: '', code: '', rememberMe: false })
const smsChallenge = ref<SmsChallenge | null>(null)
const resendSeconds = ref(0)
const selectorTicket = ref('')
const accountCandidates = ref<MobileLoginCandidate[]>([])
let countdownTimer: number | undefined

const passwordReady = computed(
  () => passwordForm.username.trim().length > 0 && passwordForm.password.length > 0,
)
const smsCodeReady = computed(
  () => smsChallenge.value !== null && /^[0-9]{6}$/.test(smsForm.code),
)
const canSendSms = computed(
  () => isMainlandMobile(smsForm.mobile) && resendSeconds.value === 0,
)

/** 过滤非数字并限制大陆手机号长度。 */
function handleMobileInput(value: string): void {
  smsForm.mobile = normalizeMobileInput(value)
}

/** 构造与当前浏览器绑定的受控设备描述。 */
function device(): DeviceRequest {
  return {
    deviceId: getOrCreateDeviceId(),
    deviceType: 'WEB',
    deviceName: '租户管理端浏览器',
    userAgentSummary: navigator.userAgent.slice(0, 256),
  }
}

/** 统一处理认证完成或进入多账号选择的后端结果。 */
async function handleLoginResult(result: LoginResult, rememberMe: boolean): Promise<void> {
  if (result.nextStep === 'AUTHENTICATED' && result.session) {
    saveTenantAdminToken(result.session.tokenValue, rememberMe)
    feedback.success('登录成功')
    await router.replace({ name: 'home' })
    return
  }
  if (
    result.nextStep === 'ACCOUNT_SELECTION_REQUIRED' &&
    result.accountSelectorTicket &&
    result.accountCandidates?.length
  ) {
    selectorTicket.value = result.accountSelectorTicket
    accountCandidates.value = result.accountCandidates
    feedback.success('手机号验证成功，请选择登录账号')
    return
  }
  feedback.error('登录响应缺少有效会话或账号候选')
}

const passwordLoginFlight = useSingleFlight(async () => {
  if (!passwordReady.value) {
    feedback.warning('请输入用户名和密码')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(passwordForm.password, 'TENANT_PASSWORD_LOGIN')
    passwordForm.password = ''
    const result = await passwordLogin({
      appCode: 'TENANT_ADMIN',
      username: passwordForm.username.trim(),
      passwordEnvelope,
      rememberMe: passwordForm.rememberMe,
      device: device(),
    })
    await handleLoginResult(result, passwordForm.rememberMe)
  } catch (error) {
    if (error instanceof PasswordEnvelopeError) feedback.error(error.message)
  }
})

/** 根据后端允许重发时间维护验证码倒计时。 */
function startCountdown(resendAt: string): void {
  if (countdownTimer !== undefined) window.clearInterval(countdownTimer)
  const refresh = (): void => {
    resendSeconds.value = Math.max(0, Math.ceil((new Date(resendAt).getTime() - Date.now()) / 1000))
    if (resendSeconds.value === 0 && countdownTimer !== undefined) {
      window.clearInterval(countdownTimer)
      countdownTimer = undefined
    }
  }
  refresh()
  if (resendSeconds.value > 0) countdownTimer = window.setInterval(refresh, 1000)
}

const sendSmsFlight = useSingleFlight(async () => {
  if (!canSendSms.value) {
    feedback.warning(resendSeconds.value > 0 ? `请在 ${resendSeconds.value} 秒后重试` : '请输入正确的11位手机号')
    return
  }
  smsChallenge.value = await createSmsLoginChallenge({
    purpose: 'LOGIN',
    mobile: smsForm.mobile.trim(),
    deviceId: getOrCreateDeviceId(),
  })
  smsForm.code = ''
  startCountdown(smsChallenge.value.resendAt)
  feedback.success(`验证码已发送至 ${smsChallenge.value.maskedMobile}`)
})

const smsLoginFlight = useSingleFlight(async () => {
  if (!smsChallenge.value || !smsCodeReady.value) {
    feedback.warning('请输入六位短信验证码')
    return
  }
  const result = await smsLogin({
    appCode: 'TENANT_ADMIN',
    challengeId: smsChallenge.value.challengeId,
    code: smsForm.code,
    rememberMe: smsForm.rememberMe,
    device: device(),
  })
  smsForm.code = ''
  await handleLoginResult(result, smsForm.rememberMe)
})

const accountSelectionFlight = useSingleFlight(async (accountId: string) => {
  if (!selectorTicket.value) {
    feedback.warning('账号选择凭证已失效，请重新验证手机号')
    resetSmsFlow()
    return
  }
  const result = await accountSelectionLogin({
    appCode: 'TENANT_ADMIN',
    accountSelectorTicket: selectorTicket.value,
    accountId,
    rememberMe: smsForm.rememberMe,
    device: device(),
  })
  await handleLoginResult(result, smsForm.rememberMe)
})

/** 放弃当前短信挑战和账号选择，重新输入手机号。 */
function resetSmsFlow(): void {
  smsChallenge.value = null
  selectorTicket.value = ''
  accountCandidates.value = []
  smsForm.code = ''
  resendSeconds.value = 0
  if (countdownTimer !== undefined) {
    window.clearInterval(countdownTimer)
    countdownTimer = undefined
  }
}

/** 展示用户所在时区的最近登录时间。 */
function formatLatestLogin(value: string | null): string {
  if (!value) return '首次登录'
  return `最近登录 ${new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))}`
}

onUnmounted(() => {
  if (countdownTimer !== undefined) window.clearInterval(countdownTimer)
})
</script>

<template>
  <main class="login-shell">
    <section class="brand-panel">
      <p class="eyebrow">CANTEEN SMILE · TENANT</p>
      <h1>机构、账号与权限，始终在租户边界内治理。</h1>
      <p>支持用户名密码与手机号验证码登录；一个手机号绑定多个账号时，由本人选择本次进入的身份。</p>
    </section>

    <section class="form-panel">
      <div class="login-card">
        <template v-if="accountCandidates.length > 0">
          <div class="card-heading">
            <p class="eyebrow">SELECT ACCOUNT</p>
            <h2>选择登录账号</h2>
            <span>已按最近登录时间排序，本次选择不会改变手机号绑定关系。</span>
          </div>
          <div class="candidate-list">
            <button
              v-for="candidate in accountCandidates"
              :key="candidate.accountId"
              type="button"
              class="candidate-card"
              :disabled="accountSelectionFlight.pending.value"
              @click="accountSelectionFlight.run(candidate.accountId)"
            >
              <span class="candidate-avatar">{{ candidate.displayName.slice(0, 1) }}</span>
              <span class="candidate-main">
                <strong>{{ candidate.displayName }}</strong>
                <small>{{ candidate.username }} · {{ candidate.organizationName }}</small>
                <em>{{ candidate.tenantName }}</em>
              </span>
              <span class="candidate-recent">{{ formatLatestLogin(candidate.latestLoginTime) }}</span>
            </button>
          </div>
          <el-button text @click="resetSmsFlow">返回手机号验证</el-button>
        </template>

        <template v-else>
          <div class="card-heading">
            <p class="eyebrow">TENANT ADMINISTRATION</p>
            <h2>登录租户管理端</h2>
          </div>
          <div class="mode-switch" role="tablist" aria-label="登录方式">
            <button
              type="button"
              :class="{ active: loginMode === 'password' }"
              @click="loginMode = 'password'"
            >用户名密码</button>
            <button
              type="button"
              :class="{ active: loginMode === 'sms' }"
              @click="loginMode = 'sms'"
            >手机号验证码</button>
          </div>

          <form v-if="loginMode === 'password'" class="login-form" @submit.prevent="passwordLoginFlight.run()">
            <label>
              <span>用户名</span>
              <el-input v-model="passwordForm.username" autocomplete="username" maxlength="64" size="large" />
            </label>
            <label>
              <span>密码</span>
              <el-input v-model="passwordForm.password" type="password" show-password autocomplete="current-password" maxlength="128" size="large" />
            </label>
            <el-checkbox v-model="passwordForm.rememberMe">记住我</el-checkbox>
            <el-button native-type="submit" type="primary" size="large" :disabled="!passwordReady" :loading="passwordLoginFlight.pending.value">登录</el-button>
          </form>

          <form v-else class="login-form" @submit.prevent="smsLoginFlight.run()">
            <label>
              <span>手机号</span>
              <el-input
                v-model="smsForm.mobile"
                :disabled="smsChallenge !== null"
                maxlength="11"
                inputmode="numeric"
                size="large"
                autocomplete="tel"
                placeholder="请输入11位手机号"
                @input="handleMobileInput"
              />
            </label>
            <label>
              <span>验证码</span>
              <div class="code-row">
                <el-input v-model="smsForm.code" maxlength="6" size="large" inputmode="numeric" autocomplete="one-time-code" />
                <el-button
                  type="primary"
                  size="large"
                  :loading="sendSmsFlight.pending.value"
                  @click="sendSmsFlight.run()"
                >{{ resendSeconds > 0 ? `${resendSeconds}s` : smsChallenge ? '重新发送' : '获取验证码' }}</el-button>
              </div>
            </label>
            <button v-if="smsChallenge" type="button" class="change-mobile" @click="resetSmsFlow">更换手机号</button>
            <el-checkbox v-model="smsForm.rememberMe">记住我</el-checkbox>
            <el-button native-type="submit" type="primary" size="large" :disabled="!smsCodeReady" :loading="smsLoginFlight.pending.value">登录</el-button>
          </form>
        </template>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-shell { min-height: 100vh; display: grid; grid-template-columns: 1.05fr .95fr; background: #f4f7f5; }
.brand-panel { padding: clamp(48px,8vw,110px); display: flex; flex-direction: column; justify-content: center; color: #effbfb; background: radial-gradient(circle at 15% 15%, rgba(130,224,218,.25), transparent 34%), #123f50; }
.eyebrow { margin: 0; color: #35b9b2; font-size: 11px; font-weight: 700; letter-spacing: .15em; }
.brand-panel h1 { max-width: 650px; margin: 12px 0 24px; font-size: clamp(38px,4.5vw,66px); line-height: 1.13; }
.brand-panel > p:last-child { max-width: 560px; color: rgba(239,251,251,.72); line-height: 1.8; }
.form-panel { padding: 28px; display: grid; place-items: center; }
.login-card { width: min(100%,470px); padding: 40px; display: grid; gap: 22px; border: 1px solid #dce5e1; border-radius: 24px; background: #fff; box-shadow: 0 28px 70px rgba(23,67,75,.1); }
.card-heading { display: grid; gap: 8px; }
.card-heading h2 { margin: 0; font-size: 30px; }
.card-heading span { color: #7a8884; font-size: 13px; line-height: 1.6; }
.mode-switch { padding: 4px; display: grid; grid-template-columns: 1fr 1fr; gap: 4px; border-radius: 10px; background: #f1f5f3; }
.mode-switch button { height: 38px; border: 0; border-radius: 8px; color: #667470; background: transparent; cursor: pointer; }
.mode-switch button.active { color: #123f50; font-weight: 700; background: #fff; box-shadow: 0 2px 10px rgba(18,63,80,.09); }
.login-form { display: grid; gap: 18px; }
label { display: grid; gap: 8px; font-size: 14px; font-weight: 600; }
.code-row { display: grid; grid-template-columns: 1fr 118px; gap: 10px; }
.change-mobile { justify-self: start; margin-top: -10px; padding: 0; border: 0; color: #268a86; background: transparent; cursor: pointer; }
.candidate-list { display: grid; gap: 10px; }
.candidate-card { width: 100%; padding: 14px; display: grid; grid-template-columns: 42px 1fr auto; align-items: center; gap: 12px; border: 1px solid #dfe8e4; border-radius: 12px; text-align: left; background: #fff; cursor: pointer; transition: border-color .18s, box-shadow .18s; }
.candidate-card:hover { border-color: #54bbb6; box-shadow: 0 8px 22px rgba(18,63,80,.08); }
.candidate-card:disabled { cursor: wait; opacity: .65; }
.candidate-avatar { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 11px; color: #11635f; font-weight: 700; background: #dff5f2; }
.candidate-main { min-width: 0; display: grid; gap: 3px; }
.candidate-main strong, .candidate-main small, .candidate-main em { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.candidate-main small { color: #65736f; }
.candidate-main em { color: #8b9793; font-size: 12px; font-style: normal; }
.candidate-recent { color: #64716e; font-size: 12px; white-space: nowrap; }
@media (max-width: 820px) { .login-shell { grid-template-columns: 1fr; } .brand-panel { min-height: 280px; padding: 48px 28px; } .login-card { padding: 28px; } .candidate-card { grid-template-columns: 42px 1fr; } .candidate-recent { grid-column: 2; } }
</style>
