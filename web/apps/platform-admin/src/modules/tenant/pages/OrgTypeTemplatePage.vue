<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listOrgTypeTemplates, publishOrgTypeTemplate } from '../api/tenantApi'
import type { OrgTypeTemplate, OrgTypeTemplateItem, OrgTypeTemplateRelation } from '../types'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

const router = useRouter()
const templates = ref<OrgTypeTemplate[]>([])
const loading = ref(false)
const editorVisible = ref(false)
const types = ref<OrgTypeTemplateItem[]>([])
const relations = ref<OrgTypeTemplateRelation[]>([])

function resetEditor(): void {
  types.value = [
    { typeCode: 'COUNTRY', name: '国家', sortOrder: 10 },
    { typeCode: 'PROVINCE', name: '省', sortOrder: 20 },
    { typeCode: 'CITY', name: '市', sortOrder: 30 },
    { typeCode: 'DISTRICT', name: '区县', sortOrder: 40 },
    { typeCode: 'CENTER_SCHOOL', name: '中心校', sortOrder: 50 },
    { typeCode: 'SCHOOL', name: '学校', sortOrder: 60 },
    { typeCode: 'CANTEEN', name: '食堂', sortOrder: 70 },
  ]
  relations.value = [
    { parentTypeCode: 'COUNTRY', childTypeCode: 'PROVINCE' },
    { parentTypeCode: 'PROVINCE', childTypeCode: 'CITY' },
    { parentTypeCode: 'CITY', childTypeCode: 'DISTRICT' },
    { parentTypeCode: 'DISTRICT', childTypeCode: 'CENTER_SCHOOL' },
    { parentTypeCode: 'CENTER_SCHOOL', childTypeCode: 'SCHOOL' },
    { parentTypeCode: 'SCHOOL', childTypeCode: 'CANTEEN' },
  ]
}

async function loadTemplates(): Promise<void> {
  loading.value = true
  try {
    templates.value = await listOrgTypeTemplates()
  } catch {
    // 统一 Axios 实例已经反馈错误。
  } finally {
    loading.value = false
  }
}

function openEditor(): void {
  resetEditor()
  editorVisible.value = true
}

function addType(): void {
  types.value.push({ typeCode: '', name: '', sortOrder: (types.value.length + 1) * 10 })
}

function addRelation(): void {
  relations.value.push({ parentTypeCode: '', childTypeCode: '' })
}

const publishFlight = useSingleFlight(async () => {
  if (types.value.some((item) => !item.typeCode.trim() || !item.name.trim())) {
    feedback.warning('请完整填写机构类型编码和名称')
    return
  }
  if (relations.value.some((item) => !item.parentTypeCode || !item.childTypeCode)) {
    feedback.warning('请完整选择机构类型父子关系')
    return
  }
  try {
    await publishOrgTypeTemplate({
      types: types.value.map((item) => ({ ...item, typeCode: item.typeCode.trim().toUpperCase(), name: item.name.trim() })),
      relations: relations.value.map((item) => ({
        parentTypeCode: item.parentTypeCode.trim().toUpperCase(),
        childTypeCode: item.childTypeCode.trim().toUpperCase(),
      })),
    })
    feedback.success('机构类型模板新版本已发布')
    editorVisible.value = false
    await loadTemplates()
  } catch {
    // 统一 Axios 实例已经反馈错误，保留编辑内容供用户修正或重试。
  }
})

onMounted(() => void loadTemplates())
</script>

