<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { SmsRuntimePolicy } from '@canteen-smile/contracts'
import { reauthenticatePassword } from '@/modules/auth/api/authApi'
import { encryptPassword, PasswordEnvelopeError } from '@/modules/auth/passwordEnvelope'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'
import { getSmsSettings, updateSmsSettings } from '../api/smsDeliveryApi'

const loading = ref(false)
const currentPassword = ref('')
const reason = ref('')
const form = reactive({
  challengeTtlSeconds: 300,
  resendIntervalSeconds: 60,
  maxVerificationAttempts: 5,
  mobileHourlyLimit: 5,
  mobileDailyLimit: 10,
  ipHourlyLimit: 30,
  ipDailyLimit: 100,
  deviceHourlyLimit: 10,
  deviceDailyLimit: 30,
  version: 0,
  updatedTime: '' as string | null,
})

/** 将后端策略覆盖到页面编辑模型。 */
function assign(policy: SmsRuntimePolicy): void {
  Object.assign(form, policy)
}

/** 加载当前短信限流设置。 */
async function load(): Promise<void> {
  loading.value = true
  try { assign(await getSmsSettings()) } finally { loading.value = false }
}

const saveFlight = useSingleFlight(async () => {
  if (!currentPassword.value || !reason.value.trim()) {
    feedback.warning('请填写当前平台密码和修改原因')
    return
  }
  if (form.mobileDailyLimit < form.mobileHourlyLimit
    || form.ipDailyLimit < form.ipHourlyLimit
    || form.deviceDailyLimit < form.deviceHourlyLimit) {
    feedback.warning('每日限额不能低于对应的每小时限额')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(currentPassword.value, 'PLATFORM_REAUTH_PASSWORD')
    currentPassword.value = ''
    const reauth = await reauthenticatePassword({
      passwordEnvelope,
      allowedAction: 'PLATFORM_SMS_POLICY_UPDATE',
    })
    assign(await updateSmsSettings({
      challengeTtlSeconds: form.challengeTtlSeconds,
      resendIntervalSeconds: form.resendIntervalSeconds,
      maxVerificationAttempts: form.maxVerificationAttempts,
      mobileHourlyLimit: form.mobileHourlyLimit,
      mobileDailyLimit: form.mobileDailyLimit,
      ipHourlyLimit: form.ipHourlyLimit,
      ipDailyLimit: form.ipDailyLimit,
      deviceHourlyLimit: form.deviceHourlyLimit,
      deviceDailyLimit: form.deviceDailyLimit,
      version: form.version,
      reauthTicket: reauth.reauthTicket,
      reason: reason.value.trim(),
    }))
    reason.value = ''
    feedback.success('短信限流设置已生效')
  } catch (error) {
    if (error instanceof PasswordEnvelopeError) feedback.error(error.message)
  }
})

function formatTime(value: string | null): string {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '尚未修改'
}

onMounted(load)
</script>

<template>
  <div class="policy-page" v-loading="loading">
    <section class="page-heading">
      <p>平台治理 / 短信管理 / 短信设置</p>
      <h2>验证码与发送限流</h2>
      <span>所有修改立即影响后续验证码挑战，已发送验证码仍按创建时的失效时间处理。</span>
    </section>

    <section class="form-card">
      <div class="section-title"><strong>验证码规则</strong><span>错误次数最大不得超过 5 次</span></div>
      <div class="form-grid three">
        <label><span>有效时间（秒）</span><el-input-number v-model="form.challengeTtlSeconds" :min="60" :max="900" :step="30" /></label>
        <label><span>重发间隔（秒）</span><el-input-number v-model="form.resendIntervalSeconds" :min="30" :max="600" :step="10" /></label>
        <label><span>最大错误次数</span><el-input-number v-model="form.maxVerificationAttempts" :min="1" :max="5" /></label>
      </div>

      <div class="section-title"><strong>多维发送限流</strong><span>每日限额不能低于小时限额</span></div>
      <div class="form-grid rate-grid">
        <label><span>手机号 / 小时</span><el-input-number v-model="form.mobileHourlyLimit" :min="1" :max="100" /></label>
        <label><span>手机号 / 每日</span><el-input-number v-model="form.mobileDailyLimit" :min="1" :max="500" /></label>
        <label><span>IP / 小时</span><el-input-number v-model="form.ipHourlyLimit" :min="1" :max="1000" /></label>
        <label><span>IP / 每日</span><el-input-number v-model="form.ipDailyLimit" :min="1" :max="5000" /></label>
        <label><span>设备 / 小时</span><el-input-number v-model="form.deviceHourlyLimit" :min="1" :max="500" /></label>
        <label><span>设备 / 每日</span><el-input-number v-model="form.deviceDailyLimit" :min="1" :max="2000" /></label>
      </div>

      <div class="section-title"><strong>敏感操作确认</strong><span>最后更新：{{ formatTime(form.updatedTime) }}</span></div>
      <div class="form-grid confirm-grid">
        <label><span>当前平台密码</span><el-input v-model="currentPassword" type="password" show-password autocomplete="current-password" /></label>
        <label><span>修改原因</span><el-input v-model="reason" maxlength="500" show-word-limit placeholder="请输入本次调整原因" /></label>
      </div>
      <div class="actions">
        <el-button @click="load">重新加载</el-button>
        <el-button type="primary" :loading="saveFlight.pending.value" @click="saveFlight.run()">保存设置</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.policy-page { max-width: 1180px; margin: 0 auto; display: grid; gap: 18px; }
.page-heading p { margin: 0 0 10px; color: #6d48c4; font-size: 12px; font-weight: 800; letter-spacing: .1em; }
.page-heading h2 { margin: 0 0 8px; font-size: 32px; }
.page-heading span, .section-title span { color: #7e7785; }
.form-card { padding: 24px; display: grid; gap: 20px; border: 1px solid #e0e1dc; border-radius: 14px; background: #fff; }
.section-title { display: flex; justify-content: space-between; align-items: baseline; padding-bottom: 10px; border-bottom: 1px solid #ecece8; }
.form-grid { display: grid; gap: 18px; }
.three { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.rate-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.confirm-grid { grid-template-columns: minmax(220px, .7fr) minmax(320px, 1.3fr); }
label { display: grid; gap: 8px; color: #4f4955; font-size: 13px; }
label :deep(.el-input-number) { width: 100%; }
.actions { display: flex; justify-content: flex-end; gap: 10px; }
@media (max-width: 760px) { .three, .rate-grid, .confirm-grid { grid-template-columns: 1fr; } .page-heading h2 { font-size: 25px; } }
</style>
