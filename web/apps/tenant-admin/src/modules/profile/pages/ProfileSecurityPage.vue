<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import type { MobileBindingStatus, SmsChallenge } from '@canteen-smile/contracts'
import {
  confirmMobileBinding,
  createMobileBindingChallenge,
  getMobileBindingStatus,
} from '@/modules/auth/api/authApi'
import { getOrCreateDeviceId } from '@/modules/auth/device'
import { isMainlandMobile, normalizeMobileInput } from '@/modules/auth/mobile'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

const tenantContext = useTenantContextStore()
const loading = ref(false)
const binding = ref<MobileBindingStatus | null>(null)
const challenge = ref<SmsChallenge | null>(null)
const resendSeconds = ref(0)
const form = reactive({ mobile: '', code: '' })
let countdownTimer: number | undefined

const canSend = computed(() => isMainlandMobile(form.mobile) && resendSeconds.value === 0)
const canConfirm = computed(() => challenge.value !== null && /^[0-9]{6}$/.test(form.code))

/** 过滤非数字并限制大陆手机号长度。 */
function handleMobileInput(value: string): void {
  form.mobile = normalizeMobileInput(value)
}

/** 加载当前账号不泄露完整号码的手机号绑定状态。 */
async function load(): Promise<void> {
  loading.value = true
  try {
    binding.value = await getMobileBindingStatus()
  } finally {
    loading.value = false
  }
}

/** 根据服务端允许重发时间启动页面倒计时。 */
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

const sendFlight = useSingleFlight(async () => {
  if (!canSend.value) {
    feedback.warning(resendSeconds.value > 0 ? `请在 ${resendSeconds.value} 秒后重试` : '请输入正确的11位手机号')
    return
  }
  challenge.value = await createMobileBindingChallenge({
    mobile: form.mobile.trim(),
    deviceId: getOrCreateDeviceId(),
  })
  form.code = ''
  startCountdown(challenge.value.resendAt)
  feedback.success(`验证码已发送至 ${challenge.value.maskedMobile}`)
})

const confirmFlight = useSingleFlight(async () => {
  if (!challenge.value || !canConfirm.value) {
    feedback.warning('请输入六位短信验证码')
    return
  }
  binding.value = await confirmMobileBinding({
    mobile: form.mobile.trim(),
    challengeId: challenge.value.challengeId,
    code: form.code,
  })
  form.mobile = ''
  form.code = ''
  challenge.value = null
  resendSeconds.value = 0
  if (countdownTimer !== undefined) {
    window.clearInterval(countdownTimer)
    countdownTimer = undefined
  }
  feedback.success('手机号绑定成功')
})

/** 放弃当前挑战并允许重新输入手机号；服务端重发限流仍然有效。 */
function resetChallenge(): void {
  challenge.value = null
  form.code = ''
  resendSeconds.value = 0
  if (countdownTimer !== undefined) {
    window.clearInterval(countdownTimer)
    countdownTimer = undefined
  }
}

onMounted(load)
onUnmounted(() => {
  if (countdownTimer !== undefined) window.clearInterval(countdownTimer)
})
</script>

