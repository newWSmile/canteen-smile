<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { AuditLog, AuditLogPageQuery, AuditResult, AuditSource } from '@canteen-smile/contracts'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { pageTenantAuditLogs } from '../api/auditApi'

const tenantContext = useTenantContextStore()
const loading = ref(false)
const rows = ref<AuditLog[]>([])
const total = ref(0)
const startTime = ref('')
const endTime = ref('')
const query = reactive<AuditLogPageQuery>({ pageNo: 1, pageSize: 20, source: 'IAM' })
const sourceLabels: Record<AuditSource, string> = { IAM: '管理操作审计', AUTH: '认证安全审计' }
const resultLabels: Record<AuditResult, string> = { SUCCESS: '成功', FAILURE: '失败', DENIED: '已拒绝' }
const operatorLabels: Record<string, string> = {
  PLATFORM_IDENTITY: '平台身份', TENANT_ACCOUNT: '租户账号', SYSTEM: '系统', ANONYMOUS: '匿名请求',
}

/** 加载后端按当前租户身份下推数据边界后的真实审计分页。 */
async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await pageTenantAuditLogs({
      ...query,
      actionCode: query.actionCode?.trim() || undefined,
      operatorId: query.operatorId?.trim() || undefined,
      startTime: startTime.value ? new Date(startTime.value).toISOString() : undefined,
      endTime: endTime.value ? new Date(endTime.value).toISOString() : undefined,
    })
    rows.value = page.items
    total.value = page.total
  } finally { loading.value = false }
}

function search(): void { query.pageNo = 1; void load() }
function reset(): void {
  Object.assign(query, { pageNo: 1, pageSize: 20, source: 'IAM', actionCode: undefined, result: undefined, operatorId: undefined })
  startTime.value = ''; endTime.value = ''; void load()
}
function changePage(pageNo: number): void { query.pageNo = pageNo; void load() }
function changeSize(pageSize: number): void { query.pageSize = pageSize; query.pageNo = 1; void load() }
function formatTime(value: string): string { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
function resultType(result: AuditResult): 'success' | 'danger' | 'warning' { return result === 'SUCCESS' ? 'success' : result === 'FAILURE' ? 'danger' : 'warning' }

onMounted(load)
</script>

<template>
  <div class="audit-page">
    <section class="page-heading">
      <div><p>租户管理 / 审计日志</p><h2>授权变化和认证安全事件，都能追溯。</h2><span>{{ tenantContext.context?.rootOwner ? '机构所有者可查看本租户全部记录。' : '普通管理员仅能查看本机构管理记录与本人的认证记录。' }}</span></div>
    </section>
    <section class="query-card">
      <el-select v-model="query.source" aria-label="审计来源"><el-option label="管理操作审计" value="IAM" /><el-option label="认证安全审计" value="AUTH" /></el-select>
      <el-select v-model="query.result" clearable placeholder="全部结果" aria-label="操作结果"><el-option label="成功" value="SUCCESS" /><el-option label="失败" value="FAILURE" /><el-option label="已拒绝" value="DENIED" /></el-select>
      <el-input v-model="query.actionCode" clearable placeholder="操作编码（精确）" />
      <el-input v-model="query.operatorId" clearable placeholder="操作者 ID（精确）" />
      <input v-model="startTime" class="date-input" type="datetime-local" aria-label="开始时间" />
      <input v-model="endTime" class="date-input" type="datetime-local" aria-label="结束时间" />
      <div class="query-actions"><el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button></div>
    </section>
    <section class="table-card" v-loading="loading">
      <el-table :data="rows" empty-text="暂无审计记录">
        <el-table-column label="发生时间" min-width="170"><template #default="scope">{{ formatTime(scope.row.occurredTime) }}</template></el-table-column>
        <el-table-column label="来源" width="130"><template #default="scope">{{ sourceLabels[scope.row.source as AuditSource] }}</template></el-table-column>
        <el-table-column label="操作编码" min-width="210" prop="actionCode" />
        <el-table-column label="结果" width="90"><template #default="scope"><el-tag :type="resultType(scope.row.result)">{{ resultLabels[scope.row.result as AuditResult] }}</el-tag></template></el-table-column>
        <el-table-column label="操作者" min-width="150"><template #default="scope"><strong>{{ operatorLabels[scope.row.operatorType] || scope.row.operatorType }}</strong><small>ID {{ scope.row.operatorId || '—' }}</small></template></el-table-column>
        <el-table-column label="目标 / 主体" min-width="170"><template #default="scope"><span>{{ scope.row.targetType || '—' }}</span><small>ID {{ scope.row.targetId || '—' }}</small></template></el-table-column>
        <el-table-column label="原因 / 失败码" min-width="190"><template #default="scope">{{ scope.row.reason || scope.row.failureReasonCode || '—' }}</template></el-table-column>
        <el-table-column label="链路 ID" min-width="190"><template #default="scope"><code>{{ scope.row.traceId || '—' }}</code></template></el-table-column>
      </el-table>
      <div class="pagination"><el-pagination background layout="sizes, prev, pager, next, total" :page-sizes="[20, 50, 100]" :page-size="query.pageSize" :current-page="query.pageNo" :total="total" @current-change="changePage" @size-change="changeSize" /></div>
    </section>
  </div>
</template>

<style scoped>
.audit-page { padding: 36px; display: grid; gap: 18px; }
.page-heading p { margin: 0 0 10px; color: #087c7c; font-size: 12px; font-weight: 800; letter-spacing: .1em; }
.page-heading h2 { margin: 0 0 8px; font-size: 32px; }
.page-heading span { color: #738380; }
.query-card { padding: 14px 16px; display: grid; grid-template-columns: 160px 140px minmax(170px,1fr) 170px 190px 190px auto; gap: 10px; align-items: center; border: 1px solid #dce6e1; border-radius: 14px; background: #fff; }
.date-input { width: 100%; height: 32px; padding: 0 10px; color: #53615f; border: 1px solid #dcdfe6; border-radius: 4px; background: #fff; }
.query-actions { display: flex; white-space: nowrap; }
.table-card { overflow: hidden; border: 1px solid #dce6e1; border-radius: 14px; background: #fff; }
.table-card small { margin-top: 4px; display: block; color: #82908f; }
.table-card code { color: #5d706d; font-size: 12px; word-break: break-all; }
.pagination { padding: 16px; display: flex; justify-content: flex-end; border-top: 1px solid #e7eeea; }
@media (max-width: 1300px) { .query-card { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 760px) { .audit-page { padding: 20px; } .query-card { grid-template-columns: 1fr; } .page-heading h2 { font-size: 25px; } }
</style>
