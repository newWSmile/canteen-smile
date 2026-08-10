<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { searchOrganizations } from '@/modules/organization/api/organizationApi'
import type { OrganizationSearchResult } from '@/modules/organization/types'
import {
  changeRoleStatus,
  createRole,
  deleteRole,
  getGrantBoundary,
  getRoleDataPolicies,
  getRolePermissions,
  pageRoles,
  replaceRoleDataPolicies,
  replaceRolePermissions,
  updateRole,
} from '../api/roleApi'
import type { DataScopeType, GrantBoundary, Role, RoleDataPolicy, RolePermission } from '../types'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

interface PermissionTreeNode extends RolePermission { children: PermissionTreeNode[] }

interface PermissionCheckState { checkedKeys: Array<string | number> }

const tenantContext = useTenantContextStore()
const loading = ref(false)
const roles = ref<Role[]>([])
const total = ref(0)
const pageNo = ref(1)
const boundary = ref<GrantBoundary | null>(null)
const editVisible = ref(false)
const editingRole = ref<Role | null>(null)
const permissionVisible = ref(false)
const permissionRole = ref<Role | null>(null)
const permissionTree = ref<PermissionTreeNode[]>([])
const permissionParentIds = ref<Map<string, string | null>>(new Map())
const permissionTreeRef = ref<{
  getCheckedKeys: () => Array<string | number>
  setCheckedKeys: (keys: string[]) => void
  setChecked: (key: string, checked: boolean, deep: boolean) => void
} | null>(null)
let syncingPermissionTree = false
const permissionReason = ref('')
const dataVisible = ref(false)
const dataRole = ref<Role | null>(null)
const dataPolicies = ref<RoleDataPolicy[]>([])
const dataReason = ref('')
const operationVisible = ref(false)
const operationRole = ref<Role | null>(null)
const operationAction = ref<'status' | 'delete'>('status')
const operationReason = ref('')
const organizationOptions = ref<OrganizationSearchResult[]>([])
const organizationLoading = ref(false)

const form = reactive({
  name: '', description: '', defaultScopeType: 'CURRENT_ORG' as DataScopeType,
  specifiedOrganizationIds: [] as string[],
})

const canCreate = computed(() => tenantContext.hasPermission('iam:role:create'))
const canUpdate = computed(() => tenantContext.hasPermission('iam:role:update'))
const canStatus = computed(() => tenantContext.hasPermission('iam:role:status'))
const canDelete = computed(() => tenantContext.hasPermission('iam:role:delete'))
const canGrant = computed(() => tenantContext.hasPermission('iam:role:grant'))
const canDataScope = computed(() => tenantContext.hasPermission('iam:role:data-scope'))
const specifiedScope = computed(() => form.defaultScopeType.startsWith('SPECIFIED_'))

const scopeLabels: Record<DataScopeType, string> = {
  SELF: '本人数据', CURRENT_ORG: '本机构', CURRENT_ORG_AND_DESCENDANTS: '本机构及下级',
  SPECIFIED_ORGS: '指定机构', SPECIFIED_ORGS_AND_DESCENDANTS: '指定机构及其下级', TENANT_ALL: '租户全部',
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [page, grantBoundary] = await Promise.all([pageRoles(pageNo.value), getGrantBoundary()])
    roles.value = page.items
    total.value = page.total
    boundary.value = grantBoundary
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  editingRole.value = null
  Object.assign(form, { name: '', description: '', defaultScopeType: 'CURRENT_ORG', specifiedOrganizationIds: [] })
  editVisible.value = true
}

function openEdit(role: Role): void {
  editingRole.value = role
  Object.assign(form, { name: role.name, description: role.description || '', defaultScopeType: role.defaultScopeType, specifiedOrganizationIds: [] })
  editVisible.value = true
}

const editFlight = useSingleFlight(async () => {
  if (!form.name.trim()) {
    feedback.warning('请填写角色名称')
    return
  }
  if (specifiedScope.value && form.specifiedOrganizationIds.length === 0) {
    feedback.warning('请选择至少一个机构')
    return
  }
  if (editingRole.value) {
    await updateRole(editingRole.value, { name: form.name.trim(), description: form.description.trim() || undefined })
    feedback.success('角色资料已更新')
  } else {
    await createRole({
      name: form.name.trim(), description: form.description.trim() || undefined,
      defaultScopeType: form.defaultScopeType,
      specifiedOrganizationIds: specifiedScope.value ? form.specifiedOrganizationIds : [],
    })
    feedback.success('角色已创建')
  }
  editVisible.value = false
  await load()
})