<template>
  <section class="profile-page" v-loading="loading">
    <header class="page-heading">
      <div>
        <p>账号中心 / 个人安全</p>
        <h2>保护当前账号的登录与恢复凭证。</h2>
        <span>手机号只有完成验证码校验后才会正式绑定，管理员不能代替本人直接确认。</span>
      </div>
    </header>

    <div class="security-grid">
      <article class="identity-card">
        <span class="card-label">当前身份</span>
        <strong>{{ tenantContext.displayName }}</strong>
        <small>{{ tenantContext.context?.username }}</small>
        <dl>
          <div><dt>租户</dt><dd>{{ tenantContext.context?.tenantName || '—' }}</dd></div>
          <div><dt>机构</dt><dd>{{ tenantContext.context?.organizationName || '—' }}</dd></div>
        </dl>
      </article>

      <article class="mobile-card">
        <div class="mobile-card__heading">
          <div>
            <span class="card-label">手机号凭证</span>
            <h3>{{ binding?.bound ? '已完成安全绑定' : '尚未绑定手机号' }}</h3>
          </div>
          <el-tag :type="binding?.bound ? 'success' : 'warning'" effect="light">
            {{ binding?.bound ? '已验证' : '待绑定' }}
          </el-tag>
        </div>

        <template v-if="binding?.bound">
          <div class="bound-mobile">
            <span>已验证手机号</span>
            <strong>{{ binding.maskedMobile }}</strong>
            <small>验证时间：{{ binding.verifiedTime ? new Date(binding.verifiedTime).toLocaleString('zh-CN', { hour12: false }) : '—' }}</small>
          </div>
          <el-alert
            type="success"
            :closable="false"
            title="该手机号可以用于后续短信登录和自助找回密码。"
          />
        </template>

        <template v-else>
          <el-alert
            type="info"
            :closable="false"
            title="同一个手机号可以绑定不同账号；使用短信登录时再选择具体账号。"
          />
          <div class="binding-form">
            <label>
              <span>手机号</span>
              <div class="mobile-code-row">
                <el-input
                  v-model="form.mobile"
                  maxlength="11"
                  inputmode="numeric"
                  autocomplete="tel"
                  placeholder="请输入11位手机号"
                  :disabled="challenge !== null"
                  @input="handleMobileInput"
                />
                <el-button
                  class="send-code-button"
                  type="primary"
                  :loading="sendFlight.pending.value"
                  @click="sendFlight.run()"
                >
                  {{ resendSeconds > 0 ? `${resendSeconds} 秒` : '发送验证码' }}
                </el-button>
              </div>
            </label>
            <label v-if="challenge">
              <span>短信验证码</span>
              <el-input
                v-model="form.code"
                maxlength="6"
                inputmode="numeric"
                autocomplete="one-time-code"
                placeholder="请输入六位验证码"
              />
              <small>验证码已发送至 {{ challenge.maskedMobile }}，并将在 {{ new Date(challenge.expiresAt).toLocaleTimeString('zh-CN', { hour12: false }) }} 失效。</small>
            </label>
          </div>
          <div v-if="challenge" class="form-actions">
            <el-button @click="resetChallenge">修改手机号</el-button>
            <el-button type="primary" :disabled="!canConfirm" :loading="confirmFlight.pending.value" @click="confirmFlight.run()">
              确认绑定
            </el-button>
          </div>
        </template>
      </article>
    </div>
  </section>
</template>

<style scoped>
.profile-page { padding: 40px 36px; }
.page-heading { display: flex; justify-content: space-between; gap: 24px; }
.page-heading p, .card-label { margin: 0 0 9px; color: #16768a; font-size: 11px; font-weight: 800; letter-spacing: .12em; }
.page-heading h2 { margin: 0 0 10px; font-size: clamp(30px, 3vw, 42px); }
.page-heading span { color: #758382; }
.security-grid { margin-top: 28px; display: grid; grid-template-columns: minmax(240px, .65fr) minmax(440px, 1.35fr); gap: 18px; align-items: start; }
.identity-card, .mobile-card { padding: 24px; border: 1px solid #dce5e1; border-radius: 16px; background: #fff; }
.identity-card { display: grid; gap: 7px; }
.identity-card > strong { font-size: 22px; }
.identity-card > small { color: #7d8988; }
.identity-card dl { margin: 18px 0 0; display: grid; gap: 12px; }
.identity-card dl div { padding-top: 12px; display: grid; gap: 5px; border-top: 1px solid #edf0ee; }
.identity-card dt { color: #879290; font-size: 12px; }
.identity-card dd { margin: 0; overflow-wrap: anywhere; }
.mobile-card { display: grid; gap: 20px; }
.mobile-card__heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.mobile-card__heading h3 { margin: 0; font-size: 22px; }
.bound-mobile { padding: 22px; display: grid; gap: 8px; border-radius: 13px; background: #eff9f5; }
.bound-mobile span, .bound-mobile small { color: #648078; }
.bound-mobile strong { color: #176b55; font-size: 28px; letter-spacing: .04em; }
.binding-form { display: grid; gap: 18px; }
.binding-form label { display: grid; gap: 8px; color: #3f4c4b; font-size: 13px; font-weight: 600; }
.binding-form small { color: #7f8a89; font-weight: 400; line-height: 1.5; }
.mobile-code-row { display: grid; grid-template-columns: minmax(0, 1fr) 112px; gap: 10px; }
.send-code-button { width: 112px; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; }
@media (max-width: 860px) { .profile-page { padding: 28px 20px; } .security-grid { grid-template-columns: 1fr; } }
@media (max-width: 520px) { .mobile-code-row { grid-template-columns: 1fr; } .send-code-button { width: 100%; } }
</style>
