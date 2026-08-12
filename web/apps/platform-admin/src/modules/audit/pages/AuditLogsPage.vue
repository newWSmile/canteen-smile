<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { AuditLog, AuditLogPageQuery, AuditResult, AuditSource } from '@canteen-smile/contracts'
import { pagePlatformAuditLogs } from '../api/auditApi'

const loading = ref(false)
const rows = ref<AuditLog[]>([])
const selected = ref<AuditLog | null>(null)
const total = ref(0)
const startTime = ref('')
const endTime = ref('')
const query = reactive<AuditLogPageQuery>({ pageNo: 1, pageSize: 20, source: 'IAM' })

const sourceLabels: Record<AuditSource, string> = { IAM: '平台治理审计', AUTH: '认证安全审计' }
const resultLabels: Record<AuditResult, string> = { SUCCESS: '成功', FAILURE: '失败', DENIED: '已拒绝' }
const appCodeLabels: Record<string, string> = { PLATFORM_ADMIN: '平台管理端', TENANT_ADMIN: '租户管理端', TENANT_PORTAL: '租户业务端', SERVICE: '服务端任务' }

/** 加载平台身份范围内真实审计分页。 */
async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await pagePlatformAuditLogs({
      ...query,
      actionCode: query.actionCode?.trim() || undefined,
      operatorId: query.operatorId?.trim() || undefined,
      startTime: startTime.value ? new Date(startTime.value).toISOString() : undefined,
      endTime: endTime.value ? new Date(endTime.value).toISOString() : undefined,
    })
    rows.value = page.items
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function search(): void { query.pageNo = 1; void load() }
function reset(): void {
  Object.assign(query, { pageNo: 1, pageSize: 20, source: 'IAM', actionCode: undefined, result: undefined, operatorId: undefined })
  startTime.value = ''
  endTime.value = ''
  void load()
}
function changePage(pageNo: number): void { query.pageNo = pageNo; void load() }
function changeSize(pageSize: number): void { query.pageSize = pageSize; query.pageNo = 1; void load() }
function formatTime(value: string): string { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
function resultType(result: AuditResult): 'success' | 'danger' | 'warning' {
  return result === 'SUCCESS' ? 'success' : result === 'FAILURE' ? 'danger' : 'warning'
}
function showDetails(row: AuditLog): void { selected.value = row }
function rowClassName({ row }: { row: AuditLog }): string {
  return selected.value?.id === row.id && selected.value.source === row.source ? 'selected-audit-row' : ''
}
function operatorName(row: AuditLog): string { return row.operatorDisplayName || row.operatorUsername || row.operatorTypeName }
function targetName(row: AuditLog): string { return row.targetName || row.targetCode || row.targetTypeName }
function appCodeName(value: string | null): string { return value ? (appCodeLabels[value] || value) : '—' }

onMounted(load)
</script>

<template>
  <div class="audit-page">
    <section class="page-heading">
      <div><p>平台治理 / 审计日志</p><h2>关键操作可追溯，认证事件有边界。</h2><span>平台治理记录来自 IAM，登录与会话安全记录由 Auth 独立持有并通过内部签名接口查询。</span></div>
    </section>

    <section class="query-card">
      <el-select v-model="query.source" aria-label="审计来源">
        <el-option label="平台治理审计" value="IAM" />
        <el-option label="认证安全审计" value="AUTH" />
      </el-select>
      <el-select v-model="query.result" clearable placeholder="全部结果" aria-label="操作结果">
        <el-option label="成功" value="SUCCESS" /><el-option label="失败" value="FAILURE" /><el-option label="已拒绝" value="DENIED" />
      </el-select>
      <el-input v-model="query.actionCode" clearable placeholder="高级筛选：操作编码" />
      <el-input v-model="query.operatorId" clearable placeholder="操作者 ID（精确）" />
      <input v-model="startTime" class="date-input" type="datetime-local" aria-label="开始时间" />
      <input v-model="endTime" class="date-input" type="datetime-local" aria-label="结束时间" />
      <div class="query-actions"><el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button></div>
    </section>

    <div class="audit-workspace" :class="{ 'has-detail': selected }">
    <section class="table-card" v-loading="loading">
      <div class="table-meta"><strong>审计记录</strong><span>点击任意一行查看详情</span></div>
      <el-table :data="rows" :row-class-name="rowClassName" empty-text="暂无审计记录" @row-click="showDetails">
        <el-table-column label="发生时间" min-width="155"><template #default="scope">{{ formatTime(scope.row.occurredTime) }}</template></el-table-column>
        <el-table-column label="来源" width="110"><template #default="scope">{{ sourceLabels[scope.row.source as AuditSource] }}</template></el-table-column>
        <el-table-column label="操作" min-width="170"><template #default="scope"><strong>{{ scope.row.actionName }}</strong></template></el-table-column>
        <el-table-column label="结果" width="72"><template #default="scope"><el-tag :type="resultType(scope.row.result)">{{ resultLabels[scope.row.result as AuditResult] }}</el-tag></template></el-table-column>
        <el-table-column label="操作者" min-width="170"><template #default="scope"><strong>{{ operatorName(scope.row) }}</strong><small>{{ scope.row.operatorUsername ? `${scope.row.operatorUsername} · ` : '' }}{{ scope.row.operatorTypeName }}</small></template></el-table-column>
        <el-table-column label="操作对象" min-width="170"><template #default="scope"><strong>{{ targetName(scope.row) }}</strong><small>{{ scope.row.targetCode ? `${scope.row.targetCode} · ` : '' }}{{ scope.row.targetTypeName }}</small></template></el-table-column>
        <el-table-column label="原因 / 失败说明" min-width="150"><template #default="scope">{{ scope.row.reason || scope.row.failureReason || '—' }}</template></el-table-column>
      </el-table>
      <div class="pagination"><el-pagination background layout="sizes, prev, pager, next, total" :page-sizes="[20, 50, 100]" :page-size="query.pageSize" :current-page="query.pageNo" :total="total" @current-change="changePage" @size-change="changeSize" /></div>
    </section>
    <transition name="audit-detail">
      <aside v-if="selected" class="detail-panel" aria-label="审计详情">
        <header class="detail-header">
          <div class="detail-title"><small>审计详情</small><h3>{{ selected.actionName }}</h3><p>{{ operatorName(selected) }} → {{ targetName(selected) }}</p></div>
          <div class="detail-actions"><el-tag :type="resultType(selected.result)">{{ resultLabels[selected.result] }}</el-tag><el-button link @click="selected = null">关闭</el-button></div>
        </header>
      <el-descriptions :column="1" border label-width="104px">
        <el-descriptions-item label="发生时间">{{ formatTime(selected.occurredTime) }}</el-descriptions-item>
        <el-descriptions-item label="审计来源">{{ sourceLabels[selected.source] }}</el-descriptions-item>
        <el-descriptions-item v-if="selected.categoryPath.length" label="审计分类">{{ selected.categoryPath.join(' / ') }}</el-descriptions-item>
        <el-descriptions-item v-if="selected.appCode" label="操作入口">{{ appCodeName(selected.appCode) }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ selected.actionName }}</el-descriptions-item>
        <el-descriptions-item label="操作编码"><code>{{ selected.actionCode }}</code></el-descriptions-item>
        <el-descriptions-item label="执行结果">{{ resultLabels[selected.result] }}</el-descriptions-item>
        <el-descriptions-item label="操作者">{{ operatorName(selected) }}（{{ selected.operatorTypeName }}）</el-descriptions-item>
        <el-descriptions-item label="操作者标识"><code>{{ selected.operatorType }} / {{ selected.operatorId || '—' }}</code></el-descriptions-item>
        <el-descriptions-item label="操作对象">{{ targetName(selected) }}（{{ selected.targetTypeName }}）</el-descriptions-item>
        <el-descriptions-item label="对象标识"><code>{{ selected.targetType || '—' }} / {{ selected.targetId || '—' }}</code></el-descriptions-item>
        <el-descriptions-item v-if="selected.reason" label="操作原因">{{ selected.reason }}</el-descriptions-item>
        <el-descriptions-item v-if="selected.loginMethodName" label="登录方式">{{ selected.loginMethodName }}</el-descriptions-item>
        <el-descriptions-item v-if="selected.failureReason" label="失败说明">{{ selected.failureReason }}</el-descriptions-item>
        <el-descriptions-item v-if="selected.failureReasonCode" label="失败码"><code>{{ selected.failureReasonCode }}</code></el-descriptions-item>
        <el-descriptions-item v-if="selected.maskedMobile" label="脱敏手机号">{{ selected.maskedMobile }}</el-descriptions-item>
        <el-descriptions-item v-if="selected.deviceSummary" label="设备摘要">{{ selected.deviceSummary }}</el-descriptions-item>
        <el-descriptions-item v-if="selected.ipAddress" label="来源 IP">{{ selected.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="链路 ID"><code>{{ selected.traceId || '—' }}</code></el-descriptions-item>
        <el-descriptions-item v-if="selected.durationMs != null" label="执行耗时">{{ selected.durationMs }} ms</el-descriptions-item>
      </el-descriptions>
      </aside>
    </transition>
    </div>
  </div>
</template>

<style scoped>
.audit-page { display: grid; gap: 18px; }
.page-heading p { margin: 0 0 10px; color: #6d48c4; font-size: 12px; font-weight: 800; letter-spacing: .1em; }
.page-heading h2 { margin: 0 0 8px; font-size: 32px; }
.page-heading span { color: #7e7785; }
.query-card { padding: 14px 16px; display: grid; grid-template-columns: 160px 140px minmax(170px,1fr) 170px 190px 190px auto; gap: 10px; align-items: center; border: 1px solid #e0e1dc; border-radius: 14px; background: #fff; }
.date-input { width: 100%; height: 32px; padding: 0 10px; color: #55505b; border: 1px solid #dcdfe6; border-radius: 4px; background: #fff; }
.query-actions { display: flex; white-space: nowrap; }
.table-card { overflow: hidden; border: 1px solid #e0e1dc; border-radius: 14px; background: #fff; }
.audit-workspace { min-width: 0; display: grid; grid-template-columns: minmax(0, 1fr); gap: 16px; align-items: start; }
.audit-workspace.has-detail { grid-template-columns: minmax(720px, 1fr) minmax(360px, 420px); }
.table-meta { padding: 13px 16px; display: flex; align-items: baseline; gap: 12px; border-bottom: 1px solid #ebece8; }
.table-meta span { color: #8b8590; font-size: 13px; }
.detail-panel { position: sticky; top: 106px; max-height: calc(100vh - 122px); overflow: auto; scrollbar-gutter: stable; padding: 18px; border: 1px solid #e0e1dc; border-radius: 14px; background: #fff; box-shadow: 0 14px 36px rgb(45 34 68 / 12%); }
.detail-header { margin-bottom: 16px; display: flex; align-items: flex-start; justify-content: space-between; }
.detail-title { min-width: 0; }
.detail-title small { color: #8b8590; }
.detail-header h3 { margin: 4px 0 0; font-size: 20px; }
.detail-header p { margin: 7px 0 0; overflow: hidden; color: #716879; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.detail-actions { display: flex; align-items: center; gap: 8px; }
.detail-panel code { color: #716879; font-size: 12px; word-break: break-all; }
.audit-detail-enter-active, .audit-detail-leave-active { transition: opacity .18s ease, transform .18s ease; }
.audit-detail-enter-from, .audit-detail-leave-to { opacity: 0; transform: translateX(18px); }
:deep(.el-table__row) { cursor: pointer; }
:deep(.selected-audit-row > td.el-table__cell) { background: #f4efff !important; }
.table-card small { margin-top: 4px; display: block; color: #8b8590; }
.table-card strong { color: #2e2933; font-weight: 600; }
.table-card code { color: #716879; font-size: 12px; word-break: break-all; }
.pagination { padding: 16px; display: flex; justify-content: flex-end; border-top: 1px solid #ebece8; }
@media (max-width: 1300px) { .query-card { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 1500px) { .audit-workspace.has-detail { grid-template-columns: minmax(0, 1fr); } .detail-panel { position: static; max-height: none; } }
@media (max-width: 760px) { .query-card { grid-template-columns: 1fr; } .page-heading h2 { font-size: 25px; } }
</style>
