<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { reauthenticatePassword } from '@/modules/auth/api/authApi'
import { encryptPassword, PasswordEnvelopeError } from '@/modules/auth/passwordEnvelope'
import { getTenantSecurityPolicy, updateTenantSecurityPolicy } from '../api/securityApi'
import type { TenantSecurityPolicy } from '../types'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

const tenantContext = useTenantContextStore()
const loading = ref(false)
const current = ref<TenantSecurityPolicy | null>(null)
const form = reactive({
  concurrentLoginEnabled: true,
  maxDevices: 5,
  rememberMeEnabled: true,
  idleSeconds: 7200,
  absoluteSeconds: 604800,
  rememberIdleSeconds: 604800,
  rememberAbsoluteSeconds: 2592000,
  passwordExpiryEnabled: false,
  passwordExpiryDays: null as number | null,
  auditRetentionDays: 180,
  currentPassword: '',
  reason: '',
})

const canManage = computed(
  () => tenantContext.hasPermission('iam:tenant-security:manage'),
)

/** 加载当前租户真实安全策略。 */
async function load(): Promise<void> {
  loading.value = true
  try {
    const policy = await getTenantSecurityPolicy()
    current.value = policy
    Object.assign(form, {
      concurrentLoginEnabled: policy.concurrentLoginEnabled,
      maxDevices: policy.maxDevices,
      rememberMeEnabled: policy.rememberMeEnabled,
      idleSeconds: policy.idleSeconds,
      absoluteSeconds: policy.absoluteSeconds,
      rememberIdleSeconds: policy.rememberIdleSeconds,
      rememberAbsoluteSeconds: policy.rememberAbsoluteSeconds,
      passwordExpiryEnabled: policy.passwordExpiryEnabled,
      passwordExpiryDays: policy.passwordExpiryDays,
      auditRetentionDays: policy.auditRetentionDays,
      currentPassword: '',
      reason: '',
    })
  } finally {
    loading.value = false
  }
}

/** 将秒数转换为便于核对的中文时长。 */
function durationText(seconds: number): string {
  if (seconds % 86400 === 0) return `${seconds / 86400} 天`
  if (seconds % 3600 === 0) return `${seconds / 3600} 小时`
  return `${Math.floor(seconds / 60)} 分钟`
}

const saveFlight = useSingleFlight(async () => {
  if (!current.value) return
  if (!canManage.value) {
    feedback.warning('当前账号没有修改租户安全策略的权限')
    return
  }
  if (!form.currentPassword || !form.reason.trim()) {
    feedback.warning('请填写修改原因并输入当前密码完成再认证')
    return
  }
  if (form.idleSeconds > form.absoluteSeconds
    || form.rememberIdleSeconds > form.rememberAbsoluteSeconds) {
    feedback.warning('会话空闲时长不能超过对应的最长存活时长')
    return
  }
  if (form.passwordExpiryEnabled && !form.passwordExpiryDays) {
    feedback.warning('启用密码到期时必须填写密码有效天数')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(form.currentPassword, 'TENANT_REAUTH_PASSWORD')
    const ticket = await reauthenticatePassword({
      passwordEnvelope,
      allowedAction: 'TENANT_SECURITY_POLICY_UPDATE',
    })
    current.value = await updateTenantSecurityPolicy({
      concurrentLoginEnabled: form.concurrentLoginEnabled,
      maxDevices: form.maxDevices,
      rememberMeEnabled: form.rememberMeEnabled,
      idleSeconds: form.idleSeconds,
      absoluteSeconds: form.absoluteSeconds,
      rememberIdleSeconds: form.rememberIdleSeconds,
      rememberAbsoluteSeconds: form.rememberAbsoluteSeconds,
      passwordExpiryEnabled: form.passwordExpiryEnabled,
      passwordExpiryDays: form.passwordExpiryEnabled ? form.passwordExpiryDays : null,
      auditRetentionDays: form.auditRetentionDays,
      version: current.value.version,
      reauthTicket: ticket.reauthTicket,
      reason: form.reason.trim(),
    })
    feedback.success('租户安全策略已保存；收紧策略时已有会话将通过安全事件失效')
    await load()
  } catch (error) {
    if (error instanceof PasswordEnvelopeError) feedback.error(error.message)
    else throw error
  }
})

onMounted(load)
</script>

