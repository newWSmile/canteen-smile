<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { bootstrapPlatform } from '../api/authApi'
import type { PlatformBootstrapResult } from '../types'
import { encryptPassword, PasswordEnvelopeError } from '../passwordEnvelope'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'

const form = reactive({
  bootstrapSecret: '',
  username: '',
  displayName: '',
  password: '',
  confirmPassword: '',
})
const result = ref<PlatformBootstrapResult | null>(null)
const ready = computed(
  () =>
    form.bootstrapSecret.length >= 32 &&
    form.username.trim().length > 0 &&
    form.password.length >= 8 &&
    form.confirmPassword.length >= 8,
)

const bootstrapFlight = useSingleFlight(async () => {
  if (!ready.value) {
    feedback.warning('请完整填写引导密钥、用户名和密码')
    return
  }
  if (form.password !== form.confirmPassword) {
    feedback.warning('两次输入的密码不一致')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(form.password, 'PLATFORM_BOOTSTRAP')
    form.password = ''
    form.confirmPassword = ''
    result.value = await bootstrapPlatform(form.bootstrapSecret, {
      username: form.username,
      displayName: form.displayName.trim() || undefined,
      passwordEnvelope,
    })
    form.bootstrapSecret = ''
    feedback.success('首位平台超级管理员创建成功，请立即离线保存恢复码')
  } catch (error) {
    if (error instanceof PasswordEnvelopeError) feedback.error(error.message)
    // Axios 失败仍由统一实例展示后端稳定错误信息。
  }
})

async function copyRecoveryCodes(): Promise<void> {
  if (!result.value) return
  await navigator.clipboard.writeText(result.value.recoveryCodes.join('\n'))
  feedback.success('恢复码已复制，请转存到安全的离线位置')
}
</script>

<template>
  <main class="bootstrap-shell">
    <section class="bootstrap-card">
      <header>
        <RouterLink class="back-link" to="/login">← 返回登录</RouterLink>
        <p class="eyebrow">ONE-TIME PLATFORM BOOTSTRAP</p>
        <h1>首次安全引导</h1>
        <p>
          此入口只在系统不存在平台身份时有效。成功后将永久关闭；用户名会全平台永久保留。
        </p>
      </header>

      <form v-if="!result" class="bootstrap-form" @submit.prevent="bootstrapFlight.run()">
        <label class="full-row">
          <span>高熵引导密钥</span>
          <el-input
            v-model="form.bootstrapSecret"
            autocomplete="off"
            placeholder="必须与服务启动环境变量 PLATFORM_BOOTSTRAP_SECRET 一致"
            show-password
            size="large"
            type="password"
          />
          <small>仅从安全环境变量读取，不会写入数据库或项目文件。</small>
        </label>
        <label>
          <span>平台用户名</span>
          <el-input v-model="form.username" maxlength="128" placeholder="全平台唯一" size="large" />
        </label>
        <label>
          <span>显示名称（可选）</span>
          <el-input v-model="form.displayName" maxlength="128" placeholder="默认使用用户名" size="large" />
        </label>
        <label>
          <span>初始密码</span>
          <el-input
            v-model="form.password"
            autocomplete="new-password"
            maxlength="128"
            placeholder="至少 8 位，满足四类字符中的三类"
            show-password
            size="large"
            type="password"
          />
        </label>
        <label>
          <span>确认密码</span>
          <el-input
            v-model="form.confirmPassword"
            autocomplete="new-password"
            maxlength="128"
            placeholder="再次输入密码"
            show-password
            size="large"
            type="password"
          />
        </label>
        <el-button
          class="create-action full-row"
          native-type="submit"
          size="large"
          type="primary"
          :disabled="!ready"
          :loading="bootstrapFlight.pending.value"
        >
          创建首位平台超级管理员
        </el-button>
      </form>

      <section v-else class="recovery-result">
        <div class="success-mark">✓</div>
        <div>
          <p class="eyebrow">BOOTSTRAP COMPLETED</p>
          <h2>{{ result.username }} 已创建</h2>
          <p>以下恢复码不会再次显示，仅用于受控账号恢复，不会在普通登录时要求输入。</p>
        </div>
        <div class="recovery-grid">
          <code v-for="code in result.recoveryCodes" :key="code">{{ code }}</code>
        </div>
        <div class="result-actions">
          <el-button size="large" @click="copyRecoveryCodes">复制全部恢复码</el-button>
          <RouterLink class="login-action" to="/login">我已安全保存，前往登录</RouterLink>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.bootstrap-shell {
  min-height: 100vh;
  padding: clamp(24px, 6vw, 72px);
  display: grid;
  place-items: center;
  color: #292432;
  background:
    linear-gradient(rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.72)),
    radial-gradient(circle at 15% 15%, #dcd0ff, transparent 35%),
    radial-gradient(circle at 85% 85%, #ccefe0, transparent 32%),
    #f3f2f0;
}

.bootstrap-card {
  width: min(100%, 900px);
  padding: clamp(28px, 5vw, 64px);
  border: 1px solid rgba(77, 62, 100, 0.12);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 32px 90px rgba(52, 42, 70, 0.12);
}

header {
  max-width: 690px;
  margin-bottom: 40px;
}

.back-link {
  display: inline-block;
  margin-bottom: 32px;
  color: #6d48c4;
  text-decoration: none;
}

.eyebrow {
  margin: 0 0 10px;
  color: #6d48c4;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.15em;
}

h1,
h2 {
  margin: 0;
  letter-spacing: -0.035em;
}

h1 {
  font-size: clamp(36px, 5vw, 54px);
}

header > p:last-child,
.recovery-result p {
  color: #77717d;
  line-height: 1.75;
}

.bootstrap-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px;
}

label {
  display: grid;
  gap: 9px;
  font-size: 14px;
  font-weight: 600;
}

small {
  color: #8a8490;
  font-weight: 400;
  line-height: 1.6;
}

.full-row {
  grid-column: 1 / -1;
}

.create-action {
  margin-top: 10px;
  --el-color-primary: #6543bf;
  --el-color-primary-light-3: #8266d0;
  --el-color-primary-dark-2: #51349c;
}

.recovery-result {
  display: grid;
  gap: 26px;
}

.success-mark {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  color: #fff;
  border-radius: 18px;
  background: #30936c;
  font-size: 28px;
}

.recovery-grid {
  padding: 22px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  border: 1px solid #dfd9e8;
  border-radius: 16px;
  background: #f8f6fb;
}

code {
  padding: 10px 12px;
  color: #322743;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  text-align: center;
}

.result-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.login-action {
  padding: 12px 18px;
  color: #fff;
  border-radius: 8px;
  background: #6543bf;
  text-decoration: none;
}

@media (max-width: 680px) {
  .bootstrap-form,
  .recovery-grid {
    grid-template-columns: 1fr;
  }

  .full-row {
    grid-column: auto;
  }
}
</style>
