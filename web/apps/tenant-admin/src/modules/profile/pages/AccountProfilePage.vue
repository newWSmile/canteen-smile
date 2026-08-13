<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { reauthenticatePassword } from '@/modules/auth/api/authApi'
import { encryptPassword } from '@/modules/auth/passwordEnvelope'
import { clearTenantAdminToken } from '@/shared/http/client'
import { feedback } from '@/shared/feedback'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { changeCurrentUsername } from '../api/profileApi'

const router = useRouter()
const tenantContext = useTenantContextStore()
const form = reactive({ username: '', reason: '', currentPassword: '' })

const saveFlight = useSingleFlight(async () => {
  if (!/^[A-Za-z0-9][A-Za-z0-9._-]{2,127}$/.test(form.username.trim())) {
    feedback.warning('用户名需为 3 至 128 位字母、数字、点、下划线或短横线')
    return
  }
  if (!form.reason.trim() || !form.currentPassword) {
    feedback.warning('请填写修改原因并输入当前登录密码')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(form.currentPassword, 'TENANT_REAUTH_PASSWORD')
    const ticket = await reauthenticatePassword({
      passwordEnvelope,
      allowedAction: 'TENANT_USERNAME_CHANGE',
    })
    const result = await changeCurrentUsername({
      username: form.username.trim(),
      reauthTicket: ticket.reauthTicket,
      reason: form.reason.trim(),
    })
    tenantContext.clear()
    clearTenantAdminToken()
    feedback.success(`用户名已修改为 ${result.username}，请重新登录`)
    await router.replace({ name: 'login' })
  } finally {
    form.currentPassword = ''
  }
})
</script>

<template>
  <section class="page">
    <div class="page-lead">
      <p class="eyebrow">ACCOUNT CENTER / PROFILE</p>
      <h2>用户名可以修改，但每一个曾用名都永久保留。</h2>
      <p>新用户名全平台唯一且不区分大小写；修改成功后全部设备会话立即失效。</p>
    </div>
    <div class="profile-grid">
      <div class="panel summary">
        <span>当前用户名</span><strong>{{ tenantContext.context?.username }}</strong>
        <span>显示名称</span><strong>{{ tenantContext.displayName }}</strong>
        <span>所属机构</span><strong>{{ tenantContext.context?.organizationName }}</strong>
      </div>
      <div class="panel form-panel">
        <el-alert type="warning" :closable="false" title="旧用户名不能再次用于登录，也不会释放给其他账号。" />
        <el-form label-position="top" class="dialog-form">
          <el-form-item label="新用户名"><el-input v-model="form.username" maxlength="128" autocomplete="off" placeholder="请输入新的全平台唯一用户名" /></el-form-item>
          <el-form-item label="修改原因"><el-input v-model="form.reason" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
          <el-form-item label="当前登录密码"><el-input v-model="form.currentPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
        </el-form>
        <div class="actions"><el-button type="primary" :loading="saveFlight.pending.value" :disabled="saveFlight.pending.value" @click="saveFlight.run()">修改用户名并重新登录</el-button></div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page{padding:36px}.page-lead{margin-bottom:28px}.eyebrow{margin:0 0 10px;color:#168f89;font-size:11px;font-weight:700;letter-spacing:.14em}.page-lead h2{margin:0;font-size:34px}.page-lead p:last-child{color:#748281}.profile-grid{display:grid;grid-template-columns:360px minmax(0,1fr);gap:20px}.panel{padding:24px;border:1px solid #dce5e1;border-radius:16px;background:#fff}.summary{display:grid;gap:8px;align-content:start}.summary span{margin-top:12px;color:#83918f;font-size:13px}.summary strong{font-size:18px}.dialog-form{margin-top:20px}.actions{display:flex;justify-content:flex-end}@media(max-width:900px){.profile-grid{grid-template-columns:1fr}}@media(max-width:760px){.page{padding:20px}.page-lead h2{font-size:28px}}
</style>
