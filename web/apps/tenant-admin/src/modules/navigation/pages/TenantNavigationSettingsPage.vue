<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { reauthenticatePassword } from '@/modules/auth/api/authApi'
import { encryptPassword } from '@/modules/auth/passwordEnvelope'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { feedback } from '@/shared/feedback'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import {
  getTenantNavigationSettings,
  updateTenantFeature,
  updateTenantMenu,
} from '../api/navigationApi'
import type { TenantFeature, TenantMenuSetting, TenantNavigationSettings } from '../types'

type PendingChange =
  | { type: 'feature'; target: TenantFeature; nextValue: boolean }
  | { type: 'menu'; target: TenantMenuSetting; nextValue: boolean }

const tenantContext = useTenantContextStore()
const loading = ref(false)
const settings = ref<TenantNavigationSettings>({ features: [], menus: [] })
const dialogVisible = ref(false)
const pendingChange = ref<PendingChange | null>(null)
const form = reactive({ reason: '', currentPassword: '' })
const navigationRecoveryPermission = 'iam:tenant-navigation:view'

function isRecoveryMenu(menu: TenantMenuSetting): boolean {
  return menu.permissionCode === navigationRecoveryPermission
}

async function load(): Promise<void> {
  loading.value = true
  try {
    settings.value = await getTenantNavigationSettings()
  } finally {
    loading.value = false
  }
}

function openFeature(feature: TenantFeature, enabled: boolean): void {
  pendingChange.value = { type: 'feature', target: feature, nextValue: enabled }
  Object.assign(form, { reason: '', currentPassword: '' })
  dialogVisible.value = true
}

function openMenu(menu: TenantMenuSetting, hidden: boolean): void {
  pendingChange.value = { type: 'menu', target: menu, nextValue: hidden }
  Object.assign(form, { reason: '', currentPassword: '' })
  dialogVisible.value = true
}

const saveFlight = useSingleFlight(async () => {
  if (!pendingChange.value || !form.reason.trim() || !form.currentPassword) {
    feedback.warning('请填写修改原因并输入当前登录密码')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(form.currentPassword, 'TENANT_REAUTH_PASSWORD')
    const ticket = await reauthenticatePassword({
      passwordEnvelope,
      allowedAction: 'TENANT_NAVIGATION_UPDATE',
    })
    if (pendingChange.value.type === 'feature') {
      const command = {
        version: pendingChange.value.target.version,
        reauthTicket: ticket.reauthTicket,
        reason: form.reason.trim(),
      }
      settings.value = await updateTenantFeature(
        pendingChange.value.target.featureCode,
        pendingChange.value.nextValue,
        command,
      )
      feedback.success('租户功能状态已保存；全部账号将重新登录以刷新权限')
    } else {
      const command = {
        version: pendingChange.value.target.tenantVersion,
        reauthTicket: ticket.reauthTicket,
        reason: form.reason.trim(),
      }
      settings.value = await updateTenantMenu(
        pendingChange.value.target.permissionCode,
        pendingChange.value.nextValue,
        command,
      )
      feedback.success('租户统一菜单显示已保存')
    }
    dialogVisible.value = false
    await tenantContext.load()
  } finally {
    form.currentPassword = ''
  }
})

onMounted(load)
</script>

<template>
  <section class="page" v-loading="loading">
    <div class="page-lead">
      <div>
        <p class="eyebrow">TENANT GOVERNANCE / NAVIGATION</p>
        <h2>功能停用控制权限，菜单隐藏只控制显示。</h2>
        <p>功能停用会让受影响账号全部设备下线；菜单隐藏不会改变任何后端接口权限。</p>
      </div>
    </div>

    <div class="panel section-panel">
      <div class="panel-title"><div><h3>租户功能</h3><p>关闭后租户内全部机构立即失去对应功能权限。</p></div></div>
      <div v-for="feature in settings.features" :key="feature.featureCode" class="setting-row">
        <div><strong>{{ feature.name }}</strong><small>{{ feature.description }} · {{ feature.featureCode }}</small></div>
        <el-switch
          :model-value="feature.enabled"
          :disabled="!tenantContext.hasPermission('iam:tenant-navigation:manage')"
          @change="openFeature(feature, Boolean($event))"
        />
      </div>
    </div>

    <div class="panel section-panel">
      <div class="panel-title"><div><h3>租户统一菜单</h3><p>父菜单隐藏时，其全部子菜单会随之隐藏。</p></div></div>
      <div v-for="menu in settings.menus" :key="menu.permissionCode" class="setting-row">
        <div>
          <strong>{{ menu.name }}</strong>
          <small>{{ menu.permissionCode }}<template v-if="menu.featureCode"> · {{ menu.featureCode }}</template></small>
        </div>
        <div class="row-control">
          <el-tag v-if="isRecoveryMenu(menu)" type="info">恢复入口，始终显示</el-tag>
          <el-tag v-if="!menu.featureEnabled" type="warning">功能已停用</el-tag>
          <span>{{ menu.tenantHidden ? '已隐藏' : '显示' }}</span>
          <el-switch
            :model-value="!menu.tenantHidden"
            :disabled="isRecoveryMenu(menu) || !tenantContext.hasPermission('iam:tenant-navigation:manage')"
            @change="openMenu(menu, !Boolean($event))"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="确认敏感配置变更" width="560px" :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" title="本次操作将记录完整审计日志，需要填写原因并使用当前密码再认证。" />
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="修改原因"><el-input v-model="form.reason" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item label="当前登录密码"><el-input v-model="form.currentPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saveFlight.pending.value" :disabled="saveFlight.pending.value" @click="saveFlight.run()">确认保存</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page{padding:36px}.page-lead{margin-bottom:28px}.eyebrow{margin:0 0 10px;color:#168f89;font-size:11px;font-weight:700;letter-spacing:.14em}.page-lead h2{margin:0;font-size:34px}.page-lead p:last-child{color:#748281}.panel{border:1px solid #dce5e1;border-radius:16px;background:#fff}.section-panel{margin-bottom:20px;padding:0 24px}.panel-title{padding:22px 0 18px;border-bottom:1px solid #e7eeea}.panel-title h3,.panel-title p{margin:0}.panel-title p{margin-top:6px;color:#82908f}.setting-row{min-height:76px;display:flex;align-items:center;justify-content:space-between;gap:20px;border-bottom:1px solid #edf1ef}.setting-row:last-child{border-bottom:0}.setting-row strong,.setting-row small{display:block}.setting-row small{margin-top:6px;color:#879593}.row-control{display:flex;align-items:center;gap:12px;color:#6f7d7b}.dialog-form{margin-top:18px}@media(max-width:760px){.page{padding:20px}.page-lead h2{font-size:28px}}
</style>
