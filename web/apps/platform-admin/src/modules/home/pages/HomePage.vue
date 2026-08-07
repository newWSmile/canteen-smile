<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentSession, logoutCurrentSession } from '@/modules/auth/api/authApi'
import type { Session } from '@/modules/auth/types'
import { pagePlatformTenants } from '@/modules/tenant/api/tenantApi'
import type { TenantStatus, TenantSummary } from '@/modules/tenant/types'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'
import { clearPlatformToken } from '@/shared/http/client'

const router = useRouter()
const session = ref<Session | null>(null)
const tenants = ref<TenantSummary[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = 20
const status = ref<TenantStatus | ''>('')
const loading = ref(false)

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
    session.value = await getCurrentSession()
    await loadTenants()
  } catch {
    clearPlatformToken()
    await router.replace({ name: 'login' })
  }
}

const logoutFlight = useSingleFlight(async () => {
  try {
    await logoutCurrentSession()
  } catch {
    // 即使服务端会话已经失效，本地 Token 仍必须清理。
  } finally {
    clearPlatformToken()
    feedback.success('当前设备已退出')
    await router.replace({ name: 'login' })
  }
})

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

function changePage(nextPage: number): void {
  pageNo.value = nextPage
  void loadTenants()
}

function changeStatus(): void {
  pageNo.value = 1
  void loadTenants()
}

onMounted(() => void initialize())
</script>

<template>
  <div class="workspace-shell">
    <aside class="sidebar">
      <div class="brand"><span>CS</span><strong>Canteen Smile</strong></div>
      <p class="workspace-label">PLATFORM ADMINISTRATION</p>
      <nav>
        <a class="active" href="#"><span>◫</span>租户治理</a>
        <a class="disabled" href="#"><span>◇</span>平台身份</a>
        <a class="disabled" href="#"><span>◎</span>平台审计</a>
        <a class="disabled" href="#"><span>⚙</span>安全配置</a>
      </nav>
      <div class="boundary-note">
        <span class="boundary-dot" />
        <div><strong>平台身份边界</strong><small>不属于任何租户或机构</small></div>
      </div>
    </aside>

    <main class="main-area">
      <header class="topbar">
        <div>
          <p>平台治理 / 租户</p>
          <h1>租户治理</h1>
        </div>
        <div class="identity">
          <div class="avatar">P</div>
          <div><strong>平台超级管理员</strong><small>ID {{ session?.accountId || '—' }}</small></div>
          <el-button text :loading="logoutFlight.pending.value" @click="logoutFlight.run()">退出</el-button>
        </div>
      </header>

      <section class="content">
        <div class="intro-row">
          <div>
            <p class="eyebrow">TENANT GOVERNANCE</p>
            <h2>跨租户只做治理，不进入租户业务。</h2>
            <p>当前数据由 IAM 实时分页查询，权限在 Gateway 与 IAM 服务双重校验。</p>
          </div>
          <el-button disabled size="large" type="primary">创建租户（下一阶段）</el-button>
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
                <div class="tenant-name"><span>{{ scope.row.name.slice(0, 1) }}</span><div><strong>{{ scope.row.name }}</strong><small>{{ scope.row.tenantCode }}</small></div></div>
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
    </main>
  </div>
</template>

<style scoped>
.workspace-shell { min-height: 100vh; display: grid; grid-template-columns: 244px 1fr; color: #242129; background: #f3f4f1; }
.sidebar { padding: 28px 20px; display: flex; flex-direction: column; color: #eeeaf5; background: #211a2d; }
.brand { display: flex; align-items: center; gap: 12px; }
.brand > span { width: 38px; height: 38px; display: grid; place-items: center; border: 1px solid #86749e; border-radius: 12px; font-size: 12px; }
.workspace-label { margin: 30px 8px 12px; color: #8d7ca3; font-size: 10px; letter-spacing: .12em; }
nav { display: grid; gap: 6px; }
nav a { padding: 12px 14px; display: flex; gap: 12px; color: #a9a0b3; border-radius: 10px; text-decoration: none; }
nav a.active { color: #fff; background: #6d48c4; }
nav a.disabled { cursor: not-allowed; opacity: .65; }
.boundary-note { margin-top: auto; padding: 16px; display: flex; gap: 12px; align-items: flex-start; border: 1px solid #40364f; border-radius: 14px; background: #2a2237; }
.boundary-note div { display: grid; gap: 5px; }
.boundary-note small { color: #8f849c; line-height: 1.5; }
.boundary-dot { width: 8px; height: 8px; margin-top: 5px; flex: none; border-radius: 99px; background: #5cdcaa; }
.main-area { min-width: 0; }
.topbar { min-height: 92px; padding: 20px 36px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #e3e4df; background: rgba(255,255,255,.78); }
.topbar p { margin: 0 0 4px; color: #908a95; font-size: 12px; }
.topbar h1 { margin: 0; font-size: 22px; }
.identity { display: flex; align-items: center; gap: 11px; }
.identity > div:nth-child(2) { display: grid; gap: 3px; }
.identity small { color: #8c8691; font-size: 11px; }
.avatar { width: 38px; height: 38px; display: grid; place-items: center; color: #fff; border-radius: 12px; background: #6d48c4; font-weight: 700; }
.content { padding: 36px; }
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
.pagination-row { padding: 20px 24px; display: flex; justify-content: flex-end; border-top: 1px solid #ecece8; }
@media (max-width: 1040px) { .metric-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 760px) { .workspace-shell { grid-template-columns: 1fr; } .sidebar { display: none; } .topbar, .content { padding-left: 18px; padding-right: 18px; } .identity > div:nth-child(2) { display: none; } .intro-row { align-items: flex-start; flex-direction: column; } .metric-grid { grid-template-columns: 1fr; } }
</style>
