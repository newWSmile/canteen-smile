<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type {
  SmsDeliveryPageQuery,
  SmsDeliveryRecord,
  SmsDeliveryStatus,
  SmsPurpose,
} from '@canteen-smile/contracts'
import { pageSmsDeliveries } from '../api/smsDeliveryApi'

const loading = ref(false)
const rows = ref<SmsDeliveryRecord[]>([])
const total = ref(0)
const startTime = ref('')
const endTime = ref('')
const query = reactive<SmsDeliveryPageQuery>({ pageNo: 1, pageSize: 20 })

const purposeLabels: Record<SmsPurpose, string> = {
  LOGIN: '手机号登录',
  ACTIVATION: '账号激活',
  PASSWORD_RESET: '密码找回',
  MOBILE_BIND: '绑定手机号',
  MOBILE_CHANGE: '更换手机号',
  ADMIN_REAUTH: '管理员再认证',
  PLATFORM_SECOND_FACTOR: '平台二次验证',
}
const statusLabels: Record<SmsDeliveryStatus, string> = {
  PROCESSING: '处理中',
  ACCEPTED: '已接受',
  FAILED: '失败',
}

/** 加载平台身份可查看的短信发送记录。 */
async function load(): Promise<void> {
  loading.value = true
  try {
    const page = await pageSmsDeliveries({
      ...query,
      mobile: query.mobile?.trim() || undefined,
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
  Object.assign(query, { pageNo: 1, pageSize: 20, mobile: undefined })
  startTime.value = ''
  endTime.value = ''
  void load()
}
function changePage(pageNo: number): void { query.pageNo = pageNo; void load() }
function changeSize(pageSize: number): void { query.pageSize = pageSize; query.pageNo = 1; void load() }
function formatTime(value: string | null): string {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}
function statusType(status: SmsDeliveryStatus): 'success' | 'danger' | 'warning' {
  return status === 'ACCEPTED' ? 'success' : status === 'FAILED' ? 'danger' : 'warning'
}

onMounted(load)
</script>

<template>
  <div class="sms-page">
    <section class="page-heading">
      <div>
        <p>平台治理 / 短信管理 / 短信列表</p>
        <h2>发送内容和投递结果，都有据可查。</h2>
        <span>手机号始终脱敏；验证码默认显示为 ******，仅在短信安全显式开启后，后续新记录才会保留明文。</span>
      </div>
    </section>

    <section class="query-card">
      <el-input v-model="query.mobile" clearable maxlength="32" placeholder="完整手机号（精确查询）" />
      <input v-model="startTime" class="date-input" type="datetime-local" aria-label="开始时间" />
      <input v-model="endTime" class="date-input" type="datetime-local" aria-label="结束时间" />
      <div class="query-actions">
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </section>

    <section class="table-card" v-loading="loading">
      <div class="table-meta"><strong>短信发送记录</strong><span>敏感正文记录会显示红色风险标识</span></div>
      <el-table :data="rows" empty-text="暂无短信发送记录">
        <el-table-column label="提交时间" min-width="160">
          <template #default="scope">{{ formatTime(scope.row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="手机号" width="130" prop="maskedMobile" />
        <el-table-column label="用途" width="130">
          <template #default="scope">{{ purposeLabels[scope.row.purpose as SmsPurpose] }}</template>
        </el-table-column>
        <el-table-column label="短信内容" min-width="360">
          <template #default="scope">
            <div class="content-cell">
              <el-tag v-if="scope.row.sensitiveContentRetained" type="danger" size="small">含验证码明文</el-tag>
              <span class="message-content">{{ scope.row.content }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="策略" min-width="160" prop="providerCode" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabels[scope.row.status as SmsDeliveryStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失败说明" min-width="160">
          <template #default="scope">{{ scope.row.failureMessage || '—' }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          background
          layout="sizes, prev, pager, next, total"
          :page-sizes="[20, 50, 100]"
          :page-size="query.pageSize"
          :current-page="query.pageNo"
          :total="total"
          @current-change="changePage"
          @size-change="changeSize"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.sms-page { display: grid; gap: 18px; }
.page-heading p { margin: 0 0 10px; color: #6d48c4; font-size: 12px; font-weight: 800; letter-spacing: .1em; }
.page-heading h2 { margin: 0 0 8px; font-size: 32px; }
.page-heading span { color: #7e7785; }
.query-card { padding: 14px 16px; display: grid; grid-template-columns: minmax(220px, 1fr) 210px 210px auto; gap: 10px; align-items: center; border: 1px solid #e0e1dc; border-radius: 14px; background: #fff; }
.date-input { width: 100%; height: 32px; padding: 0 10px; color: #55505b; border: 1px solid #dcdfe6; border-radius: 4px; background: #fff; }
.query-actions { display: flex; white-space: nowrap; }
.table-card { overflow: hidden; border: 1px solid #e0e1dc; border-radius: 14px; background: #fff; }
.table-meta { padding: 13px 16px; display: flex; align-items: baseline; gap: 12px; border-bottom: 1px solid #ebece8; }
.table-meta span { color: #8b8590; font-size: 13px; }
.message-content { white-space: pre-wrap; word-break: break-word; }
.content-cell { display: grid; justify-items: start; gap: 6px; }
.pagination { padding: 16px; display: flex; justify-content: flex-end; border-top: 1px solid #ebece8; }
@media (max-width: 1100px) { .query-card { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 760px) { .query-card { grid-template-columns: 1fr; } .page-heading h2 { font-size: 25px; } }
</style>
