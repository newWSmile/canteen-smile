<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import type { SmsChallenge } from '@canteen-smile/contracts'
import { useRouter } from 'vue-router'
import {
  createSmsChallenge,
  selectSmsPasswordResetAccount,
  verifySmsPasswordReset,
} from '../api/authApi'
import type { MobileLoginCandidate, SmsPasswordResetResult } from '../types'
import { getOrCreateDeviceId } from '../device'
import { isMainlandMobile, normalizeMobileInput } from '../mobile'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

const router = useRouter()
const form = reactive({ mobile: '', code: '' })
const challenge = ref<SmsChallenge | null>(null)
const resendSeconds = ref(0)
const selectorTicket = ref('')
const accountCandidates = ref<MobileLoginCandidate[]>([])
let countdownTimer: number | undefined

const canSend = computed(
  () => isMainlandMobile(form.mobile) && resendSeconds.value === 0,
)
const canVerify = computed(
  () => challenge.value !== null && /^[0-9]{6}$/.test(form.code),
)

/** 过滤非数字字符并限制为大陆手机号长度。 */
function handleMobileInput(value: string): void {
  form.mobile = normalizeMobileInput(value)
}

/** 根据服务端允许重发时间维护当前用途独立倒计时。 */
function startCountdown(resendAt: string): void {
  if (countdownTimer !== undefined) window.clearInterval(countdownTimer)
  const refresh = (): void => {
    resendSeconds.value = Math.max(
      0,
      Math.ceil((new Date(resendAt).getTime() - Date.now()) / 1000),
    )
    if (resendSeconds.value === 0 && countdownTimer !== undefined) {
      window.clearInterval(countdownTimer)
      countdownTimer = undefined
    }
  }
  refresh()
  if (resendSeconds.value > 0) countdownTimer = window.setInterval(refresh, 1000)
}

/** 统一处理单账号重置或多账号选择结果。 */
async function handleRecoveryResult(result: SmsPasswordResetResult): Promise<void> {
  if (result.nextStep === 'RESET_PASSWORD' && result.passwordResetTicket) {
    feedback.success('身份验证成功，请设置新密码')
    await router.replace({
      name: 'reset-password',
      query: { ticket: result.passwordResetTicket },
    })
    return
  }
  if (
    result.nextStep === 'ACCOUNT_SELECTION_REQUIRED'
    && result.accountSelectorTicket
    && result.accountCandidates.length > 0
  ) {
    selectorTicket.value = result.accountSelectorTicket
    accountCandidates.value = result.accountCandidates
    feedback.success('手机号验证成功，请选择需要找回的账号')
    return
  }
  feedback.error('密码找回响应缺少有效票据或账号候选')
}

const sendFlight = useSingleFlight(async () => {
  if (!canSend.value) {
    feedback.warning(
      resendSeconds.value > 0
        ? `请在 ${resendSeconds.value} 秒后重试`
        : '请输入正确的11位手机号',
    )
    return
  }
  challenge.value = await createSmsChallenge({
    purpose: 'PASSWORD_RESET',
    mobile: form.mobile.trim(),
    deviceId: getOrCreateDeviceId(),
  })
  form.code = ''
  startCountdown(challenge.value.resendAt)
  feedback.success(`验证码已发送至 ${challenge.value.maskedMobile}`)
})

const verifyFlight = useSingleFlight(async () => {
  if (!challenge.value || !canVerify.value) {
    feedback.warning('请输入六位短信验证码')
    return
  }
  const result = await verifySmsPasswordReset({
    appCode: 'TENANT_ADMIN',
    challengeId: challenge.value.challengeId,
    code: form.code,
  })
  form.code = ''
  await handleRecoveryResult(result)
})

const selectionFlight = useSingleFlight(async (accountId: string) => {
  if (!selectorTicket.value) {
    feedback.warning('账号选择凭证已失效，请重新验证手机号')
    resetFlow()
    return
  }
  const result = await selectSmsPasswordResetAccount({
    appCode: 'TENANT_ADMIN',
    accountSelectorTicket: selectorTicket.value,
    accountId,
  })
  await handleRecoveryResult(result)
})

/** 放弃当前挑战和候选，返回手机号输入步骤。 */
function resetFlow(): void {
  challenge.value = null
  selectorTicket.value = ''
  accountCandidates.value = []
  form.code = ''
  resendSeconds.value = 0
  if (countdownTimer !== undefined) {
    window.clearInterval(countdownTimer)
    countdownTimer = undefined
  }
}

