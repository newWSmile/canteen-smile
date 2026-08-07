<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useTenantContextStore } from '@/app/store/tenantContext'
import {
  changeOrganizationStatus,
  createOrganization,
  deleteOrganization,
  getActiveOrganizationTypes,
  getChildOrganizations,
  getOrganizationTypeRelations,
  getRootOrganization,
  moveOrganization,
  searchOrganizations,
  updateOrganization,
} from '@/modules/organization/api/organizationApi'
import type {
  Organization,
  OrganizationSearchResult,
  OrganizationType,
  OrganizationTypeRelation,
} from '@/modules/organization/types'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

type DialogMode = 'create' | 'edit' | 'move' | 'status' | 'delete'

const tenantContext = useTenantContextStore()
const loading = ref(false)
const current = ref<Organization | null>(null)
const children = ref<Organization[]>([])
const types = ref<OrganizationType[]>([])
const relations = ref<OrganizationTypeRelation[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)
const trail = ref<Organization[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<DialogMode>('create')
const selected = ref<Organization | null>(null)
const form = reactive({ organizationTypeId: '', businessCode: '', name: '' })
const reason = ref('')
const moveKeyword = ref('')
const moveCandidates = ref<OrganizationSearchResult[]>([])
const newParentId = ref('')

const canCreate = computed(() => tenantContext.hasPermission('iam:org:create'))
const canUpdate = computed(() => tenantContext.hasPermission('iam:org:update'))
const canMove = computed(() => tenantContext.hasPermission('iam:org:move'))
const canStatus = computed(() => tenantContext.hasPermission('iam:org:status'))
const canDelete = computed(() => tenantContext.hasPermission('iam:org:delete'))
const allowedChildTypes = computed(() => {
  if (!current.value) return []
  const childIds = new Set(
    relations.value
      .filter((relation) => relation.parentTypeId === current.value?.organizationTypeId)
      .map((relation) => relation.childTypeId),
  )
  return types.value.filter((type) => childIds.has(type.id))
})

async function loadChildren(): Promise<void> {
  if (!current.value) return
  loading.value = true
  try {
    const page = await getChildOrganizations(current.value.id, pageNo.value, pageSize.value)
    children.value = page.items
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function initialize(): Promise<void> {
  loading.value = true
  try {
    const [root, activeTypes, typeRelations] = await Promise.all([
      getRootOrganization(),
      getActiveOrganizationTypes(),
      getOrganizationTypeRelations(),
    ])
    current.value = root
    trail.value = [root]
    types.value = activeTypes
    relations.value = typeRelations
  } finally {
    loading.value = false
  }
  await loadChildren()
}

async function enter(organization: Organization): Promise<void> {
  current.value = organization
  trail.value.push(organization)
  pageNo.value = 1
  await loadChildren()
}

async function goToTrail(index: number): Promise<void> {
  const target = trail.value[index]
  if (!target) return
  current.value = target
  trail.value = trail.value.slice(0, index + 1)
  pageNo.value = 1
  await loadChildren()
}

function openDialog(mode: DialogMode, organization: Organization | null = null): void {
  dialogMode.value = mode
  selected.value = organization
  reason.value = ''
  moveKeyword.value = ''
  moveCandidates.value = []
  newParentId.value = ''
  if (mode === 'create') {
    Object.assign(form, {
      organizationTypeId: allowedChildTypes.value[0]?.id ?? '',
      businessCode: '',
      name: '',
    })
  } else if (organization) {
    Object.assign(form, {
      organizationTypeId: organization.organizationTypeId,
      businessCode: organization.businessCode,
      name: organization.name,
    })
  }
  dialogVisible.value = true
}

const searchFlight = useSingleFlight(async () => {
  if (moveKeyword.value.trim().length < 2) {
    feedback.warning('请输入至少两个字符搜索目标父机构')
    return
  }
  moveCandidates.value = await searchOrganizations(moveKeyword.value.trim())
})

const submitFlight = useSingleFlight(async () => {
  if (dialogMode.value === 'create') {
    if (!current.value || !form.organizationTypeId || !form.businessCode.trim() || !form.name.trim()) {
      feedback.warning('请完整填写机构类型、业务编码和名称')
      return
    }
    await createOrganization({
      parentId: current.value.id,
      organizationTypeId: form.organizationTypeId,
      businessCode: form.businessCode.trim(),
      name: form.name.trim(),
      adminRegionId: null,
    })
    feedback.success('机构已创建')
  } else if (dialogMode.value === 'edit' && selected.value) {
    if (!form.organizationTypeId || !form.name.trim()) {
      feedback.warning('请完整填写机构类型和名称')
      return
    }
    await updateOrganization(selected.value, {
      organizationTypeId: form.organizationTypeId,
      name: form.name.trim(),
      adminRegionId: selected.value.adminRegionId,
    })
    feedback.success('机构资料已更新')
  } else if (dialogMode.value === 'move' && selected.value) {
    if (!newParentId.value || !reason.value.trim()) {
      feedback.warning('请选择新父机构并填写迁移原因')
      return
    }
    await moveOrganization(selected.value, newParentId.value, reason.value.trim())
    feedback.success('机构已迁移')
  } else if (dialogMode.value === 'status' && selected.value) {
    if (!reason.value.trim()) {
      feedback.warning('请填写状态变更原因')
      return
    }
    await changeOrganizationStatus(selected.value, reason.value.trim())
    feedback.success(selected.value.ownStatus === 'ACTIVE' ? '机构及下级已继承停用' : '机构已恢复')
  } else if (dialogMode.value === 'delete' && selected.value) {
    if (!reason.value.trim()) {
      feedback.warning('请填写删除原因')
      return
    }
    await deleteOrganization(selected.value, reason.value.trim())
    feedback.success('空白机构已删除，业务编码仍永久保留')
  }
  dialogVisible.value = false
  await loadChildren()
})

function dialogTitle(): string {
  return {
    create: '新增直属子机构',
    edit: '编辑机构资料',
    move: '迁移机构',
    status: selected.value?.ownStatus === 'ACTIVE' ? '停用机构' : '恢复机构',
    delete: '删除空白机构',
  }[dialogMode.value]
}

onMounted(initialize)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="page-lead">
      <div>
        <p class="eyebrow">ORGANIZATION TREE</p>
        <h2>机构树由关系规则约束，停用状态由祖先向下继承。</h2>
        <p>当前列表只查询直属子机构并分页；迁移、状态变更和删除均由后端做租户边界与依赖校验。</p>
      </div>
      <el-button
        v-if="canCreate"
        type="primary"
        :disabled="current?.effectiveStatus !== 'ACTIVE' || !allowedChildTypes.length"
        @click="openDialog('create')"
      >新增直属机构</el-button>
    </div>

    <div class="breadcrumb">
      <button v-for="(item, index) in trail" :key="item.id" @click="goToTrail(index)">
        {{ item.name }}<span v-if="index < trail.length - 1">/</span>
      </button>
    </div>

    <div v-if="current" class="current-card">
      <div class="current-mark">{{ current.name.slice(0, 1) }}</div>
      <div><small>当前父机构 · {{ current.typeName }}</small><h3>{{ current.name }}</h3><code>{{ current.businessCode }}</code></div>
      <div class="current-status">
        <el-tag :type="current.effectiveStatus === 'ACTIVE' ? 'success' : 'danger'">
          {{ current.effectiveStatus === 'ACTIVE' ? '有效' : '继承停用' }}
        </el-tag>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title"><strong>直属子机构</strong><span>总计 {{ total }}</span></div>
      <el-table :data="children" empty-text="暂无直属子机构">
        <el-table-column label="机构" min-width="240">
          <template #default="scope">
            <button class="org-link" @click="enter(scope.row)"><strong>{{ scope.row.name }}</strong><small>{{ scope.row.businessCode }}</small></button>
          </template>
        </el-table-column>
        <el-table-column prop="typeName" label="机构类型" min-width="130" />
        <el-table-column label="自身状态" width="110">
          <template #default="scope"><el-tag :type="scope.row.ownStatus === 'ACTIVE' ? 'success' : 'info'">{{ scope.row.ownStatus === 'ACTIVE' ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="实际状态" width="110">
          <template #default="scope"><el-tag :type="scope.row.effectiveStatus === 'ACTIVE' ? 'success' : 'danger'">{{ scope.row.effectiveStatus === 'ACTIVE' ? '有效' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="下级" width="80">
          <template #default="scope">{{ scope.row.hasChildren ? '有' : '无' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="scope">
            <el-button v-if="canUpdate" link type="primary" @click="openDialog('edit', scope.row)">编辑</el-button>
            <el-button v-if="canMove" link type="primary" @click="openDialog('move', scope.row)">迁移</el-button>
            <el-button v-if="canStatus" link :type="scope.row.ownStatus === 'ACTIVE' ? 'warning' : 'success'" @click="openDialog('status', scope.row)">{{ scope.row.ownStatus === 'ACTIVE' ? '停用' : '恢复' }}</el-button>
            <el-button v-if="canDelete" link type="danger" @click="openDialog('delete', scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="pageNo" v-model:page-size="pageSize" :total="total" layout="prev, pager, next, total" @current-change="loadChildren" /></div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle()" width="540px">
      <el-form v-if="dialogMode === 'create' || dialogMode === 'edit'" label-position="top">
        <el-form-item label="机构类型">
          <el-select v-model="form.organizationTypeId" style="width:100%">
            <el-option v-for="type in allowedChildTypes" :key="type.id" :label="`${type.name} · ${type.typeCode}`" :value="type.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="机构业务编码">
          <el-input v-model="form.businessCode" maxlength="64" placeholder="租户内永久唯一，创建后不可修改" />
        </el-form-item>
        <el-form-item label="机构名称"><el-input v-model="form.name" maxlength="200" /></el-form-item>
        <el-alert v-if="dialogMode === 'edit'" type="info" :closable="false" title="修改类型时，后端会校验父关系和所有直属子关系。" />
      </el-form>

      <div v-else-if="dialogMode === 'move'" class="move-box">
        <el-input v-model="moveKeyword" placeholder="输入名称或业务编码，至少两个字符">
          <template #append><el-button :loading="searchFlight.pending.value" @click="searchFlight.run()">搜索</el-button></template>
        </el-input>
        <el-radio-group v-model="newParentId" class="candidate-list">
          <el-radio v-for="candidate in moveCandidates" :key="candidate.id" :value="candidate.id" :disabled="candidate.effectiveStatus !== 'ACTIVE'">
            <span>{{ candidate.breadcrumb }}</span><small>{{ candidate.typeName }} · {{ candidate.businessCode }}</small>
          </el-radio>
        </el-radio-group>
        <el-empty v-if="!moveCandidates.length" :image-size="54" description="搜索并选择新的父机构" />
      </div>

      <el-alert
        v-if="dialogMode === 'status'"
        :type="selected?.ownStatus === 'ACTIVE' ? 'warning' : 'success'"
        :closable="false"
        :title="selected?.ownStatus === 'ACTIVE' ? '停用后全部下级机构继承停用。' : '恢复自身状态后，实际状态仍受上级机构影响。'"
      />
      <el-alert v-if="dialogMode === 'delete'" type="error" :closable="false" title="仅完全空白机构可以删除；删除后业务编码仍永久保留。" />

      <el-form v-if="dialogMode === 'move' || dialogMode === 'status' || dialogMode === 'delete'" label-position="top" class="reason-form">
        <el-form-item label="操作原因"><el-input v-model="reason" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="dialogMode === 'delete' ? 'danger' : 'primary'" :loading="submitFlight.pending.value" @click="submitFlight.run()">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page { padding: 34px 36px 56px; }.page-lead { display:flex; align-items:end; justify-content:space-between; gap:24px; margin-bottom:22px; }
.eyebrow { margin:0; color:#176b8c; font-size:11px; font-weight:700; letter-spacing:.15em; } h2 { margin:8px 0; font-size:30px; }.page-lead p:last-child { margin:0; color:#75827f; }
.breadcrumb { margin:12px 0; display:flex; flex-wrap:wrap; gap:4px; }.breadcrumb button { padding:0; border:0; color:#27758b; background:none; cursor:pointer; }.breadcrumb span { margin-left:6px; color:#a6afac; }
.current-card { padding:20px; display:grid; grid-template-columns:auto 1fr auto; gap:16px; align-items:center; border:1px solid #dce4e0; border-radius:16px; background:#fff; }.current-mark { width:48px; height:48px; display:grid; place-items:center; color:#123f50; background:#b8ece6; border-radius:14px; font-size:20px; font-weight:700; }.current-card small,.current-card code { color:#748481; }.current-card h3 { margin:4px 0; }
.panel { margin-top:18px; overflow:hidden; border:1px solid #dce4e0; border-radius:16px; background:#fff; }.panel-title { padding:18px 22px; display:flex; justify-content:space-between; border-bottom:1px solid #e6ece9; }.panel-title span { color:#899391; }.org-link { padding:0; display:grid; gap:4px; border:0; text-align:left; color:#1d5364; background:none; cursor:pointer; }.org-link small { color:#899391; }.pager { padding:16px 20px; display:flex; justify-content:flex-end; border-top:1px solid #e6ece9; }
.move-box { display:grid; gap:14px; }.candidate-list { display:grid; gap:8px; }.candidate-list :deep(.el-radio) { height:auto; padding:10px; align-items:start; border:1px solid #e2e9e6; border-radius:10px; }.candidate-list span,.candidate-list small { display:block; }.candidate-list small { margin-top:3px; color:#83908d; }.reason-form { margin-top:16px; }
@media (max-width:760px){.page{padding:24px 20px}.page-lead{align-items:start;flex-direction:column}.current-card{grid-template-columns:auto 1fr}.current-status{grid-column:1/-1}}
</style>