async function openPermissions(role: Role): Promise<void> {
  permissionRole.value = role
  permissionReason.value = ''
  const nodes = await getRolePermissions(role.id)
  permissionTree.value = buildPermissionTree(nodes)
  permissionParentIds.value = new Map(nodes.map((node) => [node.id, node.parentId]))
  permissionVisible.value = true
  await nextTick()
  permissionTreeRef.value?.setCheckedKeys(nodes.filter((node) => node.granted).map((node) => node.id))
}

function setDescendantsChecked(nodes: PermissionTreeNode[], checked: boolean): void {
  for (const node of nodes) {
    permissionTreeRef.value?.setChecked(node.id, checked, false)
    setDescendantsChecked(node.children, checked)
  }
}

function setAncestorsChecked(parentId: string | null): void {
  let currentParentId = parentId
  while (currentParentId) {
    permissionTreeRef.value?.setChecked(currentParentId, true, false)
    currentParentId = permissionParentIds.value.get(currentParentId) ?? null
  }
}

function handlePermissionCheck(node: PermissionTreeNode, state: PermissionCheckState): void {
  if (syncingPermissionTree) return
  const checked = state.checkedKeys.some((key) => String(key) === node.id)
  syncingPermissionTree = true
  try {
    if (node.children.length > 0) {
      setDescendantsChecked(node.children, checked)
    } else if (checked) {
      setAncestorsChecked(node.parentId)
    }
  } finally {
    syncingPermissionTree = false
  }
}

const permissionFlight = useSingleFlight(async () => {
  if (!permissionRole.value) return
  if (!permissionReason.value.trim()) {
    feedback.warning('请填写授权原因')
    return
  }
  const checkedIds = permissionTreeRef.value?.getCheckedKeys().map(String) ?? []
  await replaceRolePermissions(permissionRole.value, checkedIds, permissionReason.value.trim())
  feedback.success('角色功能权限已保存，受影响账号授权版本已提升')
  permissionVisible.value = false
  await load()
})

async function openDataPolicy(role: Role): Promise<void> {
  dataRole.value = role
  dataReason.value = ''
  dataPolicies.value = await getRoleDataPolicies(role.id)
  dataVisible.value = true
}

const dataFlight = useSingleFlight(async () => {
  if (!dataRole.value) return
  if (!dataReason.value.trim()) {
    feedback.warning('请填写数据范围变更原因')
    return
  }
  const invalidSpecified = dataPolicies.value.some(
    (policy) => policy.scopeType.startsWith('SPECIFIED_') && policy.organizationIds.length === 0,
  )
  if (invalidSpecified) {
    feedback.warning('指定机构范围至少需要选择一个机构')
    return
  }
  await replaceRoleDataPolicies(dataRole.value, dataPolicies.value, dataReason.value.trim())
  feedback.success('角色数据范围已保存，受影响账号授权版本已提升')
  dataVisible.value = false
  await load()
})

function openOperation(role: Role, action: 'status' | 'delete'): void {
  operationRole.value = role
  operationAction.value = action
  operationReason.value = ''
  operationVisible.value = true
}

const operationFlight = useSingleFlight(async () => {
  if (!operationRole.value || !operationReason.value.trim()) {
    feedback.warning('请填写操作原因')
    return
  }
  if (operationAction.value === 'delete') {
    await deleteRole(operationRole.value, operationReason.value.trim())
    feedback.success('角色已删除，角色编码和历史 ID 永久保留')
  } else {
    const wasActive = operationRole.value.status === 'ACTIVE'
    await changeRoleStatus(operationRole.value, operationReason.value.trim())
    feedback.success(wasActive ? '角色已停用' : '角色已恢复')
  }
  operationVisible.value = false
  await load()
})

async function remoteOrganizations(keyword: string): Promise<void> {
  if (!keyword.trim()) return
  organizationLoading.value = true
  try {
    organizationOptions.value = await searchOrganizations(keyword.trim())
  } finally {
    organizationLoading.value = false
  }
}