<template>
  <main class="template-page">
    <header>
      <div>
        <el-button text @click="router.push({ name: 'home' })">← 返回租户治理</el-button>
        <p>PLATFORM / ORGANIZATION TYPE TEMPLATE</p>
        <h1>机构类型模板</h1>
        <span>每次发布形成不可变版本；创建租户时复制为该租户独立维护的数据。</span>
      </div>
      <el-button size="large" type="primary" @click="openEditor">发布新版本</el-button>
    </header>

    <section v-loading="loading" class="version-grid">
      <article v-for="template in templates" :key="template.templateVersion">
        <div class="version-heading">
          <div><small>VERSION</small><strong>v{{ template.templateVersion }}</strong></div>
          <el-tag type="success">已发布</el-tag>
        </div>
        <div class="type-tags">
          <el-tag v-for="item in template.types" :key="item.typeCode" effect="plain">
            {{ item.name }} · {{ item.typeCode }}
          </el-tag>
        </div>
        <p>允许关系 {{ template.relations.length }} 条 · 类型 {{ template.types.length }} 个</p>
      </article>
      <el-empty v-if="!loading && templates.length === 0" description="尚未发布机构类型模板" />
    </section>

    <el-dialog v-model="editorVisible" title="发布完整模板版本" width="860px" destroy-on-close>
      <el-alert type="warning" :closable="false" title="已发布版本不可修改；请在发布前确认类型编码和允许关系。" />
      <div class="editor-section">
        <div class="editor-title"><strong>机构类型</strong><el-button @click="addType">新增类型</el-button></div>
        <div v-for="(item, index) in types" :key="index" class="editor-row type-row">
          <el-input v-model="item.typeCode" placeholder="编码，如 CITY" />
          <el-input v-model="item.name" placeholder="中文名称" />
          <el-input-number v-model="item.sortOrder" :min="0" :max="9999" />
          <el-button type="danger" plain @click="types.splice(index, 1)">移除</el-button>
        </div>
      </div>
      <div class="editor-section">
        <div class="editor-title"><strong>允许的父子关系</strong><el-button @click="addRelation">新增关系</el-button></div>
        <div v-for="(item, index) in relations" :key="index" class="editor-row relation-row">
          <el-select v-model="item.parentTypeCode" placeholder="父类型">
            <el-option v-for="type in types" :key="type.typeCode" :label="`${type.name} · ${type.typeCode}`" :value="type.typeCode" />
          </el-select>
          <span>可以新增</span>
          <el-select v-model="item.childTypeCode" placeholder="子类型">
            <el-option v-for="type in types" :key="type.typeCode" :label="`${type.name} · ${type.typeCode}`" :value="type.typeCode" />
          </el-select>
          <el-button type="danger" plain @click="relations.splice(index, 1)">移除</el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishFlight.pending.value" :disabled="publishFlight.pending.value" @click="publishFlight.run()">
          确认发布新版本
        </el-button>
      </template>
    </el-dialog>
  </main>
</template>

<style scoped>
.template-page { min-height: 100vh; padding: 44px; color: #242129; background: #f3f4f1; }
header { max-width: 1280px; margin: 0 auto 28px; display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; }
header p { margin: 28px 0 8px; color: #6d48c4; font-size: 11px; font-weight: 700; letter-spacing: .13em; }
header h1 { margin: 0 0 8px; font-size: 40px; }
header span { color: #77727c; }
.version-grid { max-width: 1280px; margin: auto; display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.version-grid article { padding: 24px; border: 1px solid #e0e1dc; border-radius: 18px; background: #fff; }
.version-heading { display: flex; align-items: flex-start; justify-content: space-between; }
.version-heading > div { display: grid; gap: 4px; }
.version-heading small { color: #96909a; font-size: 10px; letter-spacing: .12em; }
.version-heading strong { font-size: 28px; }
.type-tags { margin: 20px 0; display: flex; flex-wrap: wrap; gap: 8px; }
article p { margin: 0; color: #87818c; font-size: 12px; }
.editor-section { margin-top: 22px; }
.editor-title { margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center; }
.editor-row { margin-bottom: 9px; display: grid; gap: 10px; align-items: center; }
.type-row { grid-template-columns: 1fr 1fr 150px auto; }
.relation-row { grid-template-columns: 1fr auto 1fr auto; }
.relation-row span { color: #8a8490; font-size: 12px; }
@media (max-width: 900px) { .version-grid { grid-template-columns: 1fr; } .template-page { padding: 20px; } header { align-items: flex-start; flex-direction: column; } }
</style>
