<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, type Ref } from 'vue'
import type {
  MobileBindingStatus,
  MobileSecurityAction,
  SmsChallenge,
} from '@canteen-smile/contracts'
import { useRouter } from 'vue-router'
import {
  confirmMobileBinding,
  confirmMobileChange,
  confirmMobileUnbind,
  createCurrentMobileChallenge,
  createMobileBindingChallenge,
  createMobileChangeChallenge,
  getMobileBindingStatus,
  reauthenticatePassword,
  verifyCurrentMobile,
} from '@/modules/auth/api/authApi'
import { getOrCreateDeviceId } from '@/modules/auth/device'
import { isMainlandMobile, normalizeMobileInput } from '@/modules/auth/mobile'
import { encryptPassword, PasswordEnvelopeError } from '@/modules/auth/passwordEnvelope'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { clearTenantAdminToken } from '@/shared/http/client'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

type MobileOperation = 'change' | 'unbind'
type VerifyMethod = 'SMS' | 'PASSWORD'
type CountdownName = 'binding' | 'current' | 'next'

const router = useRouter()
const tenantContext = useTenantContextStore()
const loading = ref(false)
const binding = ref<MobileBindingStatus | null>(null)

const bindingChallenge = ref<SmsChallenge | null>(null)
const bindingResendSeconds = ref(0)
const bindingForm = reactive({ mobile: '', code: '' })

const securityVisible = ref(false)
const operation = ref<MobileOperation>('change')
const verifyMethod = ref<VerifyMethod>('SMS')
const currentChallenge = ref<SmsChallenge | null>(null)
const currentResendSeconds = ref(0)
const newChallenge = ref<SmsChallenge | null>(null)
const newResendSeconds = ref(0)
const reauthTicket = ref('')
const securityForm = reactive({ currentCode: '', currentPassword: '', newMobile: '', newCode: '' })

const timers: Record<CountdownName, number | undefined> = {
  binding: undefined,
  current: undefined,
  next: undefined,
}

const countdowns: Record<CountdownName, Ref<number>> = {
  binding: bindingResendSeconds,
  current: currentResendSeconds,
  next: newResendSeconds,
}

const canSendBinding = computed(
  () => isMainlandMobile(bindingForm.mobile) && bindingResendSeconds.value === 0,
)
const canConfirmBinding = computed(
  () => bindingChallenge.value !== null && /^[0-9]{6}$/.test(bindingForm.code),
)
const canVerifyCurrentCode = computed(
  () => currentChallenge.value !== null && /^[0-9]{6}$/.test(securityForm.currentCode),
)
const canVerifyPassword = computed(() => securityForm.currentPassword.length > 0)
const canSendNewMobile = computed(
  () => reauthTicket.value.length > 0
    && isMainlandMobile(securityForm.newMobile)
    && newResendSeconds.value === 0,
)
const canConfirmChange = computed(
  () => reauthTicket.value.length > 0
    && newChallenge.value !== null
    && isMainlandMobile(securityForm.newMobile)
    && /^[0-9]{6}$/.test(securityForm.newCode),
)
const operationAction = computed<MobileSecurityAction>(
  () => operation.value === 'change' ? 'MOBILE_CHANGE' : 'MOBILE_UNBIND',
)
const operationTitle = computed(() => operation.value === 'change' ? '更换手机号' : '解绑手机号')

