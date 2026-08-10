<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePlatformSessionStore } from '@/app/store/platformSession'
import { reauthenticatePassword } from '@/modules/auth/api/authApi'
import { encryptPassword, PasswordEnvelopeError } from '@/modules/auth/passwordEnvelope'
import { createPlatformTenant, issueTenantOwnerActivationLink, issueTenantOwnerPasswordResetLink, listOrgTypeTemplates, pagePlatformTenants } from '@/modules/tenant/api/tenantApi'
import type { AccountStatus, CreateTenantRequest, OrgTypeTemplate, TenantStatus, TenantSummary } from '@/modules/tenant/types'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'
import { clearPlatformToken } from '@/shared/http/client'

const router = useRouter()
const platformSession = usePlatformSessionStore()
const session = computed(() => platformSession.session)
const tenants = ref<TenantSummary[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 20
const status = ref<TenantStatus | ''>('')
const loading = ref(false)
const wizardVisible = ref(false)
const wizardStep = ref(0)
const templates = ref<OrgTypeTemplate[]>([])
const creationKey = ref('')
const creation = ref<CreateTenantRequest>(defaultCreation())
const activationDialogVisible = ref(false)
const activationLink = ref('')
const activationExpiresAt = ref('')
const passwordResetDialogVisible = ref(false)
const passwordResetResultVisible = ref(false)
const passwordResetTenant = ref<TenantSummary | null>(null)
const passwordResetPassword = ref('')
const passwordResetReason = ref('')
const passwordResetLink = ref('')
const passwordResetExpiresAt = ref('')

const selectedTemplate = computed(() =>
  templates.value.find((item) => item.templateVersion === creation.value.templateVersion),
)

function defaultCreation(): CreateTenantRequest {
  return {
    tenantCode: '',
    name: '',
    templateVersion: 0,
    rootOrganization: { typeCode: '', businessCode: '', name: '' },
    owner: { username: '', displayName: '', employeeNumber: '' },
    securityPolicy: {
      concurrentLoginEnabled: true,
      maxDevices: 5,
      rememberMeEnabled: true,
      idleSeconds: 7200,
      absoluteSeconds: 604800,
      rememberIdleSeconds: 604800,
      rememberAbsoluteSeconds: 2592000,
      passwordExpiryEnabled: false,
      auditRetentionDays: 180,
    },
  }
}

const activeCount = computed(() => tenants.value.filter((item) => item.status === 'ACTIVE').length)
const provisioningCount = computed(
  () => tenants.value.filter((item) => item.provisionStatus === 'INITIALIZING').length,
)

async function loadTenants(): Promise<void> {
  loading.value = true
  try {
    const page = await pagePlatformTenants({
      pageNo: pageNo.value,
      pageSize,
      status: status.value || undefined,
    })
    tenants.value = page.items
    total.value = page.total
  } catch {
    // Axios 统一实例已经展示错误。
  } finally {
    loading.value = false
  }
}

async function initialize(): Promise<void> {
  try {
    await platformSession.load()
  } catch {
    platformSession.clear()
    clearPlatformToken()
    await router.replace({ name: 'login' })
    return
  }
  await loadTenants()
}

async function openWizard(): Promise<void> {
  try {
    templates.value = await listOrgTypeTemplates()
  } catch {
    return
  }
  if (templates.value.length === 0) {
    feedback.warning('请先发布至少一个机构类型模板版本')
    await router.push({ name: 'org-type-templates' })
    return
  }
  creation.value = defaultCreation()
  creation.value.templateVersion = templates.value[0]?.templateVersion ?? 0
  creation.value.rootOrganization.typeCode = templates.value[0]?.types[0]?.typeCode ?? ''
  creationKey.value = crypto.randomUUID()
  wizardStep.value = 0
  wizardVisible.value = true
}

function validateCurrentStep(): boolean {
  const value = creation.value
  if (wizardStep.value === 0 && (!value.tenantCode.trim() || !value.name.trim())) {
    feedback.warning('请填写租户编码和租户名称')
    return false
  }
  if (wizardStep.value === 1 && !value.templateVersion) {
    feedback.warning('请选择机构类型模板版本')
    return false
  }
  if (wizardStep.value === 2 && (!value.rootOrganization.typeCode || !value.rootOrganization.businessCode.trim() || !value.rootOrganization.name.trim())) {
    feedback.warning('请完整填写根机构信息')
    return false
  }
  if (wizardStep.value === 3 && !value.owner.username.trim()) {
    feedback.warning('请填写首位机构所有者用户名')
    return false
  }
  return true
}

function nextWizardStep(): void {
  if (validateCurrentStep()) wizardStep.value += 1
}

function selectTemplate(version: number): void {
  creation.value.templateVersion = version
  const template = templates.value.find((item) => item.templateVersion === version)
  creation.value.rootOrganization.typeCode = template?.types[0]?.typeCode ?? ''
}

const createTenantFlight = useSingleFlight(async () => {
  if (!validateCurrentStep()) return
  const request: CreateTenantRequest = {
    ...creation.value,
    tenantCode: creation.value.tenantCode.trim().toUpperCase(),
    name: creation.value.name.trim(),
    rootOrganization: {
      ...creation.value.rootOrganization,
      businessCode: creation.value.rootOrganization.businessCode.trim(),
      name: creation.value.rootOrganization.name.trim(),
    },
    owner: {
      username: creation.value.owner.username.trim(),
      displayName: creation.value.owner.displayName?.trim() || undefined,
      employeeNumber: creation.value.owner.employeeNumber?.trim() || undefined,
    },
    securityPolicy: {
      ...creation.value.securityPolicy,
      passwordExpiryDays: creation.value.securityPolicy.passwordExpiryEnabled
        ? creation.value.securityPolicy.passwordExpiryDays
        : undefined,
    },
  }
  try {
    const result = await createPlatformTenant(request, creationKey.value)
    feedback.success(`租户“${result.tenant.name}”创建成功，所有者账号等待激活`)
    wizardVisible.value = false
    await loadTenants()
  } catch {
    // 统一 Axios 实例已经反馈错误；保留幂等键和表单，允许安全重试。
  }
})

const activationFlight = useSingleFlight(async (tenantId: string) => {
  const result = await issueTenantOwnerActivationLink(tenantId)
  const tenantAdminBaseUrl = (import.meta.env.VITE_TENANT_ADMIN_BASE_URL || 'http://localhost:5174')
    .replace(/\/$/, '')
  activationLink.value = `${tenantAdminBaseUrl}/activate?ticket=${encodeURIComponent(result.activationTicket)}`
  activationExpiresAt.value = result.expiresAt
  activationDialogVisible.value = true
  feedback.success('新的所有者激活链接已生成，旧链接已失效')
})

async function copyActivationLink(): Promise<void> {
  try {
    await navigator.clipboard.writeText(activationLink.value)
    feedback.success('激活链接已复制')
  } catch {
    feedback.warning('浏览器未允许自动复制，请手动选择链接复制')
  }
}

/** 打开租户所有者敏感密码恢复操作。 */
function openPasswordReset(tenant: TenantSummary): void {
  passwordResetTenant.value = tenant
  passwordResetPassword.value = ''
  passwordResetReason.value = ''
  passwordResetDialogVisible.value = true
}

const passwordResetFlight = useSingleFlight(async () => {
  const tenant = passwordResetTenant.value
  if (!tenant || !passwordResetPassword.value || !passwordResetReason.value.trim()) {
    feedback.warning('请填写当前平台密码和操作原因')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(
      passwordResetPassword.value,
      'PLATFORM_REAUTH_PASSWORD',
    )
    passwordResetPassword.value = ''
    const reauth = await reauthenticatePassword({
      passwordEnvelope,
      allowedAction: 'TENANT_OWNER_PASSWORD_RESET',
    })
    const result = await issueTenantOwnerPasswordResetLink(tenant.id, {
      reauthTicket: reauth.reauthTicket,
      reason: passwordResetReason.value.trim(),
    })
    const tenantAdminBaseUrl = (import.meta.env.VITE_TENANT_ADMIN_BASE_URL || 'http://localhost:5174')
      .replace(/\/$/, '')
    passwordResetLink.value = `${tenantAdminBaseUrl}/reset-password?ticket=${encodeURIComponent(result.resetTicket)}`
    passwordResetExpiresAt.value = result.expiresAt
    passwordResetDialogVisible.value = false
    passwordResetResultVisible.value = true
    feedback.success('密码恢复链接已生成，所有旧设备会话已失效')
    await loadTenants()
  } catch (error) {
    if (error instanceof PasswordEnvelopeError) feedback.error(error.message)
  }
})

/** 复制只展示一次的密码恢复链接。 */
async function copyPasswordResetLink(): Promise<void> {
  try {
    await navigator.clipboard.writeText(passwordResetLink.value)
    feedback.success('密码恢复链接已复制')
  } catch {
    feedback.warning('浏览器未允许自动复制，请手动选择链接复制')
  }
}

function statusLabel(value: TenantStatus): string {
  return {
    INITIALIZING: '初始化中',
    ACTIVE: '正常',
    SUSPENDED: '已暂停',
    EXPIRED: '已到期',
    CANCELLED: '已注销',
  }[value]
}

function statusType(value: TenantStatus): 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'ACTIVE') return 'success'
  if (value === 'INITIALIZING') return 'warning'
  if (value === 'CANCELLED') return 'info'
  return 'danger'
}

