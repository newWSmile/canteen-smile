<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createPermissionResource,
  deprecatePermissionResource,
  pagePermissionResources,
  publishPermissionResource,
} from '../api/permissionApi'
import type {
  CreatePermissionResourceRequest,
  PermissionAppCode,
  PermissionPublishStatus,
  PermissionResource,
  PermissionResourceType,
} from '../types'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

const loading = ref(false)
const resources = ref<PermissionResource[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 20
const publishStatus = ref<PermissionPublishStatus | ''>('')
const appCode = ref<PermissionAppCode | ''>('')
const resourceType = ref<PermissionResourceType | ''>('')
const dialogVisible = ref(false)

const form = reactive<CreatePermissionResourceRequest>({
  permissionCode: '',
  resourceType: 'MENU',
  name: '',
  appCode: 'TENANT_ADMIN',
  semanticVersion: 1,
  sortOrder: 0,
})

const parentOptions = computed(() => resources.value.filter(
  (item) => item.appCode === form.appCode
    && (item.resourceType === 'DIRECTORY' || item.resourceType === 'MENU')
    && item.publishStatus !== 'DEPRECATED',
))

async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await pagePermissionResources({
      pageNo: pageNo.value,
      pageSize,
      publishStatus: publishStatus.value || undefined,
      appCode: appCode.value || undefined,
      resourceType: resourceType.value || undefined,
    })
    resources.value = page.items
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  Object.assign(form, {
    permissionCode: '', resourceType: 'MENU', parentId: undefined, name: '', description: undefined,
    appCode: 'TENANT_ADMIN', routePath: undefined, componentKey: undefined, apiMethod: undefined,
    apiPathPattern: undefined, featureCode: undefined, semanticVersion: 1, sortOrder: 0,
  })
  dialogVisible.value = true
}

const createFlight = useSingleFlight(async () => {
  if (!form.permissionCode.trim() || !form.name.trim()) {
    feedback.warning('请填写权限码和资源名称')
    return
  }
  if (form.resourceType === 'API' && (!form.apiMethod || !form.apiPathPattern?.trim())) {
    feedback.warning('API 资源必须填写 HTTP 方法和模板路径')
    return
  }
  await createPermissionResource({
    ...form,
    permissionCode: form.permissionCode.trim(),
    name: form.name.trim(),
    description: form.description?.trim() || undefined,
    routePath: form.routePath?.trim() || undefined,
    componentKey: form.componentKey?.trim() || undefined,
    apiPathPattern: form.apiPathPattern?.trim() || undefined,
    featureCode: form.featureCode?.trim() || undefined,
    apiMethod: form.resourceType === 'API' ? form.apiMethod : undefined,
  })
  feedback.success('权限资源草稿已创建')
  dialogVisible.value = false
  pageNo.value = 1
  await load()
})

const lifecycleFlight = useSingleFlight(async (resource: PermissionResource, action: 'publish' | 'deprecate') => {
  if (action === 'publish') {
    await publishPermissionResource(resource)
    feedback.success('权限资源已发布，权限码从现在起永久保留')
  } else {
    await deprecatePermissionResource(resource)
    feedback.success('权限资源已废弃，原权限码不会再次复用')
  }
  await load()
})

function changePage(value: number): void {
  pageNo.value = value
  void load()
}

function applyFilters(): void {
  pageNo.value = 1
  void load()
}

onMounted(load)
</script>

