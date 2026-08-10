<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { reauthenticatePassword } from '@/modules/auth/api/authApi'
import { encryptPassword } from '@/modules/auth/passwordEnvelope'
import { pageRoles } from '@/modules/role/api/roleApi'
import type { Role } from '@/modules/role/types'
import { feedback } from '@/shared/feedback'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import {
  createTenantUser,
  cancelTenantUser,
  changeTenantUserStatus,
  issueTenantUserActivationLink,
  pageTenantUsers,
  replaceTenantUserRoles,
  updateTenantUser,
} from '../api/userApi'
import type { TenantUser, TenantUserStatus } from '../types'

const tenantContext = useTenantContextStore()
const loading = ref(false)
const users = ref<TenantUser[]>([])
const total = ref(0)
const pageNo = ref(1)
const keyword = ref('')
const status = ref<TenantUserStatus | ''>('')
const roleOptions = ref<Role[]>([])
const createVisible = ref(false)
const roleVisible = ref(false)
const activationVisible = ref(false)
const roleUser = ref<TenantUser | null>(null)
const activationLink = ref('')
const activationExpiresAt = ref('')
const editVisible = ref(false)
const editUser = ref<TenantUser | null>(null)
const operationVisible = ref(false)
const operationUser = ref<TenantUser | null>(null)
const operationAction = ref<'status' | 'cancel'>('status')
const operationReason = ref('')

const createForm = reactive({
  username: '',
  displayName: '',
  employeeNumber: '',
  roleIds: [] as string[],
  validityMode: 'LONG_TERM' as 'LONG_TERM' | 'FIXED_PERIOD',
  effectiveAt: null as Date | null,
  expiresAt: null as Date | null,
  reason: '',
  currentPassword: '',
})

const roleForm = reactive({
  roleIds: [] as string[],
  reason: '',
  currentPassword: '',
})

const editForm = reactive({
  displayName: '',
  employeeNumber: '',
  validityMode: 'LONG_TERM' as 'LONG_TERM' | 'FIXED_PERIOD',
  effectiveAt: null as Date | null,
  expiresAt: null as Date | null,
  reason: '',
})

const canCreate = computed(() => tenantContext.hasPermission('iam:user:create'))
const canAssignRole = computed(() => tenantContext.hasPermission('iam:user:role-assign'))
const canUpdate = computed(() => tenantContext.hasPermission('iam:user:update'))
const canStatus = computed(() => tenantContext.hasPermission('iam:user:status'))
const canCancel = computed(() => tenantContext.hasPermission('iam:user:cancel'))

const statusLabels: Record<TenantUserStatus, string> = {
  PENDING_ACTIVATION: '待激活',
  ACTIVE: '正常',
  PASSWORD_RESET_REQUIRED: '需重置密码',
  DISABLED: '已停用',
  CANCELLED: '已注销',
}

/** 加载当前机构真实用户分页和可分配角色。 */
async function load(): Promise<void> {
  loading.value = true
  try {
    const [page, rolePage] = await Promise.all([
      pageTenantUsers({
        pageNo: pageNo.value,
        pageSize: 20,
        keyword: keyword.value.trim() || undefined,
        status: status.value || undefined,
      }),
      pageRoles(1, 100),
    ])
    users.value = page.items
    total.value = page.total
    roleOptions.value = rolePage.items.filter((role) => role.roleType === 'CUSTOM' && role.status === 'ACTIVE')
  } finally {
    loading.value = false
  }
}

function search(): void {
  pageNo.value = 1
  void load()
}

function changePage(value: number): void {
  pageNo.value = value
  void load()
}

function openCreate(): void {
  Object.assign(createForm, {
    username: '', displayName: '', employeeNumber: '', roleIds: [], validityMode: 'LONG_TERM',
    effectiveAt: null, expiresAt: null, reason: '', currentPassword: '',
  })
  createVisible.value = true
}

/** 通过加密密码签发只能执行指定动作的一次性票据。 */
async function getReauthTicket(
  action: 'TENANT_USER_CREATE' | 'TENANT_USER_ROLE_ASSIGN',
  currentPassword: string,
): Promise<string> {
  const passwordEnvelope = await encryptPassword(currentPassword, 'TENANT_REAUTH_PASSWORD')
  const result = await reauthenticatePassword({ passwordEnvelope, allowedAction: action })
  return result.reauthTicket
}

