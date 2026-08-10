<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppBreadcrumb, type BreadcrumbItem } from '@canteen-smile/ui'
import { logoutCurrentSession } from '@/modules/auth/api/authApi'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'
import { clearTenantAdminToken } from '@/shared/http/client'

const route = useRoute()
const router = useRouter()
const tenantContext = useTenantContextStore()

/** 根据嵌套路由生成可点击的租户管理面包屑。 */
const breadcrumbItems = computed<BreadcrumbItem[]>(() => {
  const records = route.matched.filter((record) => typeof record.meta.title === 'string')
  return records.map((record, index) => ({
    label: record.meta.title as string,
    to: index < records.length - 1
      ? record.name
        ? router.resolve({ name: record.name }).path
        : record.path
      : undefined,
  }))
})

const currentTitle = computed(() =>
  typeof route.meta.title === 'string' ? route.meta.title : '租户管理端',
)

onMounted(async () => {
  try {
    if (!tenantContext.context) await tenantContext.load()
  } catch {
    tenantContext.clear()
    clearTenantAdminToken()
    await router.replace({ name: 'login' })
  }
})

const logoutFlight = useSingleFlight(async () => {
  try {
    await logoutCurrentSession()
  } finally {
    tenantContext.clear()
    clearTenantAdminToken()
    feedback.success('当前设备已退出')
    await router.replace({ name: 'login' })
  }
})
</script>

<template>
  <div class="tenant-shell" v-loading="tenantContext.loading">
    <aside class="tenant-sidebar">
      <div class="brand"><span>CS</span><strong>Canteen Smile</strong></div>
      <p class="workspace">租户管理</p>
      <nav aria-label="租户管理端主导航">
        <RouterLink :class="{ active: route.name === 'home' }" :to="{ name: 'home' }">管理概览</RouterLink>
        <RouterLink
          v-if="tenantContext.hasPermission('iam:org-type:view')"
          :class="{ active: route.name === 'organization-types' }"
          :to="{ name: 'organization-types' }"
        >机构类型与关系</RouterLink>
        <RouterLink
          v-if="tenantContext.hasPermission('iam:org:view')"
          :class="{ active: route.name === 'organizations' }"
          :to="{ name: 'organizations' }"
        >机构树</RouterLink>
        <RouterLink
          v-if="tenantContext.hasPermission('iam:role:view')"
          :class="{ active: route.name === 'roles' }"
          :to="{ name: 'roles' }"
        >角色与授权</RouterLink>
        <RouterLink
          v-if="tenantContext.hasPermission('iam:user:view')"
          :class="{ active: route.name === 'users' }"
          :to="{ name: 'users' }"
        >用户管理</RouterLink>
        <RouterLink
          v-if="tenantContext.hasPermission('iam:audit:view')"
          :class="{ active: route.name === 'tenant-audit' }"
          :to="{ name: 'tenant-audit' }"
        >审计日志</RouterLink>
      </nav>
      <div class="boundary">
        <strong>租户隔离边界</strong>
        <small>{{ tenantContext.tenantName }}</small>
        <small>ID {{ tenantContext.context?.tenantId || '—' }}</small>
      </div>
    </aside>
    <main class="tenant-main">
      <header class="tenant-header">
        <div class="page-identity">
          <AppBreadcrumb :items="breadcrumbItems" @navigate="router.push" />
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="account-actions">
          <div><strong>{{ tenantContext.displayName }}</strong><small>{{ tenantContext.context?.username }}</small></div>
          <el-button text :loading="logoutFlight.pending.value" @click="logoutFlight.run()">退出</el-button>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.tenant-shell { min-height: 100vh; display: grid; grid-template-columns: 244px minmax(0,1fr); color: #1c2c31; background: #f3f6f4; }
.tenant-sidebar { position: sticky; top: 0; height: 100vh; padding: 28px 20px; display: flex; flex-direction: column; color: #e8f6f6; background: #123f50; }
.brand { display: flex; gap: 12px; align-items: center; }
.brand span { width: 38px; height: 38px; display: grid; place-items: center; border: 1px solid #5d8896; border-radius: 12px; }
.workspace { margin: 30px 8px 12px; color: #75a7b4; font-size: 10px; letter-spacing: .12em; }
nav { display: grid; gap: 6px; }
nav a, nav .coming { padding: 13px 14px; color: #a8c6cd; border-radius: 10px; text-decoration: none; }
nav a.active { color: #123f50; background: #76d7cf; font-weight: 700; }
nav .coming { opacity: .5; }
.boundary { margin-top: auto; padding: 16px; display: grid; gap: 6px; border: 1px solid #315d6b; border-radius: 14px; }
.boundary small { color: #8eb3bc; overflow: hidden; text-overflow: ellipsis; }
.tenant-main { min-width: 0; }
.tenant-header { position: sticky; top: 0; z-index: 20; min-height: 92px; padding: 18px 36px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #dde5e1; background: rgba(255,255,255,.94); backdrop-filter: blur(12px); }
.page-identity { display: grid; gap: 4px; --breadcrumb-link: #27758b; --breadcrumb-link-hover: #164f61; }
.tenant-header h1 { margin: 0; font-size: 22px; }
.account-actions { display: flex; align-items: center; gap: 20px; }
.account-actions > div { display: grid; text-align: right; }
.account-actions small { color: #82908f; }
@media (max-width: 760px) { .tenant-shell { grid-template-columns: 1fr; } .tenant-sidebar { display: none; } .tenant-header { padding: 18px 20px; } }
</style>
