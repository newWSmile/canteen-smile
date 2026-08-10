<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { SmsRuntimePolicy } from '@canteen-smile/contracts'
import { reauthenticatePassword } from '@/modules/auth/api/authApi'
import { encryptPassword, PasswordEnvelopeError } from '@/modules/auth/passwordEnvelope'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'
import { getSmsSecurity, updateSmsSecurity } from '../api/smsDeliveryApi'

const loading = ref(false)
const enabled = ref(false)
const persistedEnabled = ref(false)
const version = ref(0)
const updatedTime = ref<string | null>(null)
const riskAcknowledged = ref(false)
const currentPassword = ref('')
const reason = ref('')

/** 当前开关是否存在尚未保存的修改。 */
const hasChanged = computed(() => enabled.value !== persistedEnabled.value)

/** 只有从关闭切换为开启时才要求确认明文风险。 */
const requiresRiskAcknowledgement = computed(() => enabled.value && !persistedEnabled.value)

/** 当前已经持久化的安全状态文案。 */
const persistedStatusText = computed(() => persistedEnabled.value
  ? '验证码明文留存已开启'
  : '验证码明文留存已关闭')

function assign(policy: SmsRuntimePolicy): void {
  enabled.value = policy.plaintextCodeRetentionEnabled
  persistedEnabled.value = policy.plaintextCodeRetentionEnabled
  version.value = policy.version
  updatedTime.value = policy.updatedTime
  riskAcknowledged.value = false
}

async function load(): Promise<void> {
  loading.value = true
  try { assign(await getSmsSecurity()) } finally { loading.value = false }
}

const saveFlight = useSingleFlight(async () => {
  if (!hasChanged.value) {
    feedback.warning('短信安全设置没有发生变化')
    return
  }
  if (requiresRiskAcknowledgement.value && !riskAcknowledged.value) {
    feedback.warning('开启明文留存前必须确认已经知悉安全风险')
    return
  }
  if (!currentPassword.value || !reason.value.trim()) {
    feedback.warning('请填写当前平台密码和修改原因')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(currentPassword.value, 'PLATFORM_REAUTH_PASSWORD')
    currentPassword.value = ''
    const reauth = await reauthenticatePassword({
      passwordEnvelope,
      allowedAction: 'PLATFORM_SMS_POLICY_UPDATE',
    })
    assign(await updateSmsSecurity({
      plaintextCodeRetentionEnabled: enabled.value,
      version: version.value,
      reauthTicket: reauth.reauthTicket,
      reason: reason.value.trim(),
    }))
    reason.value = ''
    feedback.success(enabled.value ? '验证码明文留存已开启' : '验证码明文留存已关闭')
  } catch (error) {
    if (error instanceof PasswordEnvelopeError) feedback.error(error.message)
  }
})

/** 放弃当前未保存的开关修改。 */
function cancelChange(): void {
  enabled.value = persistedEnabled.value
  riskAcknowledged.value = false
  currentPassword.value = ''
  reason.value = ''
}

onMounted(load)
</script>