function ownerStatusLabel(value: AccountStatus | null): string {
  if (value === 'PENDING_ACTIVATION') return '待激活'
  if (value === 'ACTIVE') return '已激活'
  if (value === 'PASSWORD_RESET_REQUIRED') return '待重置密码'
  if (value === 'DISABLED') return '已停用'
  if (value === 'CANCELLED') return '已注销'
  return '状态异常'
}

function ownerStatusType(value: AccountStatus | null): 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'ACTIVE') return 'success'
  if (value === 'PENDING_ACTIVATION' || value === 'PASSWORD_RESET_REQUIRED') return 'warning'
  if (value === 'DISABLED' || value === 'CANCELLED') return 'info'
  return 'danger'
}

function changePage(nextPage: number): void {
  pageNo.value = nextPage
  void loadTenants()
}

function changeStatus(): void {
  pageNo.value = 1
  void loadTenants()
}

function refreshTenantStatuses(): void {
  if (platformSession.session && !loading.value) void loadTenants()
}

onMounted(() => {
  window.addEventListener('focus', refreshTenantStatuses)
  void initialize()
})
onBeforeUnmount(() => window.removeEventListener('focus', refreshTenantStatuses))
</script>

<template>
  <div class="home-page">
      <section class="home-content">
        <div class="intro-row">
          <div>
            <p class="eyebrow">TENANT GOVERNANCE</p>
            <h2>跨租户只做治理，不进入租户业务。</h2>
            <p>当前数据由 IAM 实时分页查询，权限在 Gateway 与 IAM 服务双重校验。</p>
          </div>
          <el-button size="large" type="primary" @click="openWizard">创建租户</el-button>
        </div>

        <div class="metric-grid">
          <article><span>当前页租户</span><strong>{{ tenants.length }}</strong><small>总计 {{ total }}</small></article>
          <article><span>当前页正常</span><strong>{{ activeCount }}</strong><small>生命周期状态 ACTIVE</small></article>
          <article><span>初始化中</span><strong>{{ provisioningCount }}</strong><small>跨 Auth 编排状态</small></article>
          <article><span>当前会话</span><strong class="session-state">安全</strong><small>{{ session?.identityType }}</small></article>
        </div>

        <section class="tenant-panel">
          <div class="panel-heading">
            <div><h3>租户列表</h3><p>列表接口最大每页 100 条，当前每页 {{ pageSize }} 条。</p></div>
            <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px" @change="changeStatus">
              <el-option label="正常" value="ACTIVE" />
              <el-option label="初始化中" value="INITIALIZING" />
              <el-option label="已暂停" value="SUSPENDED" />
              <el-option label="已到期" value="EXPIRED" />
              <el-option label="已注销" value="CANCELLED" />
            </el-select>
          </div>

          <el-table v-loading="loading" :data="tenants" empty-text="暂无租户数据">
            <el-table-column label="租户" min-width="220">
              <template #default="scope">
                <div class="tenant-name">
                  <span>{{ scope.row.name.slice(0, 1) }}</span>
                  <div>
                    <strong>{{ scope.row.name }}</strong>
                    <small>{{ scope.row.tenantCode }}</small>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="租户所有者" min-width="180">
              <template #default="scope">
                <span class="owner-username">{{ scope.row.ownerUsername || '状态异常' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="scope"><el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="rootOrganizationId" label="根机构 ID" min-width="140" />
            <el-table-column prop="securityVersion" label="安全版本" width="110" />
            <el-table-column prop="templateVersion" label="模板版本" width="110" />
            <el-table-column label="创建时间" min-width="190">
              <template #default="scope">{{ new Date(scope.row.createdTime).toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="所有者安全" width="230" fixed="right">
              <template #default="scope">
                <el-button
                  v-if="scope.row.ownerAccountStatus === 'PENDING_ACTIVATION'"
                  type="primary"
                  link
                  :loading="activationFlight.pending.value"
                  :disabled="activationFlight.pending.value || scope.row.status !== 'ACTIVE'"
                  @click="activationFlight.run(scope.row.id)"
                >生成激活链接</el-button>
                <template v-else>
                  <el-tag :type="ownerStatusType(scope.row.ownerAccountStatus)">
                    {{ ownerStatusLabel(scope.row.ownerAccountStatus) }}
                  </el-tag>
                  <el-button
                    v-if="scope.row.ownerAccountStatus === 'ACTIVE' || scope.row.ownerAccountStatus === 'PASSWORD_RESET_REQUIRED'"
                    type="danger"
                    link
                    :disabled="passwordResetFlight.pending.value || scope.row.status !== 'ACTIVE'"
                    @click="openPasswordReset(scope.row)"
                  >{{ scope.row.ownerAccountStatus === 'ACTIVE' ? '找回密码' : '重发恢复链接' }}</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-row">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :current-page="pageNo"
              :page-size="pageSize"
              :total="total"
              @current-change="changePage"
            />
          </div>
        </section>
      </section>

    <el-dialog v-model="wizardVisible" title="创建租户" width="820px" :close-on-click-modal="false">
      <el-steps :active="wizardStep" finish-status="success" align-center>
        <el-step title="租户" />
        <el-step title="模板" />
        <el-step title="根机构" />
        <el-step title="所有者" />
        <el-step title="安全策略" />
      </el-steps>

      <div class="wizard-body">
        <section v-if="wizardStep === 0" class="wizard-form two-columns">
          <el-form-item label="租户编码">
            <el-input v-model="creation.tenantCode" maxlength="64" placeholder="全大写，如 HANGZHOU_EDU" />
          </el-form-item>
          <el-form-item label="租户名称">
            <el-input v-model="creation.name" maxlength="200" placeholder="客户或城市级租户名称" />
          </el-form-item>
          <el-alert class="full-column" type="info" :closable="false" title="租户编码全平台永久保留，创建后原则上不可修改。" />
        </section>

        <section v-else-if="wizardStep === 1" class="template-options">
          <button
            v-for="item in templates"
            :key="item.templateVersion"
            type="button"
            :class="{ selected: creation.templateVersion === item.templateVersion }"
            @click="selectTemplate(item.templateVersion)"
          >
            <strong>模板 v{{ item.templateVersion }}</strong>
            <span>{{ item.types.map((type) => type.name).join(' / ') }}</span>
            <small>{{ item.types.length }} 种类型 · {{ item.relations.length }} 条允许关系</small>
          </button>
        </section>

        <section v-else-if="wizardStep === 2" class="wizard-form two-columns">
          <el-form-item label="根机构类型">
            <el-select v-model="creation.rootOrganization.typeCode" style="width: 100%">
              <el-option v-for="item in selectedTemplate?.types" :key="item.typeCode" :label="`${item.name} · ${item.typeCode}`" :value="item.typeCode" />
            </el-select>
          </el-form-item>
          <el-form-item label="机构业务编码">
            <el-input v-model="creation.rootOrganization.businessCode" maxlength="64" placeholder="租户内永久唯一" />
          </el-form-item>
          <el-form-item class="full-column" label="根机构名称">
            <el-input v-model="creation.rootOrganization.name" maxlength="200" />
          </el-form-item>
          <el-alert class="full-column" type="info" :closable="false" title="行政区域是可选关联；区域字典接口落地后再在此提供选择，不以自由输入猜测 ID。" />
        </section>

        <section v-else-if="wizardStep === 3" class="wizard-form two-columns">
          <el-form-item label="所有者用户名">
            <el-input v-model="creation.owner.username" maxlength="128" placeholder="全平台唯一且永久保留" />
          </el-form-item>
          <el-form-item label="显示名称（可选）">
            <el-input v-model="creation.owner.displayName" maxlength="128" />
          </el-form-item>
          <el-form-item label="工号（可选）">
            <el-input v-model="creation.owner.employeeNumber" maxlength="64" placeholder="根机构内永久唯一" />
          </el-form-item>
          <el-alert class="full-column" type="warning" :closable="false" title="管理员不会设置或知道用户密码；本阶段创建待激活账号，激活链接由后续安全配置阶段发放。" />
        </section>

        <section v-else class="wizard-form two-columns">
          <el-form-item label="允许多设备登录"><el-switch v-model="creation.securityPolicy.concurrentLoginEnabled" /></el-form-item>
          <el-form-item label="最大设备数"><el-input-number v-model="creation.securityPolicy.maxDevices" :min="1" :max="100" /></el-form-item>
          <el-form-item label="允许记住我"><el-switch v-model="creation.securityPolicy.rememberMeEnabled" /></el-form-item>
          <el-form-item label="审计保留天数"><el-input-number v-model="creation.securityPolicy.auditRetentionDays" :min="180" /></el-form-item>
          <el-form-item label="普通空闲超时（秒）"><el-input-number v-model="creation.securityPolicy.idleSeconds" :min="60" /></el-form-item>
          <el-form-item label="普通最长存活（秒）"><el-input-number v-model="creation.securityPolicy.absoluteSeconds" :min="60" /></el-form-item>
          <el-form-item label="记住我空闲超时（秒）"><el-input-number v-model="creation.securityPolicy.rememberIdleSeconds" :min="60" /></el-form-item>
          <el-form-item label="记住我最长存活（秒）"><el-input-number v-model="creation.securityPolicy.rememberAbsoluteSeconds" :min="60" /></el-form-item>
          <el-form-item label="启用密码定期到期"><el-switch v-model="creation.securityPolicy.passwordExpiryEnabled" /></el-form-item>
          <el-form-item v-if="creation.securityPolicy.passwordExpiryEnabled" label="密码有效天数">
            <el-input-number v-model="creation.securityPolicy.passwordExpiryDays" :min="1" :max="3650" />
          </el-form-item>
        </section>
      </div>

      <template #footer>
        <el-button @click="wizardVisible = false">取消</el-button>
        <el-button v-if="wizardStep > 0" @click="wizardStep -= 1">上一步</el-button>
        <el-button v-if="wizardStep < 4" type="primary" @click="nextWizardStep">下一步</el-button>
        <el-button v-else type="primary" :loading="createTenantFlight.pending.value" :disabled="createTenantFlight.pending.value" @click="createTenantFlight.run()">
          创建并初始化
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="activationDialogVisible" title="租户所有者激活链接" width="680px">
      <el-alert
        type="warning"
        :closable="false"
        title="链接只在本次响应中展示；重新生成后旧链接立即失效，请通过可信渠道交付给本人。"
      />
      <el-input v-model="activationLink" class="activation-link-input" readonly type="textarea" :rows="3" />
      <p class="activation-expiry">有效期至：{{ new Date(activationExpiresAt).toLocaleString() }}</p>
      <template #footer>
        <el-button @click="activationDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="copyActivationLink">复制链接</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="passwordResetDialogVisible"
      title="租户所有者找回密码"
      width="620px"
      :close-on-click-modal="false"
    >
      <el-alert
        type="warning"
        :closable="false"
        title="提交后该账号全部设备立即下线，旧密码停止登录；恢复链接 30 分钟内仅可使用一次。"
      />
      <el-form class="password-reset-form" label-position="top">
        <el-form-item label="目标租户所有者">
          <el-input :model-value="`${passwordResetTenant?.name || ''} / ${passwordResetTenant?.ownerUsername || ''}`" disabled />
        </el-form-item>
        <el-form-item label="操作原因">
          <el-input v-model="passwordResetReason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="例如：所有者手机号已更换且忘记密码" />
        </el-form-item>
        <el-form-item label="当前平台账号密码（管理员再认证）">
          <el-input v-model="passwordResetPassword" type="password" show-password maxlength="128" autocomplete="current-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="passwordResetFlight.pending.value" @click="passwordResetDialogVisible = false">取消</el-button>
        <el-button
          type="danger"
          :loading="passwordResetFlight.pending.value"
          :disabled="passwordResetFlight.pending.value"
          @click="passwordResetFlight.run()"
        >确认并生成恢复链接</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordResetResultVisible" title="一次性密码恢复链接" width="680px">
      <el-alert
        type="warning"
        :closable="false"
        title="链接只在本次响应中展示；重新生成后旧链接立即失效，请通过可信线下渠道交付给本人。"
      />
      <el-input v-model="passwordResetLink" class="activation-link-input" readonly type="textarea" :rows="3" />
      <p class="activation-expiry">有效期至：{{ new Date(passwordResetExpiresAt).toLocaleString() }}</p>
      <template #footer>
        <el-button @click="passwordResetResultVisible = false">关闭</el-button>
        <el-button type="primary" @click="copyPasswordResetLink">复制链接</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.intro-row { display: flex; justify-content: space-between; align-items: flex-end; gap: 28px; }
.eyebrow { margin: 0 0 10px; color: #6d48c4; font-size: 11px; font-weight: 700; letter-spacing: .14em; }
.intro-row h2 { max-width: 700px; margin: 0; font-size: clamp(27px, 3vw, 42px); letter-spacing: -.035em; }
.intro-row > div > p:last-child { color: #77727c; }
.metric-grid { margin: 28px 0; display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.metric-grid article { padding: 22px; display: grid; gap: 8px; border: 1px solid #e1e2dd; border-radius: 16px; background: #fff; }
.metric-grid span, .metric-grid small { color: #85808a; }
.metric-grid strong { font-size: 29px; }
.metric-grid .session-state { color: #2e8c68; }
.tenant-panel { overflow: hidden; border: 1px solid #e0e1dc; border-radius: 18px; background: #fff; }
.panel-heading { padding: 22px 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #ecece8; }
.panel-heading h3 { margin: 0 0 5px; }
.panel-heading p { margin: 0; color: #8b8690; font-size: 12px; }
.tenant-name { display: flex; gap: 12px; align-items: center; }
.tenant-name > span { width: 34px; height: 34px; display: grid; place-items: center; color: #6041aa; border-radius: 10px; background: #eee9fb; }
.tenant-name div { display: grid; gap: 4px; }
.tenant-name small { color: #918b96; }
.owner-username { color: #3d3743; font-weight: 600; }
.pagination-row { padding: 20px 24px; display: flex; justify-content: flex-end; border-top: 1px solid #ecece8; }
.wizard-body { min-height: 330px; padding: 34px 8px 4px; }
.wizard-form { display: grid; gap: 2px 18px; }
.two-columns { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.full-column { grid-column: 1 / -1; }
.template-options { display: grid; gap: 12px; }
.template-options button { padding: 18px; display: grid; gap: 7px; text-align: left; color: #2b2731; border: 1px solid #dddde2; border-radius: 14px; background: #fff; cursor: pointer; }
.template-options button.selected { border-color: #6d48c4; box-shadow: 0 0 0 2px rgba(109,72,196,.12); background: #faf8ff; }
.template-options span, .template-options small { color: #827c87; }
.activation-link-input { margin-top: 18px; }
.activation-expiry { margin: 10px 0 0; color: #7b7580; font-size: 13px; }
.password-reset-form { margin-top: 18px; }
@media (max-width: 1040px) { .metric-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 760px) { .intro-row { align-items: flex-start; flex-direction: column; } .metric-grid { grid-template-columns: 1fr; } }
</style>