const createFlight = useSingleFlight(async () => {
  if (!createForm.username.trim() || createForm.roleIds.length === 0) {
    feedback.warning('请填写用户名并至少分配一个角色')
    return
  }
  if (!createForm.reason.trim() || !createForm.currentPassword) {
    feedback.warning('创建用户属于敏感授权，请填写原因并输入当前密码')
    return
  }
  if (createForm.validityMode === 'FIXED_PERIOD'
    && (!createForm.effectiveAt || !createForm.expiresAt || createForm.effectiveAt >= createForm.expiresAt)) {
    feedback.warning('请选择合法的生效时间和到期时间')
    return
  }
  try {
    const reauthTicket = await getReauthTicket('TENANT_USER_CREATE', createForm.currentPassword)
    const fixed = createForm.validityMode === 'FIXED_PERIOD'
    const user = await createTenantUser({
      username: createForm.username.trim(),
      displayName: createForm.displayName.trim() || undefined,
      employeeNumber: createForm.employeeNumber.trim() || undefined,
      organizationId: tenantContext.context!.organizationId,
      roleIds: createForm.roleIds,
      validityMode: createForm.validityMode,
      effectiveAt: fixed ? createForm.effectiveAt!.toISOString() : undefined,
      expiresAt: fixed ? createForm.expiresAt!.toISOString() : undefined,
      reauthTicket,
      reason: createForm.reason.trim(),
    })
    feedback.success(`用户 ${user.username} 已创建，等待本人激活`)
    createVisible.value = false
    await load()
  } finally {
    createForm.currentPassword = ''
  }
})

function openRoles(user: TenantUser): void {
  roleUser.value = user
  Object.assign(roleForm, {
    roleIds: user.roles.map((role) => role.id),
    reason: '',
    currentPassword: '',
  })
  roleVisible.value = true
}

const roleFlight = useSingleFlight(async () => {
  if (!roleUser.value || roleForm.roleIds.length === 0) {
    feedback.warning('账号至少需要一个有效角色')
    return
  }
  if (!roleForm.reason.trim() || !roleForm.currentPassword) {
    feedback.warning('角色分配属于敏感授权，请填写原因并输入当前密码')
    return
  }
  try {
    const reauthTicket = await getReauthTicket('TENANT_USER_ROLE_ASSIGN', roleForm.currentPassword)
    await replaceTenantUserRoles(roleUser.value.id, {
      roleIds: roleForm.roleIds,
      reauthTicket,
      reason: roleForm.reason.trim(),
      version: roleUser.value.version,
    })
    feedback.success('用户角色已更新；授权版本已提升')
    roleVisible.value = false
    await load()
  } finally {
    roleForm.currentPassword = ''
  }
})

function openEdit(user: TenantUser): void {
  editUser.value = user
  Object.assign(editForm, {
    displayName: user.displayName || '',
    employeeNumber: user.employeeNumber || '',
    validityMode: user.validityMode,
    effectiveAt: user.effectiveAt ? new Date(user.effectiveAt) : null,
    expiresAt: user.expiresAt ? new Date(user.expiresAt) : null,
    reason: '',
  })
  editVisible.value = true
}

const editFlight = useSingleFlight(async () => {
  if (!editUser.value || !editForm.reason.trim()) {
    feedback.warning('请填写资料修改原因')
    return
  }
  if (editForm.validityMode === 'FIXED_PERIOD'
    && (!editForm.effectiveAt || !editForm.expiresAt || editForm.effectiveAt >= editForm.expiresAt)) {
    feedback.warning('请选择合法的生效时间和到期时间')
    return
  }
  const fixed = editForm.validityMode === 'FIXED_PERIOD'
  await updateTenantUser(editUser.value.id, {
    displayName: editForm.displayName.trim() || undefined,
    employeeNumber: editForm.employeeNumber.trim() || undefined,
    validityMode: editForm.validityMode,
    effectiveAt: fixed ? editForm.effectiveAt!.toISOString() : undefined,
    expiresAt: fixed ? editForm.expiresAt!.toISOString() : undefined,
    reason: editForm.reason.trim(),
    version: editUser.value.version,
  })
  feedback.success('用户资料已更新')
  editVisible.value = false
  await load()
})