/** 展示最近一次登录时间，帮助用户区分同手机号账号。 */
function formatLatestLogin(value: string | null): string {
  if (!value) return '暂无登录记录'
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
  <main class="recovery-shell">
    <section class="recovery-card">
      <template v-if="accountCandidates.length > 0">
        <header class="card-heading">
          <p>手机号验证已通过</p>
          <h1>选择需要找回的账号</h1>
          <span>同一手机号绑定了多个账号，请根据租户和机构确认本次找回对象。</span>
        </header>
        <div class="candidate-list">
          <button
            v-for="candidate in accountCandidates"
            :key="candidate.accountId"
            type="button"
            class="candidate-card"
            :disabled="selectionFlight.pending.value"
            @click="selectionFlight.run(candidate.accountId)"
          >
            <span class="candidate-avatar">{{ (candidate.displayName || candidate.username).slice(0, 1) }}</span>
            <span class="candidate-main">
              <strong>{{ candidate.displayName || candidate.username }}</strong>
              <small>{{ candidate.username }} · {{ candidate.organizationName }}</small>
              <em>{{ candidate.tenantName }}</em>
            </span>
            <span class="candidate-recent">{{ formatLatestLogin(candidate.latestLoginTime) }}</span>
          </button>
        </div>
        <el-button text @click="resetFlow">返回手机号验证</el-button>
      </template>

      <template v-else>
        <header class="card-heading">
          <p>ACCOUNT RECOVERY</p>
          <h1>手机号找回密码</h1>
          <span>验证码仅用于本次密码找回，不会与短信登录或手机号绑定流程共用。</span>
        </header>
        <el-alert
          type="info"
          :closable="false"
          title="只有已完成手机号验证绑定的账号才能使用此方式找回。"
        />
        <form class="recovery-form" @submit.prevent="verifyFlight.run()">
          <label>
            <span>手机号</span>
            <el-input
              v-model="form.mobile"
              :disabled="challenge !== null"
              maxlength="11"
              inputmode="numeric"
              autocomplete="tel"
              size="large"
              placeholder="请输入11位手机号"
              @input="handleMobileInput"
            />
          </label>
          <label>
            <span>短信验证码</span>
            <div class="code-row">
              <el-input
                v-model="form.code"
                maxlength="6"
                inputmode="numeric"
                autocomplete="one-time-code"
                size="large"
                placeholder="请输入六位验证码"
              />
              <el-button
                native-type="button"
                type="primary"
                size="large"
                :loading="sendFlight.pending.value"
                @click="sendFlight.run()"
              >{{ resendSeconds > 0 ? `${resendSeconds}s` : challenge ? '重新发送' : '获取验证码' }}</el-button>
            </div>
          </label>
          <button v-if="challenge" type="button" class="change-mobile" @click="resetFlow">更换手机号</button>
          <el-button
            native-type="submit"
            type="primary"
            size="large"
            :disabled="!canVerify"
            :loading="verifyFlight.pending.value"
          >验证并继续</el-button>
        </form>
        <router-link class="back-login" :to="{ name: 'login' }">返回登录</router-link>
      </template>
    </section>
  </main>
</template>

<style scoped>
.recovery-shell { min-height: 100vh; padding: 32px 18px; display: grid; place-items: center; background: radial-gradient(circle at 18% 15%, #dff5f4, transparent 34%), #f4f7f5; }
.recovery-card { width: min(100%, 560px); padding: 42px; display: grid; gap: 22px; border: 1px solid #dce5e1; border-radius: 24px; background: rgba(255,255,255,.96); box-shadow: 0 28px 70px rgba(23,67,75,.1); }
.card-heading { display: grid; gap: 8px; }
.card-heading p { margin: 0; color: #258c87; font-size: 11px; font-weight: 800; letter-spacing: .14em; }
.card-heading h1 { margin: 0; color: #14333d; font-size: 31px; }
.card-heading span { color: #74817e; font-size: 13px; line-height: 1.7; }
.recovery-form { display: grid; gap: 18px; }
label { display: grid; gap: 8px; color: #354744; font-size: 14px; font-weight: 600; }
.code-row { display: grid; grid-template-columns: minmax(0, 1fr) 118px; gap: 10px; }
.change-mobile { justify-self: start; margin-top: -9px; padding: 0; border: 0; color: #268a86; background: transparent; cursor: pointer; }
.back-login { justify-self: center; color: #268a86; font-size: 13px; text-decoration: none; }
.candidate-list { display: grid; gap: 10px; }
.candidate-card { width: 100%; padding: 14px; display: grid; grid-template-columns: 42px minmax(0, 1fr) auto; align-items: center; gap: 12px; border: 1px solid #dfe8e4; border-radius: 12px; text-align: left; background: #fff; cursor: pointer; transition: border-color .18s, box-shadow .18s; }
.candidate-card:hover { border-color: #54bbb6; box-shadow: 0 8px 22px rgba(18,63,80,.08); }
.candidate-card:disabled { cursor: wait; opacity: .65; }
.candidate-avatar { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 11px; color: #11635f; font-weight: 700; background: #dff5f2; }
.candidate-main { min-width: 0; display: grid; gap: 3px; }
.candidate-main strong, .candidate-main small, .candidate-main em { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.candidate-main small { color: #65736f; }
.candidate-main em { color: #8b9793; font-size: 12px; font-style: normal; }
.candidate-recent { color: #64716e; font-size: 12px; white-space: nowrap; }
@media (max-width: 600px) { .recovery-card { padding: 28px 22px; } .candidate-card { grid-template-columns: 42px minmax(0,1fr); } .candidate-recent { grid-column: 2; } }
@media (max-width: 420px) { .code-row { grid-template-columns: 1fr; } }
</style>