function buildPermissionTree(nodes: RolePermission[]): PermissionTreeNode[] {
  const map = new Map(nodes.map((node) => [node.id, { ...node, children: [] as PermissionTreeNode[] }]))
  const roots: PermissionTreeNode[] = []
  for (const node of map.values()) {
    const parent = node.parentId ? map.get(node.parentId) : undefined
    if (parent) parent.children.push(node)
    else roots.push(node)
  }
  return roots
}

function changePage(value: number): void {
  pageNo.value = value
  void load()
}

onMounted(load)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="page-lead">
      <div><p class="eyebrow">ROLE &amp; AUTHORIZATION</p><h2>角色只管理本机构，权限不能超过操作者上限。</h2><p>多角色取并集；所有者角色由系统保护，停用、授权缩小和数据范围变化都会提升受影响账号授权版本。</p></div>
      <el-button v-if="canCreate" class="management-primary-action" type="primary" @click="openCreate">新增角色</el-button>
    </div>

    <div class="boundary-card">
      <span>当前授权边界</span><strong>{{ boundary?.rootOwner ? '根机构所有者' : '普通授权管理员' }}</strong>
      <small>机构 ID {{ boundary?.organizationId }} · 可授予 {{ boundary?.permissionIds.length || 0 }} 项已发布权限</small>
    </div>

    <div class="panel">
      <el-table :data="roles" empty-text="暂无角色">
        <el-table-column label="角色" min-width="230"><template #default="scope"><strong>{{ scope.row.name }}</strong><small>{{ scope.row.roleCode }}</small></template></el-table-column>
        <el-table-column label="类型" width="110"><template #default="scope"><el-tag :type="scope.row.roleType === 'OWNER' ? 'warning' : 'info'">{{ scope.row.roleType === 'OWNER' ? '所有者' : '自定义' }}</el-tag></template></el-table-column>
        <el-table-column label="默认数据范围" min-width="160"><template #default="scope">{{ scopeLabels[scope.row.defaultScopeType as DataScopeType] }}</template></el-table-column>
        <el-table-column prop="accountCount" label="关联账号" width="100" />
        <el-table-column label="状态" width="100"><template #default="scope"><el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">{{ scope.row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" min-width="330" fixed="right">
          <template #default="scope">
            <template v-if="scope.row.roleType === 'CUSTOM'">
              <el-button v-if="canUpdate" link @click="openEdit(scope.row)">编辑</el-button>
              <el-button v-if="canGrant" type="primary" link @click="openPermissions(scope.row)">功能权限</el-button>
              <el-button v-if="canDataScope" type="primary" link @click="openDataPolicy(scope.row)">数据范围</el-button>
              <el-button v-if="canStatus" :type="scope.row.status === 'ACTIVE' ? 'warning' : 'success'" link @click="openOperation(scope.row, 'status')">{{ scope.row.status === 'ACTIVE' ? '停用' : '恢复' }}</el-button>
              <el-button v-if="canDelete" type="danger" link :disabled="scope.row.accountCount > 0" @click="openOperation(scope.row, 'delete')">删除</el-button>
            </template>
            <span v-else class="protected">系统保护，不可修改</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination"><el-pagination background layout="prev, pager, next, total" :current-page="pageNo" :page-size="20" :total="total" @current-change="changePage" /></div>
    </div>

    <el-dialog v-model="editVisible" :title="editingRole ? '编辑角色' : '新增角色'" width="620px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="角色名称"><el-input v-model="form.name" maxlength="128" /></el-form-item>
        <el-form-item label="角色说明"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
        <template v-if="!editingRole">
          <el-form-item label="默认数据范围"><el-select v-model="form.defaultScopeType" style="width:100%"><el-option v-for="scope in boundary?.scopeTypes || []" :key="scope" :label="scopeLabels[scope]" :value="scope" /></el-select></el-form-item>
          <el-form-item v-if="specifiedScope" label="指定机构"><el-select v-model="form.specifiedOrganizationIds" multiple filterable remote reserve-keyword :remote-method="remoteOrganizations" :loading="organizationLoading" style="width:100%"><el-option v-for="item in organizationOptions" :key="item.id" :label="item.breadcrumb" :value="item.id" /></el-select></el-form-item>
        </template>
      </el-form>
      <template #footer><el-button @click="editVisible=false">取消</el-button><el-button type="primary" :loading="editFlight.pending.value" :disabled="editFlight.pending.value" @click="editFlight.run()">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="permissionVisible" title="分配功能权限" width="720px" :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" title="只能分配当前操作者拥有且租户未停用的已发布权限；保存采用整版替换。" />
      <el-tree ref="permissionTreeRef" class="permission-tree" node-key="id" show-checkbox check-strictly default-expand-all :data="permissionTree" :props="{ label: 'name', children: 'children' }" @check="handlePermissionCheck">
        <template #default="{ data }"><span>{{ data.name }}</span><small>{{ data.permissionCode }}</small></template>
      </el-tree>
      <el-form label-position="top"><el-form-item label="授权原因"><el-input v-model="permissionReason" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item></el-form>
      <template #footer><el-button @click="permissionVisible=false">取消</el-button><el-button type="primary" :loading="permissionFlight.pending.value" :disabled="permissionFlight.pending.value" @click="permissionFlight.run()">保存权限</el-button></template>
    </el-dialog>

    <el-dialog v-model="dataVisible" title="配置数据范围" width="720px" :close-on-click-modal="false">
      <el-alert type="info" :closable="false" title="默认范围作用于没有模块覆盖的业务；未来发布数据模块后，会在此保留并独立维护模块覆盖。" />
      <div v-for="policy in dataPolicies" :key="policy.moduleCode" class="policy-row">
        <strong>{{ policy.moduleName }}</strong>
        <el-select v-model="policy.scopeType"><el-option v-for="scope in boundary?.scopeTypes || []" :key="scope" :label="scopeLabels[scope]" :value="scope" /></el-select>
        <el-select v-if="policy.scopeType.startsWith('SPECIFIED_')" v-model="policy.organizationIds" multiple filterable remote reserve-keyword :remote-method="remoteOrganizations" :loading="organizationLoading"><el-option v-for="item in organizationOptions" :key="item.id" :label="item.breadcrumb" :value="item.id" /></el-select>
      </div>
      <el-form label-position="top"><el-form-item label="变更原因"><el-input v-model="dataReason" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item></el-form>
      <template #footer><el-button @click="dataVisible=false">取消</el-button><el-button type="primary" :loading="dataFlight.pending.value" :disabled="dataFlight.pending.value" @click="dataFlight.run()">保存数据范围</el-button></template>
    </el-dialog>

    <el-dialog v-model="operationVisible" :title="operationAction === 'delete' ? '删除角色' : (operationRole?.status === 'ACTIVE' ? '停用角色' : '恢复角色')" width="520px" :close-on-click-modal="false">
      <el-alert v-if="operationAction === 'delete'" type="warning" :closable="false" title="删除不可恢复；角色编码和历史 ID 永久保留，不允许复用。" />
      <el-form label-position="top"><el-form-item label="操作原因"><el-input v-model="operationReason" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-form>
      <template #footer><el-button @click="operationVisible=false">取消</el-button><el-button :type="operationAction === 'delete' ? 'danger' : 'primary'" :loading="operationFlight.pending.value" :disabled="operationFlight.pending.value" @click="operationFlight.run()">确认</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page{padding:36px}.page-lead{display:flex;justify-content:space-between;align-items:flex-end;gap:24px}.eyebrow{margin:0 0 10px;color:#168f89;font-size:11px;font-weight:700;letter-spacing:.14em}.page-lead h2{margin:0;font-size:34px}.page-lead p:last-child{color:#748281}.boundary-card{margin:24px 0 14px;padding:16px 20px;display:flex;gap:14px;align-items:center;border:1px solid #cfe3df;border-radius:14px;background:#eaf7f4}.boundary-card span,.boundary-card small{color:#64807d}.panel{overflow:hidden;border:1px solid #dce5e1;border-radius:16px;background:#fff}.panel strong,.panel small{display:block}.panel small{margin-top:4px;color:#82908f}.pagination{padding:18px 22px;display:flex;justify-content:flex-end;border-top:1px solid #e8eeeb}.protected{color:#8b9795}.permission-tree{max-height:420px;margin:18px 0;padding:14px;overflow:auto;border:1px solid #e0e8e5;border-radius:12px}.permission-tree span{margin-right:10px}.permission-tree small{color:#8a9694}.policy-row{margin:18px 0;display:grid;grid-template-columns:150px 1fr 1.4fr;gap:12px;align-items:center}.policy-row .el-select{width:100%}@media(max-width:760px){.page{padding:20px}.page-lead{align-items:flex-start;flex-direction:column}.policy-row{grid-template-columns:1fr}}
</style>