function openOperation(user: TenantUser, action: 'status' | 'cancel'): void {
  operationUser.value = user
  operationAction.value = action
  operationReason.value = ''
  operationVisible.value = true
}

const operationFlight = useSingleFlight(async () => {
  if (!operationUser.value || !operationReason.value.trim()) {
    feedback.warning('请填写操作原因')
    return
  }
  if (operationAction.value === 'cancel') {
    await cancelTenantUser(operationUser.value, operationReason.value.trim())
    feedback.success('账号已注销；用户名、工号和历史 ID 永久保留')
  } else {
    const enable = operationUser.value.status === 'DISABLED'
    await changeTenantUserStatus(operationUser.value, operationReason.value.trim())
    feedback.success(enable ? '账号已恢复' : '账号已停用')
  }
  operationVisible.value = false
  await load()
})

const activationFlight = useSingleFlight(async (user: TenantUser) => {
  const result = await issueTenantUserActivationLink(user.id)
  const url = new URL('/activate', window.location.origin)
  url.searchParams.set('ticket', result.activationTicket)
  activationLink.value = url.toString()
  activationExpiresAt.value = result.expiresAt
  activationVisible.value = true
})

async function copyActivationLink(): Promise<void> {
  await navigator.clipboard.writeText(activationLink.value)
  feedback.success('激活链接已复制，请通过可信渠道交付给本人')
}

function statusTagType(value: TenantUserStatus): 'success' | 'warning' | 'info' | 'danger' {
  if (value === 'ACTIVE') return 'success'
  if (value === 'PENDING_ACTIVATION' || value === 'PASSWORD_RESET_REQUIRED') return 'warning'
  if (value === 'CANCELLED') return 'danger'
  return 'info'
}

function formatTime(value: string | null): string {
  return value ? new Date(value).toLocaleString() : '—'
}

