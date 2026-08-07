<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { completePasswordReset, getPasswordResetContext } from '../api/authApi'
import { encryptPassword, PasswordEnvelopeError } from '../passwordEnvelope'
import type { PasswordResetContext } from '../types'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

const route = useRoute()
const router = useRouter()
const ticket = computed(() => typeof route.query.ticket === 'string' ? route.query.ticket : '')
const context = ref<PasswordResetContext | null>(null)
const loading = ref(true)
const invalid = ref(false)
const form = reactive({ password: '', confirmPassword: '' })
const ready = computed(() => form.password.length >= 8 && form.password === form.confirmPassword)

onMounted(async () => {
  if (!ticket.value) {
    invalid.value = true
    loading.value = false
    return
  }
  try {
    context.value = await getPasswordResetContext(ticket.value)
  } catch {
    invalid.value = true
  } finally {
    loading.value = false
  }
})

const resetFlight = useSingleFlight(async () => {
  if (!ready.value) {
    feedback.warning('请确认两次密码一致，且密码至少 8 位')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(form.password, 'TENANT_PASSWORD_RESET')
    form.password = ''
    form.confirmPassword = ''
    const result = await completePasswordReset(ticket.value, passwordEnvelope)
    feedback.success('密码重置成功，请使用新密码登录')
    await router.replace({ name: 'login', query: { username: result.username } })
  } catch (error) {
    if (error instanceof PasswordEnvelopeError) feedback.error(error.message)
  }
})
</script>

<template>
  <main class="reset-shell">
    <section class="reset-card">
      <p class="eyebrow">CANTEEN SMILE · PASSWORD RECOVERY</p>
      <h1>重置租户账号密码</h1>
      <el-skeleton v-if="loading" :rows="5" animated />
      <el-result v-else-if="invalid" icon="error" title="恢复链接不可用" sub-title="链接可能已过期、已使用或被新链接替代。" />
      <template v-else-if="context">
        <div class="identity-context">
          <strong>{{ context.displayName }}</strong>
          <span>{{ context.username }}</span>
          <small>{{ context.tenantName }} / {{ context.organizationName }}</small>
        </div>
        <el-alert type="warning" :closable="false" title="设置成功后此链接立即失效；所有旧设备会话均已退出。" />
        <form @submit.prevent="resetFlight.run()">
          <label><span>设置新密码</span><el-input v-model="form.password" type="password" show-password autocomplete="new-password" maxlength="128" size="large" /></label>
          <label><span>确认新密码</span><el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" maxlength="128" size="large" /></label>
          <p class="policy">至少 8 位，满足数字、大写字母、小写字母、特殊字符中的三类，不能包含用户名，也不能与当前或最近五次密码相同。</p>
          <el-button native-type="submit" type="primary" size="large" :disabled="!ready" :loading="resetFlight.pending.value">设置新密码</el-button>
        </form>
      </template>
    </section>
  </main>
</template>

<style scoped>
.reset-shell { min-height: 100vh; padding: 32px 18px; display: grid; place-items: center; background: radial-gradient(circle at 20% 15%, #dff5f4, transparent 35%), #f4f7f5; }
.reset-card { width: min(100%, 560px); padding: 42px; border: 1px solid #dce5e1; border-radius: 24px; background: rgba(255,255,255,.94); box-shadow: 0 28px 70px rgba(23,67,75,.1); }
.eyebrow { color: #176b8c; font-size: 11px; font-weight: 700; letter-spacing: .14em; }
h1 { margin: 8px 0 28px; font-size: 32px; }
.identity-context { margin-bottom: 18px; padding: 18px; display: grid; gap: 6px; border-radius: 14px; background: #eef7f7; }
.identity-context span, .identity-context small, .policy { color: #6f7a7a; }
form { margin-top: 22px; display: grid; gap: 18px; }
label { display: grid; gap: 8px; font-size: 14px; font-weight: 600; }
.policy { margin: -4px 0 0; font-size: 12px; line-height: 1.7; }
</style>
