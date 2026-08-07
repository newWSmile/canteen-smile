<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { logoutCurrentSession } from '@/modules/auth/api/authApi'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'
import { clearTenantAdminToken } from '@/shared/http/client'

const route = useRoute()
const router = useRouter()
const tenantContext = useTenantContextStore()

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
      <p class="workspace">TENANT ADMINISTRATION</p>
      <nav>
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
        <span class="coming">用户与角色 · 后续阶段</span>
      </nav>
      <div class="boundary">
        <strong>租户隔离边界</strong>
        <small>{{ tenantContext.tenantName }}</small>
        <small>ID {{ tenantContext.context?.tenantId || '—' }}</small>
      </div>
    </aside>
    <main class="tenant-main">
      <header class="tenant-header">
        <div>
          <small>{{ route.meta.breadcrumb || '租户管理' }}</small>
          <h1>{{ route.meta.title || '租户管理端' }}</h1>
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
.tenant-sidebar { padding: 28px 20px; display: flex; flex-direction: column; color: #e8f6f6; background: #123f50; }
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
.tenant-header { min-height: 92px; padding: 20px 36px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #dde5e1; background: #fff; }
.tenant-header small { color: #82908f; } .tenant-header h1 { margin: 4px 0 0; font-size: 22px; }
.account-actions { display: flex; align-items: center; gap: 20px; } .account-actions > div { display: grid; text-align: right; }
@media (max-width: 760px) { .tenant-shell { grid-template-columns: 1fr; } .tenant-sidebar { display: none; } .tenant-header { padding: 18px 20px; } }
</style>