<template>
  <section class="page" v-loading="loading">
    <header class="page-lead">
      <div>
        <p class="eyebrow">TENANT SECURITY POLICY</p>
        <h2>租户安全策略</h2>
        <p>统一控制并发设备、记住我、会话时长、密码到期和审计保留。</p>
      </div>
      <el-tag type="info">安全版本 {{ current?.securityVersion ?? '—' }}</el-tag>
    </header>

    <el-alert
      class="policy-alert"
      type="warning"
      :closable="false"
      title="收紧并发、会话或密码策略会提升租户安全版本，并通过 Outbox 使租户已有设备会话重新登录。"
    />

    <div class="policy-card">
      <section>
        <h3>设备与并发</h3>
        <div class="field-row"><div><strong>允许多设备并发登录</strong><small>关闭后新登录遵循单设备策略</small></div><el-switch v-model="form.concurrentLoginEnabled" /></div>
        <div class="field-row"><div><strong>最大有效设备数</strong><small>允许范围 1 至 100 台</small></div><el-input-number v-model="form.maxDevices" :min="1" :max="100" /></div>
        <div class="field-row"><div><strong>允许记住我</strong><small>租户可统一关闭登录页记住我能力</small></div><el-switch v-model="form.rememberMeEnabled" /></div>
      </section>

      <section>
        <h3>会话时长</h3>
        <div class="duration-grid">
          <el-form-item label="普通会话空闲秒数"><el-input-number v-model="form.idleSeconds" :min="60" /><small>{{ durationText(form.idleSeconds) }}</small></el-form-item>
          <el-form-item label="普通会话最长秒数"><el-input-number v-model="form.absoluteSeconds" :min="60" /><small>{{ durationText(form.absoluteSeconds) }}</small></el-form-item>
          <el-form-item label="记住我空闲秒数"><el-input-number v-model="form.rememberIdleSeconds" :min="60" /><small>{{ durationText(form.rememberIdleSeconds) }}</small></el-form-item>
          <el-form-item label="记住我最长秒数"><el-input-number v-model="form.rememberAbsoluteSeconds" :min="60" /><small>{{ durationText(form.rememberAbsoluteSeconds) }}</small></el-form-item>
        </div>
      </section>

      <section>
        <h3>密码与审计</h3>
        <div class="field-row"><div><strong>启用密码定期到期</strong><small>普通租户默认关闭</small></div><el-switch v-model="form.passwordExpiryEnabled" /></div>
        <div v-if="form.passwordExpiryEnabled" class="field-row"><div><strong>密码有效天数</strong><small>允许范围 1 至 3650 天</small></div><el-input-number v-model="form.passwordExpiryDays" :min="1" :max="3650" /></div>
        <div class="field-row"><div><strong>审计保留天数</strong><small>平台最低保留 180 天</small></div><el-input-number v-model="form.auditRetentionDays" :min="180" /></div>
      </section>

      <section v-if="canManage" class="reauth-section">
        <h3>敏感操作确认</h3>
        <div class="reauth-grid">
          <el-input v-model="form.currentPassword" type="password" show-password placeholder="当前登录密码" />
          <el-input v-model="form.reason" maxlength="500" show-word-limit placeholder="修改原因" />
        </div>
        <div class="actions">
          <el-button @click="load">重新加载</el-button>
          <el-button type="primary" :loading="saveFlight.pending.value" @click="saveFlight.run()">保存安全策略</el-button>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.page { padding: 36px; }
.page-lead { display: flex; justify-content: space-between; align-items: flex-end; gap: 24px; }
.eyebrow { margin: 0 0 10px; color: #168f89; font-size: 11px; font-weight: 700; letter-spacing: .14em; }
h2 { margin: 0; font-size: 34px; }.page-lead p:last-child, small { color: #748281; }
.policy-alert { margin: 22px 0 16px; }
.policy-card { overflow: hidden; border: 1px solid #dce5e1; border-radius: 16px; background: #fff; }
.policy-card section { padding: 24px; border-bottom: 1px solid #e8eeeb; }.policy-card section:last-child { border-bottom: 0; }
h3 { margin: 0 0 18px; }
.field-row { min-height: 64px; display: flex; align-items: center; justify-content: space-between; gap: 24px; border-top: 1px solid #edf1ef; }
.field-row div:first-child { display: grid; gap: 5px; }.field-row small { display: block; }
.duration-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 12px 24px; }
.duration-grid :deep(.el-form-item__content) { display: grid; grid-template-columns: minmax(180px,1fr) auto; gap: 10px; }
.duration-grid :deep(.el-input-number) { width: 100%; }
.reauth-grid { display: grid; grid-template-columns: 1fr 2fr; gap: 14px; }
.actions { margin-top: 18px; display: flex; justify-content: flex-end; gap: 10px; }
@media (max-width: 760px) { .page { padding: 20px; } .duration-grid, .reauth-grid { grid-template-columns: 1fr; } }
</style>