/** 过滤非数字并限制大陆手机号长度。 */
function normalizeMobile(value: string, target: 'binding' | 'change'): void {
  const normalized = normalizeMobileInput(value)
  if (target === 'binding') bindingForm.mobile = normalized
  else securityForm.newMobile = normalized
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

/** 根据服务端允许重发时间维护不同业务步骤各自的倒计时。 */
function startCountdown(name: CountdownName, resendAt: string): void {
  stopCountdown(name)
  const refresh = (): void => {
    countdowns[name].value = Math.max(
      0,
      Math.ceil((new Date(resendAt).getTime() - Date.now()) / 1000),
    )
    if (countdowns[name].value === 0) stopCountdown(name)
  }
  refresh()
  if (countdowns[name].value > 0) timers[name] = window.setInterval(refresh, 1000)
}

/** 停止指定业务步骤倒计时并恢复可重发状态。 */
function stopCountdown(name: CountdownName): void {
  if (timers[name] !== undefined) window.clearInterval(timers[name])
  timers[name] = undefined
  countdowns[name].value = 0
}

const sendBindingFlight = useSingleFlight(async () => {
  if (!canSendBinding.value) {
    feedback.warning(
      bindingResendSeconds.value > 0
        ? `请在 ${bindingResendSeconds.value} 秒后重试`
        : '请输入正确的11位手机号',
    )
    return
  }
  bindingChallenge.value = await createMobileBindingChallenge({
    mobile: bindingForm.mobile.trim(),
    deviceId: getOrCreateDeviceId(),
  })
  bindingForm.code = ''
  startCountdown('binding', bindingChallenge.value.resendAt)
  feedback.success(`验证码已发送至 ${bindingChallenge.value.maskedMobile}`)
})

const confirmBindingFlight = useSingleFlight(async () => {
  if (!bindingChallenge.value || !canConfirmBinding.value) {
    feedback.warning('请输入六位短信验证码')
    return
  }
  binding.value = await confirmMobileBinding({
    mobile: bindingForm.mobile.trim(),
    challengeId: bindingChallenge.value.challengeId,
    code: bindingForm.code,
  })
  resetBindingChallenge()
  bindingForm.mobile = ''
  feedback.success('手机号绑定成功')
})

const sendCurrentFlight = useSingleFlight(async () => {
  if (currentResendSeconds.value > 0) {
    feedback.warning(`请在 ${currentResendSeconds.value} 秒后重试`)
    return
  }
  currentChallenge.value = await createCurrentMobileChallenge({
    deviceId: getOrCreateDeviceId(),
  })
  securityForm.currentCode = ''
  startCountdown('current', currentChallenge.value.resendAt)
  feedback.success(`验证码已发送至 ${currentChallenge.value.maskedMobile}`)
})

const verifyCurrentFlight = useSingleFlight(async () => {
  if (!currentChallenge.value || !canVerifyCurrentCode.value) {
    feedback.warning('请输入当前手机号收到的六位验证码')
    return
  }
  const result = await verifyCurrentMobile({
    challengeId: currentChallenge.value.challengeId,
    code: securityForm.currentCode,
    allowedAction: operationAction.value,
  })
  reauthTicket.value = result.reauthTicket
  securityForm.currentCode = ''
  feedback.success('当前手机号验证通过')
})

const verifyPasswordFlight = useSingleFlight(async () => {
  if (!canVerifyPassword.value) {
    feedback.warning('请输入当前登录密码')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(
      securityForm.currentPassword,
      'TENANT_REAUTH_PASSWORD',
    )
    const result = await reauthenticatePassword({
      passwordEnvelope,
      allowedAction: operationAction.value,
    })
    reauthTicket.value = result.reauthTicket
    securityForm.currentPassword = ''
    feedback.success('当前密码验证通过')
  } catch (error) {
    if (error instanceof PasswordEnvelopeError) feedback.error(error.message)
    else throw error
  }
})

const sendNewMobileFlight = useSingleFlight(async () => {
  if (!canSendNewMobile.value) {
    feedback.warning(
      newResendSeconds.value > 0
        ? `请在 ${newResendSeconds.value} 秒后重试`
        : '请输入正确的11位新手机号',
    )
    return
  }
  newChallenge.value = await createMobileChangeChallenge({
    reauthTicket: reauthTicket.value,
    mobile: securityForm.newMobile.trim(),
    deviceId: getOrCreateDeviceId(),
  })
  securityForm.newCode = ''
  startCountdown('next', newChallenge.value.resendAt)
  feedback.success(`验证码已发送至 ${newChallenge.value.maskedMobile}`)
})

const changeFlight = useSingleFlight(async () => {
  if (!newChallenge.value || !canConfirmChange.value) {
    feedback.warning('请完成新手机号验证码校验')
    return
  }
  await confirmMobileChange({
    reauthTicket: reauthTicket.value,
    newMobile: securityForm.newMobile.trim(),
    newChallengeId: newChallenge.value.challengeId,
    newCode: securityForm.newCode,
  })
  await finishSecurityOperation('手机号更换成功，所有设备已退出登录')
})

const unbindFlight = useSingleFlight(async () => {
  if (!reauthTicket.value) {
    feedback.warning('请先完成当前手机号或当前密码验证')
    return
  }
  await confirmMobileUnbind({ reauthTicket: reauthTicket.value })
  await finishSecurityOperation('手机号解绑成功，所有设备已退出登录')
})

/** 敏感手机号操作成功后清理本地会话并返回登录页。 */
async function finishSecurityOperation(message: string): Promise<void> {
  feedback.success(message)
  securityVisible.value = false
  clearTenantAdminToken()
  tenantContext.clear()
  await router.replace({ name: 'login' })
}

/** 打开换绑或解绑对话框并清理上一次未完成状态。 */
function openSecurityOperation(target: MobileOperation): void {
  resetSecurityDialog()
  operation.value = target
  securityVisible.value = true
}

/** 切换验证方式时丢弃另一种方式未完成的输入和挑战。 */
function selectVerifyMethod(method: VerifyMethod): void {
  verifyMethod.value = method
  securityForm.currentCode = ''
  securityForm.currentPassword = ''
  currentChallenge.value = null
  stopCountdown('current')
}

/** 放弃首次绑定挑战并允许重新输入手机号。 */
function resetBindingChallenge(): void {
  bindingChallenge.value = null
  bindingForm.code = ''
  stopCountdown('binding')
}

/** 清理换绑或解绑对话框内全部敏感临时状态。 */
function resetSecurityDialog(): void {
  verifyMethod.value = 'SMS'
  currentChallenge.value = null
  newChallenge.value = null
  reauthTicket.value = ''
  securityForm.currentCode = ''
  securityForm.currentPassword = ''
  securityForm.newMobile = ''
  securityForm.newCode = ''
  stopCountdown('current')
  stopCountdown('next')
}

onMounted(load)
onUnmounted(() => {
  stopCountdown('binding')
  stopCountdown('current')
  stopCountdown('next')
})
</script>

<template>
  <section class="profile-page" v-loading="loading">
    <header class="page-heading">
      <div>
        <p>账号中心 / 个人安全</p>
        <h2>保护当前账号的登录与恢复凭证。</h2>
        <span>手机号只有完成验证码校验后才会正式绑定；换绑和解绑都需要本人再次验证。</span>
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
            title="该手机号可以用于短信登录和自助找回密码。"
          />
          <div class="mobile-actions">
            <el-button type="primary" @click="openSecurityOperation('change')">更换手机号</el-button>
            <el-button type="danger" plain @click="openSecurityOperation('unbind')">解绑手机号</el-button>
          </div>
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
                  v-model="bindingForm.mobile"
                  maxlength="11"
                  inputmode="numeric"
                  autocomplete="tel"
                  placeholder="请输入11位手机号"
                  :disabled="bindingChallenge !== null"
                  @input="normalizeMobile($event, 'binding')"
                />
                <el-button
                  class="send-code-button"
                  type="primary"
                  :loading="sendBindingFlight.pending.value"
                  @click="sendBindingFlight.run()"
                >
                  {{ bindingResendSeconds > 0 ? `${bindingResendSeconds} 秒` : '发送验证码' }}
                </el-button>
              </div>
            </label>
            <label v-if="bindingChallenge">
              <span>短信验证码</span>
              <el-input
                v-model="bindingForm.code"
                maxlength="6"
                inputmode="numeric"
                autocomplete="one-time-code"
                placeholder="请输入六位验证码"
              />
              <small>验证码已发送至 {{ bindingChallenge.maskedMobile }}。</small>
            </label>
          </div>
          <div v-if="bindingChallenge" class="form-actions">
            <el-button @click="resetBindingChallenge">修改手机号</el-button>
            <el-button
              type="primary"
              :disabled="!canConfirmBinding"
              :loading="confirmBindingFlight.pending.value"
              @click="confirmBindingFlight.run()"
            >确认绑定</el-button>
          </div>
        </template>
      </article>
    </div>

    <el-dialog
      v-model="securityVisible"
      :title="operationTitle"
      width="min(560px, calc(100vw - 32px))"
      :close-on-click-modal="false"
      @closed="resetSecurityDialog"
    >
      <div class="security-dialog">
        <el-alert
          v-if="operation === 'change'"
          type="warning"
          :closable="false"
          title="更换成功后，旧手机号立即失效，当前账号所有设备都需要重新登录。"
        />
        <el-alert
          v-else
          type="error"
          :closable="false"
          title="解绑后将不能使用短信登录和手机号自助找回密码，所有设备都需要重新登录。"
        />

        <template v-if="!reauthTicket">
          <div class="dialog-section">
            <div class="section-heading">
              <strong>第一步：验证当前身份</strong>
              <small>验证结果仅允许本次{{ operationTitle }}，五分钟内有效且只能使用一次。</small>
            </div>
            <div class="verify-method-switch">
              <button
                type="button"
                :class="{ active: verifyMethod === 'SMS' }"
                @click="selectVerifyMethod('SMS')"
              >当前手机号</button>
              <button
                type="button"
                :class="{ active: verifyMethod === 'PASSWORD' }"
                @click="selectVerifyMethod('PASSWORD')"
              >当前密码</button>
            </div>

            <template v-if="verifyMethod === 'SMS'">
              <div class="current-mobile-row">
                <div>
                  <span>当前手机号</span>
                  <strong>{{ binding?.maskedMobile }}</strong>
                </div>
                <el-button
                  type="primary"
                  :loading="sendCurrentFlight.pending.value"
                  @click="sendCurrentFlight.run()"
                >{{ currentResendSeconds > 0 ? `${currentResendSeconds} 秒` : '发送验证码' }}</el-button>
              </div>
              <label v-if="currentChallenge" class="dialog-field">
                <span>当前手机号验证码</span>
                <el-input
                  v-model="securityForm.currentCode"
                  maxlength="6"
                  inputmode="numeric"
                  autocomplete="one-time-code"
                  placeholder="请输入六位验证码"
                />
              </label>
              <el-button
                v-if="currentChallenge"
                type="primary"
                :disabled="!canVerifyCurrentCode"
                :loading="verifyCurrentFlight.pending.value"
                @click="verifyCurrentFlight.run()"
              >验证并继续</el-button>
            </template>

            <template v-else>
              <label class="dialog-field">
                <span>当前登录密码</span>
                <el-input
                  v-model="securityForm.currentPassword"
                  type="password"
                  show-password
                  autocomplete="current-password"
                  placeholder="请输入当前登录密码"
                />
              </label>
              <el-button
                type="primary"
                :disabled="!canVerifyPassword"
                :loading="verifyPasswordFlight.pending.value"
                @click="verifyPasswordFlight.run()"
              >验证并继续</el-button>
              <small class="recovery-hint">原手机号无法接收短信时，可以使用当前密码；如果密码也遗忘，可由管理员先发起密码恢复。</small>
            </template>
          </div>
        </template>

        <template v-else-if="operation === 'change'">
          <div class="verified-banner"><span>✓</span><strong>当前身份验证通过</strong></div>
          <div class="dialog-section">
            <div class="section-heading">
              <strong>第二步：验证新手机号</strong>
              <small>新号码完成验证码校验后才会替代当前绑定。</small>
            </div>
            <label class="dialog-field">
              <span>新手机号</span>
              <div class="mobile-code-row">
                <el-input
                  v-model="securityForm.newMobile"
                  maxlength="11"
                  inputmode="numeric"
                  autocomplete="tel"
                  placeholder="请输入11位新手机号"
                  :disabled="newChallenge !== null"
                  @input="normalizeMobile($event, 'change')"
                />
                <el-button
                  class="send-code-button"
                  type="primary"
                  :loading="sendNewMobileFlight.pending.value"
                  @click="sendNewMobileFlight.run()"
                >{{ newResendSeconds > 0 ? `${newResendSeconds} 秒` : '发送验证码' }}</el-button>
              </div>
            </label>
            <label v-if="newChallenge" class="dialog-field">
              <span>新手机号验证码</span>
              <el-input
                v-model="securityForm.newCode"
                maxlength="6"
                inputmode="numeric"
                autocomplete="one-time-code"
                placeholder="请输入六位验证码"
              />
            </label>
          </div>
        </template>

        <template v-else>
          <div class="verified-banner"><span>✓</span><strong>当前身份验证通过</strong></div>
          <div class="unbind-confirmation">
            <strong>确定解绑 {{ binding?.maskedMobile }} 吗？</strong>
            <span>解绑不会删除账号，但短信登录和手机号找回密码将立即不可用。</span>
          </div>
        </template>
      </div>

      <template #footer>
        <el-button @click="securityVisible = false">取消</el-button>
        <el-button
          v-if="reauthTicket && operation === 'change'"
          type="primary"
          :disabled="!canConfirmChange"
          :loading="changeFlight.pending.value"
          @click="changeFlight.run()"
        >确认更换</el-button>
        <el-button
          v-if="reauthTicket && operation === 'unbind'"
          type="danger"
          :loading="unbindFlight.pending.value"
          @click="unbindFlight.run()"
        >确认解绑并退出所有设备</el-button>
      </template>
    </el-dialog>
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
.mobile-actions, .form-actions { display: flex; justify-content: flex-end; gap: 10px; }
.binding-form { display: grid; gap: 18px; }
.binding-form label, .dialog-field { display: grid; gap: 8px; color: #3f4c4b; font-size: 13px; font-weight: 600; }
.binding-form small { color: #7f8a89; font-weight: 400; line-height: 1.5; }
.mobile-code-row { display: grid; grid-template-columns: minmax(0, 1fr) 112px; gap: 10px; }
.send-code-button { width: 112px; }
.security-dialog { display: grid; gap: 18px; }
.dialog-section { display: grid; gap: 16px; }
.section-heading { display: grid; gap: 5px; }
.section-heading strong { color: #263b3d; font-size: 16px; }
.section-heading small, .recovery-hint { color: #7a8887; line-height: 1.6; }
.verify-method-switch { padding: 4px; display: grid; grid-template-columns: 1fr 1fr; gap: 4px; border-radius: 10px; background: #f0f4f3; }
.verify-method-switch button { height: 36px; border: 0; border-radius: 8px; color: #61706e; background: transparent; cursor: pointer; }
.verify-method-switch button.active { color: #116f6a; font-weight: 700; background: #fff; box-shadow: 0 2px 8px rgba(22, 69, 73, .08); }
.current-mobile-row { padding: 14px 16px; display: flex; align-items: center; justify-content: space-between; gap: 16px; border: 1px solid #e1e8e5; border-radius: 11px; }
.current-mobile-row div { display: grid; gap: 4px; }
.current-mobile-row span { color: #7c8886; font-size: 12px; }
.current-mobile-row strong { color: #1c4548; font-size: 17px; }
.verified-banner { padding: 12px 15px; display: flex; align-items: center; gap: 9px; border-radius: 10px; color: #176b55; background: #edf9f3; }
.verified-banner span { width: 24px; height: 24px; display: grid; place-items: center; border-radius: 50%; color: #fff; background: #38a477; }
.unbind-confirmation { padding: 18px; display: grid; gap: 7px; border: 1px solid #f1d3d3; border-radius: 12px; background: #fff8f8; }
.unbind-confirmation strong { color: #a22f37; font-size: 16px; }
.unbind-confirmation span { color: #7e686a; line-height: 1.6; }
@media (max-width: 860px) { .profile-page { padding: 28px 20px; } .security-grid { grid-template-columns: 1fr; } }
@media (max-width: 520px) { .mobile-code-row { grid-template-columns: 1fr; } .send-code-button { width: 100%; } .mobile-actions { justify-content: stretch; } .mobile-actions :deep(.el-button) { flex: 1; } }
</style>