<template>
  <div class="security-page" v-loading="loading">
    <section class="page-heading">
      <p>平台治理 / 短信管理 / 短信安全</p>
      <h2>敏感正文留存策略</h2>
      <span>该开关只影响后续新投递记录，不回填历史短信，也不会让完整手机号进入数据库。</span>
    </section>

    <el-alert
      :title="persistedEnabled ? '当前正在保留验证码明文' : '验证码明文属于高敏感认证数据'"
      :description="persistedEnabled
        ? '短信列表可以直接看到开启后产生的验证码正文。应用日志仍会强制显示为 ******，建议完成联调或排障后及时关闭。'
        : '默认不会保存验证码明文。确有本地联调或排障需求时，可以临时开启；完整手机号和应用日志仍保持脱敏。'"
      :type="persistedEnabled ? 'error' : 'warning'"
      :closable="false"
      show-icon
    />

    <section class="security-card" :class="{ enabled: persistedEnabled }">
      <div class="status-row">
        <div class="status-icon" :class="{ enabled: persistedEnabled }">{{ persistedEnabled ? '!' : '✓' }}</div>
        <div class="status-copy">
          <div class="status-heading">
            <strong>保存验证码明文</strong>
            <el-tag :type="persistedEnabled ? 'danger' : 'success'" effect="light">
              {{ persistedEnabled ? '高风险 · 已开启' : '安全 · 已关闭' }}
            </el-tag>
          </div>
          <span>{{ persistedStatusText }}，该状态只影响后续新产生的短信记录。</span>
        </div>
        <div class="switch-control">
          <span>{{ hasChanged ? (enabled ? '待开启' : '待关闭') : (enabled ? '已开启' : '已关闭') }}</span>
          <el-switch v-model="enabled" size="large" />
        </div>
      </div>

      <div v-if="hasChanged" class="change-panel" :class="{ danger: enabled }">
        <div class="change-heading">
          <strong>{{ enabled ? '确认开启明文留存' : '确认关闭明文留存' }}</strong>
          <span>{{ enabled ? '保存后，新验证码将直接出现在短信列表正文中。' : '保存后，新验证码将恢复为 ******。' }}</span>
        </div>
        <el-checkbox v-if="requiresRiskAcknowledgement" v-model="riskAcknowledged">
          我已知悉具有短信列表权限的平台管理员可以看到后续验证码明文
        </el-checkbox>
        <div class="confirm-grid">
          <label><span>当前平台密码</span><el-input v-model="currentPassword" type="password" show-password autocomplete="current-password" placeholder="用于管理员再认证" /></label>
          <label><span>修改原因</span><el-input v-model="reason" maxlength="500" show-word-limit placeholder="例如：本地联调短信登录流程" /></label>
        </div>
        <div class="actions">
          <el-button @click="cancelChange">取消变更</el-button>
          <el-button :type="enabled ? 'danger' : 'primary'" :loading="saveFlight.pending.value" @click="saveFlight.run()">
            {{ enabled ? '确认开启' : '确认关闭' }}
          </el-button>
        </div>
      </div>
      <div v-else class="card-footer">
        <span class="meta">最后更新时间：{{ updatedTime ? new Date(updatedTime).toLocaleString('zh-CN', { hour12: false }) : '尚未修改' }}</span>
        <el-button text type="primary" @click="load">刷新状态</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.security-page { max-width: 980px; margin: 0 auto; display: grid; gap: 18px; }
.page-heading p { margin: 0 0 10px; color: #6d48c4; font-size: 12px; font-weight: 800; letter-spacing: .1em; }
.page-heading h2 { margin: 0 0 8px; font-size: 32px; }
.page-heading span, .meta { color: #7e7785; }
.security-card { padding: 24px; display: grid; gap: 22px; border: 1px solid #e0e1dc; border-radius: 14px; background: #fff; transition: border-color .2s, box-shadow .2s; }
.security-card.enabled { border-color: #f2b8b8; box-shadow: 0 10px 34px rgba(197,68,68,.08); }
.status-row { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; gap: 16px; align-items: center; }
.status-icon { width: 42px; height: 42px; display: grid; place-items: center; color: #168563; border-radius: 13px; background: #e8f7f1; font-size: 20px; font-weight: 800; }
.status-icon.enabled { color: #d64f4f; background: #fff0f0; }
.status-copy { display: grid; gap: 8px; }
.status-heading { display: flex; align-items: center; gap: 10px; }
.status-copy > span, .switch-control > span { color: #827b87; font-size: 13px; }
.switch-control { min-width: 112px; display: flex; justify-content: flex-end; align-items: center; gap: 10px; }
.change-panel { padding: 20px; display: grid; gap: 18px; border: 1px solid #b7d8ca; border-radius: 12px; background: #f4fbf8; }
.change-panel.danger { border-color: #f1c1c1; background: #fff7f7; }
.change-heading { display: grid; gap: 6px; }
.change-heading span { color: #756e79; font-size: 13px; }
.confirm-grid { display: grid; grid-template-columns: minmax(220px, .7fr) minmax(320px, 1.3fr); gap: 18px; }
label { display: grid; gap: 8px; color: #4f4955; font-size: 13px; }
.actions { display: flex; justify-content: flex-end; gap: 10px; }
.card-footer { padding-top: 16px; display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #ecece8; }
@media (max-width: 760px) { .status-row { grid-template-columns: auto 1fr; } .switch-control { grid-column: 1 / -1; justify-content: flex-start; } .confirm-grid { grid-template-columns: 1fr; } .page-heading h2 { font-size: 25px; } }
</style>