onMounted(load)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="page-lead">
      <div>
        <p class="eyebrow">USER &amp; ACCESS</p>
        <h2>账号只属于当前机构，角色决定功能与数据边界。</h2>
        <p>管理员创建待激活账号但不知道最终密码；创建和角色分配均需要填写原因并用当前密码再认证。</p>
      </div>
      <el-button v-if="canCreate" type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <div class="filters">
      <el-input v-model="keyword" clearable placeholder="用户名前缀或显示名称" @keyup.enter="search" />
      <el-select v-model="status" clearable placeholder="全部状态">
        <el-option v-for="(label, value) in statusLabels" :key="value" :label="label" :value="value" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>

    <div class="panel">
      <el-table :data="users" empty-text="当前机构暂无用户">
        <el-table-column label="用户" min-width="220">
          <template #default="scope">
            <strong>{{ scope.row.displayName || scope.row.username }}</strong>
            <small>{{ scope.row.username }}<template v-if="scope.row.employeeNumber"> · 工号 {{ scope.row.employeeNumber }}</template></small>
          </template>
        </el-table-column>
        <el-table-column label="身份" width="110">
          <template #default="scope"><el-tag :type="scope.row.owner ? 'warning' : 'info'">{{ scope.row.owner ? '机构所有者' : '普通账号' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="角色" min-width="210">
          <template #default="scope">
            <div class="role-tags"><el-tag v-for="role in scope.row.roles" :key="role.id" size="small">{{ role.name }}</el-tag></div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope"><el-tag :type="statusTagType(scope.row.status)">{{ statusLabels[scope.row.status as TenantUserStatus] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="有效期" min-width="170">
          <template #default="scope"><span>{{ scope.row.validityMode === 'LONG_TERM' ? '长期有效' : formatTime(scope.row.expiresAt) }}</span></template>
        </el-table-column>
        <el-table-column prop="authzVersion" label="授权版本" width="100" />
        <el-table-column label="创建时间" min-width="170"><template #default="scope">{{ formatTime(scope.row.createdTime) }}</template></el-table-column>
        <el-table-column label="操作" min-width="330" fixed="right">
          <template #default="scope">
            <el-button v-if="canUpdate && !scope.row.owner && scope.row.status !== 'CANCELLED'" link @click="openEdit(scope.row)">编辑</el-button>
            <el-button v-if="canAssignRole && !scope.row.owner && scope.row.status !== 'CANCELLED'" link type="primary" @click="openRoles(scope.row)">分配角色</el-button>
            <el-button v-if="canCreate && scope.row.status === 'PENDING_ACTIVATION'" link type="success" :loading="activationFlight.pending.value" :disabled="activationFlight.pending.value" @click="activationFlight.run(scope.row)">生成激活链接</el-button>
            <el-button v-if="canStatus && !scope.row.owner && (scope.row.status === 'ACTIVE' || scope.row.status === 'DISABLED')" link :type="scope.row.status === 'ACTIVE' ? 'warning' : 'success'" @click="openOperation(scope.row, 'status')">{{ scope.row.status === 'ACTIVE' ? '停用' : '恢复' }}</el-button>
            <el-button v-if="canCancel && !scope.row.owner && scope.row.status !== 'CANCELLED'" link type="danger" @click="openOperation(scope.row, 'cancel')">注销</el-button>
            <span v-if="scope.row.owner" class="protected">所有者受保护</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination"><el-pagination background layout="prev, pager, next, total" :current-page="pageNo" :page-size="20" :total="total" @current-change="changePage" /></div>
    </div>

    <el-dialog v-model="createVisible" title="新增本机构用户" width="720px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" title="账号创建为待激活状态；最终密码由用户本人通过一次性激活链接设置。" />
      <el-form label-position="top" class="dialog-form">
        <div class="form-grid">
          <el-form-item label="用户名"><el-input v-model="createForm.username" maxlength="128" autocomplete="off" /></el-form-item>
          <el-form-item label="显示名称（可选）"><el-input v-model="createForm.displayName" maxlength="128" /></el-form-item>
          <el-form-item label="工号（本机构永久唯一，可选）"><el-input v-model="createForm.employeeNumber" maxlength="64" /></el-form-item>
          <el-form-item label="所属机构"><el-input :model-value="tenantContext.context?.organizationName" disabled /></el-form-item>
        </div>
        <el-form-item label="角色"><el-select v-model="createForm.roleIds" multiple style="width:100%"><el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" /></el-select></el-form-item>
        <el-form-item label="账号有效期"><el-radio-group v-model="createForm.validityMode"><el-radio value="LONG_TERM">长期有效</el-radio><el-radio value="FIXED_PERIOD">固定周期</el-radio></el-radio-group></el-form-item>
        <div v-if="createForm.validityMode === 'FIXED_PERIOD'" class="form-grid">
          <el-form-item label="生效时间"><el-date-picker v-model="createForm.effectiveAt" type="datetime" style="width:100%" /></el-form-item>
          <el-form-item label="到期时间"><el-date-picker v-model="createForm.expiresAt" type="datetime" style="width:100%" /></el-form-item>
        </div>
        <el-divider content-position="left">敏感授权再认证</el-divider>
        <el-form-item label="创建原因"><el-input v-model="createForm.reason" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item label="当前登录密码"><el-input v-model="createForm.currentPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible=false">取消</el-button><el-button type="primary" :loading="createFlight.pending.value" :disabled="createFlight.pending.value" @click="createFlight.run()">创建待激活账号</el-button></template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑用户资料" width="650px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" title="所属机构和用户名不在此处变更；工号一经使用将永久保留。" />
      <el-form label-position="top" class="dialog-form">
        <div class="form-grid">
          <el-form-item label="显示名称"><el-input v-model="editForm.displayName" maxlength="128" /></el-form-item>
          <el-form-item label="工号"><el-input v-model="editForm.employeeNumber" maxlength="64" /></el-form-item>
        </div>
        <el-form-item label="账号有效期"><el-radio-group v-model="editForm.validityMode"><el-radio value="LONG_TERM">长期有效</el-radio><el-radio value="FIXED_PERIOD">固定周期</el-radio></el-radio-group></el-form-item>
        <div v-if="editForm.validityMode === 'FIXED_PERIOD'" class="form-grid">
          <el-form-item label="生效时间"><el-date-picker v-model="editForm.effectiveAt" type="datetime" style="width:100%" /></el-form-item>
          <el-form-item label="到期时间"><el-date-picker v-model="editForm.expiresAt" type="datetime" style="width:100%" /></el-form-item>
        </div>
        <el-form-item label="修改原因"><el-input v-model="editForm.reason" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editVisible=false">取消</el-button><el-button type="primary" :loading="editFlight.pending.value" :disabled="editFlight.pending.value" @click="editFlight.run()">保存资料</el-button></template>
    </el-dialog>

    <el-dialog v-model="operationVisible" :title="operationAction === 'cancel' ? '注销账号' : (operationUser?.status === 'DISABLED' ? '恢复账号' : '停用账号')" width="540px" :close-on-click-modal="false">
      <el-alert v-if="operationAction === 'cancel'" type="error" :closable="false" title="注销不可恢复；用户名、工号和历史身份 ID 将永久保留，不能再次注册。" />
      <el-alert v-else-if="operationUser?.status === 'ACTIVE'" type="warning" :closable="false" title="停用后账号立即不再允许访问；Auth 全设备下线由下一阶段 Outbox 消费闭环执行。" />
      <el-form label-position="top" class="dialog-form"><el-form-item label="操作原因"><el-input v-model="operationReason" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-form>
      <template #footer><el-button @click="operationVisible=false">取消</el-button><el-button :type="operationAction === 'cancel' ? 'danger' : 'primary'" :loading="operationFlight.pending.value" :disabled="operationFlight.pending.value" @click="operationFlight.run()">确认</el-button></template>
    </el-dialog>

    <el-dialog v-model="roleVisible" title="分配用户角色" width="620px" :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" title="保存采用整版替换；只能分配当前管理员有权授予的角色，且账号至少保留一个角色。" />
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="目标用户"><el-input :model-value="roleUser?.username" disabled /></el-form-item>
        <el-form-item label="角色"><el-select v-model="roleForm.roleIds" multiple style="width:100%"><el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" /></el-select></el-form-item>
        <el-divider content-position="left">敏感授权再认证</el-divider>
        <el-form-item label="变更原因"><el-input v-model="roleForm.reason" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item label="当前登录密码"><el-input v-model="roleForm.currentPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="roleVisible=false">取消</el-button><el-button type="primary" :loading="roleFlight.pending.value" :disabled="roleFlight.pending.value" @click="roleFlight.run()">保存角色</el-button></template>
    </el-dialog>

    <el-dialog v-model="activationVisible" title="一次性激活链接" width="680px" :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" title="链接默认 24 小时有效且只能使用一次，请通过可信渠道线下交付给用户本人。" />
      <el-input class="activation-link" :model-value="activationLink" readonly type="textarea" :rows="3" />
      <p class="expiry">失效时间：{{ formatTime(activationExpiresAt) }}</p>
      <template #footer><el-button @click="activationVisible=false">关闭</el-button><el-button type="primary" @click="copyActivationLink">复制链接</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page{padding:36px}.page-lead{display:flex;justify-content:space-between;align-items:flex-end;gap:24px}.eyebrow{margin:0 0 10px;color:#168f89;font-size:11px;font-weight:700;letter-spacing:.14em}.page-lead h2{margin:0;font-size:34px}.page-lead p:last-child{color:#748281}.filters{margin:24px 0 14px;padding:16px;display:grid;grid-template-columns:minmax(240px,420px) 190px auto;gap:12px;border:1px solid #dce5e1;border-radius:14px;background:#fff}.panel{overflow:hidden;border:1px solid #dce5e1;border-radius:16px;background:#fff}.panel strong,.panel small{display:block}.panel small{margin-top:4px;color:#82908f}.role-tags{display:flex;flex-wrap:wrap;gap:6px}.pagination{padding:18px 22px;display:flex;justify-content:flex-end;border-top:1px solid #e8eeeb}.protected{color:#8b9795;font-size:13px}.dialog-form{margin-top:18px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 18px}.activation-link{margin-top:18px}.expiry{color:#778785}.el-select{width:100%}@media(max-width:760px){.page{padding:20px}.page-lead{align-items:flex-start;flex-direction:column}.filters,.form-grid{grid-template-columns:1fr}}
</style>
