<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useTenantContextStore } from '@/app/store/tenantContext'
import {
  changeOrganizationTypeStatus,
  createOrganizationType,
  getOrganizationTypeRelations,
  getOrganizationTypes,
  replaceOrganizationTypeRelations,
  updateOrganizationType,
} from '@/modules/organization/api/organizationApi'
import type { OrganizationType } from '@/modules/organization/types'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

const tenantContext = useTenantContextStore()
const loading = ref(false)
const types = ref<OrganizationType[]>([])
const relationSelection = reactive<Record<string, string[]>>({})
const dialogVisible = ref(false)
const editingType = ref<OrganizationType | null>(null)
const form = reactive({ typeCode: '', name: '', sortOrder: 0 })

const canManage = computed(() => tenantContext.hasPermission('iam:org-type:manage'))
const activeTypes = computed(() => types.value.filter((type) => type.status === 'ACTIVE'))

async function load(): Promise<void> {
  loading.value = true
  try {
    const [page, relations] = await Promise.all([
      getOrganizationTypes(),
      getOrganizationTypeRelations(),
    ])
    types.value = page.items
    for (const key of Object.keys(relationSelection)) delete relationSelection[key]
    for (const type of activeTypes.value) relationSelection[type.id] = []
    for (const relation of relations) {
      relationSelection[relation.parentTypeId]?.push(relation.childTypeId)
    }
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  editingType.value = null
  Object.assign(form, { typeCode: '', name: '', sortOrder: types.value.length * 10 })
  dialogVisible.value = true
}

function openEdit(type: OrganizationType): void {
  editingType.value = type
  Object.assign(form, { typeCode: type.typeCode, name: type.name, sortOrder: type.sortOrder })
  dialogVisible.value = true
}

const submitFlight = useSingleFlight(async () => {
  if (!form.name.trim() || (!editingType.value && !form.typeCode.trim())) {
    feedback.warning('请完整填写类型编码和名称')
    return
  }
  if (editingType.value) {
    await updateOrganizationType(editingType.value, {
      name: form.name.trim(),
      sortOrder: form.sortOrder,
    })
    feedback.success('机构类型已更新')
  } else {
    await createOrganizationType({
      typeCode: form.typeCode.trim(),
      name: form.name.trim(),
      sortOrder: form.sortOrder,
    })
    feedback.success('机构类型已创建')
  }
  dialogVisible.value = false
  await load()
})

const statusFlight = useSingleFlight(async (type: OrganizationType) => {
  await changeOrganizationTypeStatus(type)
  feedback.success(type.status === 'ACTIVE' ? '机构类型已停用' : '机构类型已恢复')
  await load()
})

const relationFlight = useSingleFlight(async () => {
  const pairs = activeTypes.value.flatMap((parent) =>
    (relationSelection[parent.id] ?? []).map((childTypeId) => ({
      parentTypeId: parent.id,
      childTypeId,
    })),
  )
  await replaceOrganizationTypeRelations(pairs)
  feedback.success('机构类型允许关系已保存')
  await load()
})

onMounted(load)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="page-lead">
      <div>
        <p class="eyebrow">ORGANIZATION TYPE GOVERNANCE</p>
        <h2>类型可以调整，已在使用的结构不会被静默破坏。</h2>
        <p>类型编码永久保留；允许关系按完整有向无环图保存，后端再次校验真实机构树。</p>
      </div>
      <el-button v-if="canManage" type="primary" @click="openCreate">新增机构类型</el-button>
    </div>

    <div class="panel">
      <div class="panel-title"><strong>机构类型</strong><span>共 {{ types.length }} 个</span></div>
      <el-table :data="types" empty-text="暂无机构类型">
        <el-table-column prop="typeCode" label="类型编码" min-width="150" />
        <el-table-column prop="name" label="类型名称" min-width="150" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="来源" width="120">
          <template #default="scope">{{ scope.row.sourceTemplateVersion ? `模板 v${scope.row.sourceTemplateVersion}` : '租户新增' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ scope.row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canManage" label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
            <el-button
              link
              :type="scope.row.status === 'ACTIVE' ? 'danger' : 'success'"
              :loading="statusFlight.pending.value"
              @click="statusFlight.run(scope.row)"
            >{{ scope.row.status === 'ACTIVE' ? '停用' : '恢复' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel relation-panel">
      <div class="panel-title">
        <div><strong>允许的父子类型关系</strong><span>一个父类型可以直接挂载多个子类型</span></div>
        <el-button
          v-if="canManage"
          type="primary"
          :loading="relationFlight.pending.value"
          @click="relationFlight.run()"
        >保存完整关系</el-button>
      </div>
      <div v-if="activeTypes.length" class="relation-grid">
        <article v-for="parent in activeTypes" :key="parent.id">
          <header><strong>{{ parent.name }}</strong><code>{{ parent.typeCode }}</code></header>
          <el-checkbox-group v-model="relationSelection[parent.id]" :disabled="!canManage">
            <el-checkbox
              v-for="child in activeTypes.filter((item) => item.id !== parent.id)"
              :key="child.id"
              :value="child.id"
            >{{ child.name }}</el-checkbox>
          </el-checkbox-group>
        </article>
      </div>
      <el-empty v-else description="请先启用至少一个机构类型" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingType ? '编辑机构类型' : '新增机构类型'" width="480px">
      <el-form label-position="top">
        <el-form-item label="类型编码">
          <el-input v-model="form.typeCode" :disabled="Boolean(editingType)" maxlength="64" placeholder="例如 SCHOOL" />
        </el-form-item>
        <el-form-item label="类型名称"><el-input v-model="form.name" maxlength="100" /></el-form-item>
        <el-form-item label="显示排序"><el-input-number v-model="form.sortOrder" :min="0" :max="100000" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitFlight.pending.value" @click="submitFlight.run()">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page { padding: 34px 36px 56px; }
.page-lead { display: flex; align-items: end; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.eyebrow { margin: 0; color: #176b8c; font-size: 11px; font-weight: 700; letter-spacing: .15em; }
h2 { margin: 8px 0; font-size: 30px; } .page-lead p:last-child { margin: 0; color: #75827f; }
.panel { margin-top: 18px; overflow: hidden; border: 1px solid #dce4e0; border-radius: 16px; background: #fff; }
.panel-title { min-height: 68px; padding: 16px 22px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #e6ece9; }
.panel-title > div { display: grid; gap: 4px; }.panel-title span { margin-left: 10px; color: #899391; font-size: 13px; }
.relation-grid { padding: 20px; display: grid; grid-template-columns: repeat(auto-fit,minmax(260px,1fr)); gap: 14px; }
.relation-grid article { padding: 16px; border: 1px solid #e2e9e6; border-radius: 12px; }
.relation-grid header { margin-bottom: 12px; display: flex; justify-content: space-between; }.relation-grid code { color: #699098; }
.relation-grid :deep(.el-checkbox-group) { display: flex; flex-wrap: wrap; gap: 2px 14px; }
@media (max-width: 760px) { .page { padding: 24px 20px; } .page-lead { align-items: start; flex-direction: column; } }
</style>
