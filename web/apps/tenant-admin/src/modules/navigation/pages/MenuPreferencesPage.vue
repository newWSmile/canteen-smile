<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { feedback } from '@/shared/feedback'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { getMenuPreferences, updateMenuPreference } from '../api/navigationApi'
import type { TenantMenuSetting } from '../types'

const tenantContext = useTenantContextStore()
const loading = ref(false)
const menus = ref<TenantMenuSetting[]>([])

async function load(): Promise<void> {
  loading.value = true
  try { menus.value = await getMenuPreferences() } finally { loading.value = false }
}

const saveFlight = useSingleFlight(async (menu: TenantMenuSetting, visible: boolean) => {
  menus.value = await updateMenuPreference(menu, !visible)
  await tenantContext.load()
  feedback.success('个人菜单偏好已保存')
})

onMounted(load)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="page-lead">
      <p class="eyebrow">PERSONAL SETTINGS / MENU</p>
      <h2>只整理自己的导航，不改变任何权限。</h2>
      <p>这里只展示当前账号确实拥有访问权限的菜单；租户统一隐藏或功能已停用的菜单不会在这里出现。</p>
    </div>
    <div class="panel">
      <div v-for="menu in menus" :key="menu.permissionCode" class="setting-row">
        <div><strong>{{ menu.name }}</strong><small>{{ menu.permissionCode }}</small></div>
        <div class="row-control">
          <el-tag v-if="menu.tenantHidden" type="info">租户已隐藏</el-tag>
          <el-tag v-else-if="!menu.featureEnabled" type="warning">功能已停用</el-tag>
          <el-switch
            :model-value="!menu.personallyHidden"
            :disabled="menu.tenantHidden || !menu.featureEnabled || saveFlight.pending.value"
            @change="saveFlight.run(menu, Boolean($event))"
          />
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page{padding:36px}.page-lead{margin-bottom:28px}.eyebrow{margin:0 0 10px;color:#168f89;font-size:11px;font-weight:700;letter-spacing:.14em}.page-lead h2{margin:0;font-size:34px}.page-lead p:last-child{color:#748281}.panel{padding:0 24px;border:1px solid #dce5e1;border-radius:16px;background:#fff}.setting-row{min-height:76px;display:flex;align-items:center;justify-content:space-between;gap:20px;border-bottom:1px solid #edf1ef}.setting-row:last-child{border-bottom:0}.setting-row strong,.setting-row small{display:block}.setting-row small{margin-top:6px;color:#879593}.row-control{display:flex;align-items:center;gap:12px}@media(max-width:760px){.page{padding:20px}.page-lead h2{font-size:28px}}
</style>
