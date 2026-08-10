<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppBreadcrumb, type BreadcrumbItem } from '@canteen-smile/ui'
import { usePlatformSessionStore } from '@/app/store/platformSession'
import { logoutCurrentSession } from '@/modules/auth/api/authApi'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'
import { clearPlatformToken } from '@/shared/http/client'

const route = useRoute()
const router = useRouter()
const platformSession = usePlatformSessionStore()

/** 根据嵌套路由生成可点击的后台面包屑。 */
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
  typeof route.meta.title === 'string' ? route.meta.title : '平台管理端',
)

onMounted(async () => {
  try {
    await platformSession.load()
  } catch {
    platformSession.clear()
    clearPlatformToken()
    await router.replace({ name: 'login' })
  }
})

const logoutFlight = useSingleFlight(async () => {
  try {
    await logoutCurrentSession()
  } catch {
    // 即使服务端会话已经失效，本地 Token 仍必须清理。
  } finally {
    platformSession.clear()
    clearPlatformToken()
    feedback.success('当前设备已退出')
    await router.replace({ name: 'login' })
  }
})
</script>

<template>
  <div class="platform-shell" v-loading="platformSession.loading">
    <aside class="platform-sidebar">
      <div class="brand"><span>CS</span><strong>Canteen Smile</strong></div>
      <p class="workspace-label">平台管理</p>
      <nav aria-label="平台管理端主导航">
        <RouterLink :class="{ active: route.name === 'home' }" :to="{ name: 'home' }"><span>◫</span>租户治理</RouterLink>
        <RouterLink :class="{ active: route.name === 'org-type-templates' }" :to="{ name: 'org-type-templates' }"><span>⌘</span>机构类型模板</RouterLink>
        <RouterLink :class="{ active: route.name === 'permission-resources' }" :to="{ name: 'permission-resources' }"><span>◆</span>权限资源</RouterLink>
        <span class="disabled"><b>◇</b>平台身份</span>
        <RouterLink :class="{ active: route.name === 'platform-audit' }" :to="{ name: 'platform-audit' }"><span>◎</span>平台审计</RouterLink>
        <span class="disabled"><b>⚙</b>安全配置</span>
      </nav>
      <div class="boundary-note">
        <span class="boundary-dot" />
        <div><strong>平台身份边界</strong><small>不属于任何租户或机构</small></div>
      </div>
    </aside>

    <main class="platform-main">
      <header class="platform-header">
        <div class="page-identity">
          <AppBreadcrumb :items="breadcrumbItems" @navigate="router.push" />
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="account-actions">
          <div class="avatar">P</div>
          <div><strong>平台超级管理员</strong><small>ID {{ platformSession.session?.accountId || '—' }}</small></div>
          <el-button text :loading="logoutFlight.pending.value" @click="logoutFlight.run()">退出</el-button>
        </div>
      </header>
      <section class="platform-content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<style scoped>
.platform-shell { min-height: 100vh; display: grid; grid-template-columns: 244px minmax(0, 1fr); color: #242129; background: #f3f4f1; }
.platform-sidebar { position: sticky; top: 0; height: 100vh; padding: 28px 20px; display: flex; flex-direction: column; color: #eeeaf5; background: #211a2d; }
.brand { display: flex; align-items: center; gap: 12px; }
.brand > span { width: 38px; height: 38px; display: grid; place-items: center; border: 1px solid #86749e; border-radius: 12px; font-size: 12px; }
.workspace-label { margin: 30px 8px 12px; color: #8d7ca3; font-size: 10px; letter-spacing: .12em; }
nav { display: grid; gap: 6px; }
nav a, nav .disabled { min-height: 44px; padding: 12px 14px; display: flex; align-items: center; gap: 12px; color: #a9a0b3; border-radius: 10px; text-decoration: none; }
nav a.active { color: #fff; background: #6d48c4; }
nav .disabled { cursor: not-allowed; opacity: .65; }
nav .disabled b { font-weight: 400; }
.boundary-note { margin-top: auto; padding: 16px; display: flex; gap: 12px; align-items: flex-start; border: 1px solid #40364f; border-radius: 14px; background: #2a2237; }
.boundary-note div { display: grid; gap: 5px; }
.boundary-note small { color: #8f849c; line-height: 1.5; }
.boundary-dot { width: 8px; height: 8px; margin-top: 5px; flex: none; border-radius: 99px; background: #5cdcaa; }
.platform-main { min-width: 0; }
.platform-header { position: sticky; top: 0; z-index: 20; min-height: 92px; padding: 18px 36px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #e3e4df; background: rgba(255,255,255,.94); backdrop-filter: blur(12px); }
.page-identity { display: grid; gap: 4px; }
.page-identity h1 { margin: 0; font-size: 22px; }
.account-actions { display: flex; align-items: center; gap: 11px; }
.account-actions > div:nth-child(2) { display: grid; gap: 3px; }
.account-actions small { color: #8c8691; font-size: 11px; }
.avatar { width: 38px; height: 38px; display: grid; place-items: center; color: #fff; border-radius: 12px; background: #6d48c4; font-weight: 700; }
.platform-content { min-width: 0; padding: 36px; }
@media (max-width: 760px) {
  .platform-shell { grid-template-columns: 1fr; }
  .platform-sidebar { display: none; }
  .platform-header, .platform-content { padding-left: 18px; padding-right: 18px; }
  .account-actions > div:nth-child(2) { display: none; }
}
</style>