<template>
  <section class="permission-page" v-loading="loading">
    <header class="page-header">
      <div>
        <p>PLATFORM / PERMISSION RESOURCE</p>
        <h1>权限资源</h1>
        <span>平台统一定义菜单、按钮与 API 契约；发布后只能废弃，权限码永久不得复用。</span>
      </div>
      <el-button type="primary" size="large" @click="openCreate">新建权限草稿</el-button>
    </header>

    <section class="toolbar">
      <el-select v-model="publishStatus" clearable placeholder="全部状态" @change="applyFilters">
        <el-option label="草稿" value="DRAFT" /><el-option label="已发布" value="PUBLISHED" />
        <el-option label="已废弃" value="DEPRECATED" />
      </el-select>
      <el-select v-model="appCode" clearable placeholder="全部应用" @change="applyFilters">
        <el-option label="平台管理端" value="PLATFORM_ADMIN" /><el-option label="租户管理端" value="TENANT_ADMIN" />
        <el-option label="租户业务端" value="TENANT_PORTAL" /><el-option label="服务端" value="SERVICE" />
      </el-select>
      <el-select v-model="resourceType" clearable placeholder="全部类型" @change="applyFilters">
        <el-option v-for="item in ['DIRECTORY','MENU','BUTTON','API','DATA_MODULE']" :key="item" :label="item" :value="item" />
      </el-select>
    </section>

    <section class="panel">
      <el-table :data="resources" empty-text="暂无权限资源">
        <el-table-column label="权限资源" min-width="260">
          <template #default="scope"><strong>{{ scope.row.name }}</strong><small>{{ scope.row.permissionCode }}</small></template>
        </el-table-column>
        <el-table-column prop="resourceType" label="类型" width="120" />
        <el-table-column prop="appCode" label="应用" min-width="150" />
        <el-table-column prop="featureCode" label="功能开关" min-width="160"><template #default="scope">{{ scope.row.featureCode || '—' }}</template></el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="scope"><el-tag :type="scope.row.publishStatus === 'PUBLISHED' ? 'success' : scope.row.publishStatus === 'DEPRECATED' ? 'info' : 'warning'">{{ scope.row.publishStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button v-if="scope.row.publishStatus === 'DRAFT'" type="primary" link :disabled="lifecycleFlight.pending.value" @click="lifecycleFlight.run(scope.row, 'publish')">发布</el-button>
            <el-popconfirm v-else-if="scope.row.publishStatus === 'PUBLISHED'" title="废弃后权限码永久不能复用，确认继续？" @confirm="lifecycleFlight.run(scope.row, 'deprecate')">
              <template #reference><el-button type="danger" link :disabled="lifecycleFlight.pending.value">废弃</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination"><el-pagination background layout="prev, pager, next, total" :current-page="pageNo" :page-size="pageSize" :total="total" @current-change="changePage" /></div>
    </section>

    <el-dialog v-model="dialogVisible" title="新建权限资源草稿" width="760px" :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" title="权限码创建后即被数据库唯一约束占用；发布后不得修改含义或复用。" />
      <el-form class="resource-form" label-position="top">
        <el-form-item label="权限码"><el-input v-model="form.permissionCode" maxlength="128" placeholder="例如 iam:role:view" /></el-form-item>
        <el-form-item label="资源名称"><el-input v-model="form.name" maxlength="128" /></el-form-item>
        <el-form-item label="资源类型"><el-select v-model="form.resourceType"><el-option v-for="item in ['DIRECTORY','MENU','BUTTON','API']" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="所属应用"><el-select v-model="form.appCode" @change="form.parentId = undefined"><el-option v-for="item in ['PLATFORM_ADMIN','TENANT_ADMIN','TENANT_PORTAL','SERVICE']" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="父目录/菜单"><el-select v-model="form.parentId" clearable><el-option v-for="item in parentOptions" :key="item.id" :label="`${item.name} · ${item.permissionCode}`" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="功能开关编码"><el-input v-model="form.featureCode" maxlength="128" placeholder="可选；发布时自动同步既有租户" /></el-form-item>
        <el-form-item v-if="form.resourceType === 'MENU'" label="前端路由"><el-input v-model="form.routePath" maxlength="256" /></el-form-item>
        <el-form-item v-if="form.resourceType === 'MENU'" label="本地组件键"><el-input v-model="form.componentKey" maxlength="128" /></el-form-item>
        <el-form-item v-if="form.resourceType === 'API'" label="HTTP 方法"><el-select v-model="form.apiMethod"><el-option v-for="item in ['GET','POST','PUT','PATCH','DELETE']" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item v-if="form.resourceType === 'API'" label="API 模板路径"><el-input v-model="form.apiPathPattern" maxlength="256" placeholder="/api/iam/v1/tenant/roles/{roleId}" /></el-form-item>
        <el-form-item label="语义版本"><el-input-number v-model="form.semanticVersion" :min="1" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item class="wide" label="说明"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="createFlight.pending.value" :disabled="createFlight.pending.value" @click="createFlight.run()">保存草稿</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.permission-page { max-width: 1280px; margin: 0 auto; color: #242129; }
.page-header { display: flex; justify-content: space-between; align-items: end; gap: 24px; }
.page-header p { margin: 24px 0 10px; color: #6d48c4; font-size: 11px; font-weight: 700; letter-spacing: .14em; }
.page-header h1 { margin: 0 0 8px; font-size: 42px; }.page-header span { color: #78727e; }
.toolbar { margin: 28px 0 14px; display: flex; gap: 12px; }.toolbar .el-select { width: 180px; }
.panel { overflow: hidden; border: 1px solid #e0e1dc; border-radius: 18px; background: #fff; }
.panel strong,.panel small { display: block; }.panel small { margin-top: 5px; color: #918b96; }
.pagination { padding: 18px 22px; display: flex; justify-content: flex-end; border-top: 1px solid #ecece8; }
.resource-form { margin-top: 20px; display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }.resource-form .el-select { width: 100%; }.resource-form .wide { grid-column: 1/-1; }
@media(max-width:760px){.page-header{align-items:flex-start;flex-direction:column}.resource-form{grid-template-columns:1fr}.resource-form .wide{grid-column:auto}}
</style>
