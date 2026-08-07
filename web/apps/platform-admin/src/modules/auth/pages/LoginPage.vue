<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { passwordLogin } from '../api/authApi'
import { encryptPassword, PasswordEnvelopeError } from '../passwordEnvelope'
import { getOrCreateDeviceId } from '../device'
import { useSingleFlight } from '@/shared/composables/useSingleFlight'
import { feedback } from '@/shared/feedback'
import { savePlatformToken } from '@/shared/http/client'

const router = useRouter()
const passwordForm = reactive({
  username: '',
  password: '',
  rememberMe: false,
})
const passwordReady = computed(
  () => passwordForm.username.trim().length > 0 && passwordForm.password.length > 0,
)

const passwordFlight = useSingleFlight(async () => {
  if (!passwordReady.value) {
    feedback.warning('请输入用户名和密码')
    return
  }
  try {
    const passwordEnvelope = await encryptPassword(
      passwordForm.password,
      'PLATFORM_PASSWORD_LOGIN',
    )
    passwordForm.password = ''
    const result = await passwordLogin({
      appCode: 'PLATFORM_ADMIN',
      username: passwordForm.username,
      passwordEnvelope,
      rememberMe: passwordForm.rememberMe,
      device: {
        deviceId: getOrCreateDeviceId(),
        deviceType: 'WEB',
        deviceName: '平台管理端浏览器',
        userAgentSummary: navigator.userAgent.slice(0, 256),
      },
    })
    if (result.nextStep !== 'AUTHENTICATED' || !result.session) {
      feedback.error('登录响应缺少有效会话')
      return
    }
    savePlatformToken(result.session.tokenValue, passwordForm.rememberMe)
    feedback.success('登录成功')
    await router.replace({ name: 'home' })
  } catch (error) {
    if (error instanceof PasswordEnvelopeError) feedback.error(error.message)
    // Axios 失败仍由统一实例通过 feedback 展示。
  }
})
</script>

<template>
  <main class="auth-shell">
    <section class="brand-panel">
      <div class="brand-mark">CS</div>
      <p class="eyebrow">CANTEEN SMILE · PLATFORM</p>
      <h1>让每一次平台治理，都有边界、有凭据、有审计。</h1>
      <p class="brand-copy">
        平台身份与所有租户账号严格隔离。当前使用用户名和密码建立独立设备会话。
      </p>
      <div class="security-note">
        <span class="security-dot" />
        <span>Argon2id 密码摘要 · RSA-OAEP 密码信封 · 独立设备会话</span>
      </div>
    </section>

    <section class="form-panel">
      <div class="form-card">
        <p class="eyebrow">PLATFORM ADMINISTRATION</p>
        <h2>登录平台管理端</h2>
        <p class="form-intro">使用全平台唯一用户名登录。</p>

        <form class="auth-form" @submit.prevent="passwordFlight.run()">
          <label>
            <span>用户名</span>
            <el-input
              v-model="passwordForm.username"
              autocomplete="username"
              maxlength="64"
              placeholder="请输入平台用户名"
              size="large"
            />
          </label>
          <label>
            <span>密码</span>
            <el-input
              v-model="passwordForm.password"
              autocomplete="current-password"
              maxlength="128"
              placeholder="请输入密码"
              show-password
              size="large"
              type="password"
            />
          </label>
          <el-checkbox v-model="passwordForm.rememberMe">记住我（最长 30 天）</el-checkbox>
          <el-button
            class="primary-action"
            native-type="submit"
            size="large"
            type="primary"
            :disabled="!passwordReady"
            :loading="passwordFlight.pending.value"
          >
            登录
          </el-button>
        </form>

        <p class="bootstrap-link">
          尚未创建首位平台管理员？
          <RouterLink to="/bootstrap">进入一次性初始化</RouterLink>
        </p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(360px, 1.1fr) minmax(420px, 0.9fr);
  background: #f5f6f2;
}

.brand-panel {
  position: relative;
  overflow: hidden;
  padding: clamp(48px, 8vw, 112px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #f8f5ff;
  background:
    radial-gradient(circle at 18% 18%, rgba(223, 213, 255, 0.24), transparent 32%),
    radial-gradient(circle at 88% 82%, rgba(92, 220, 170, 0.18), transparent 26%),
    #251a3d;
}

.brand-panel::after {
  content: '';
  position: absolute;
  inset: 24px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 28px;
  pointer-events: none;
}

.brand-mark {
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  margin-bottom: 48px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 18px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.eyebrow {
  margin: 0 0 16px;
  color: #9e83eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.brand-panel .eyebrow {
  color: #c8b7f8;
}

h1 {
  max-width: 680px;
  margin: 0;
  font-size: clamp(36px, 4.4vw, 66px);
  line-height: 1.12;
  letter-spacing: -0.04em;
}

.brand-copy {
  max-width: 580px;
  margin: 30px 0 50px;
  color: rgba(248, 245, 255, 0.7);
  font-size: 17px;
  line-height: 1.8;
}

.security-note {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(248, 245, 255, 0.68);
  font-size: 13px;
}

.security-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #5cdcaa;
  box-shadow: 0 0 0 6px rgba(92, 220, 170, 0.1);
}

.form-panel {
  padding: 44px;
  display: grid;
  place-items: center;
}

.form-card {
  width: min(100%, 440px);
  padding: 46px;
  border: 1px solid #e4e5df;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 28px 70px rgba(43, 34, 62, 0.09);
}

h2 {
  margin: 0;
  color: #201b2b;
  font-size: 30px;
  letter-spacing: -0.03em;
}

.form-intro {
  margin: 12px 0 32px;
  color: #77727f;
  line-height: 1.7;
}

.auth-form {
  display: grid;
  gap: 20px;
}

label {
  display: grid;
  gap: 8px;
  color: #393441;
  font-size: 14px;
  font-weight: 600;
}

.primary-action {
  width: 100%;
  margin-top: 4px;
  --el-color-primary: #6543bf;
  --el-color-primary-light-3: #8266d0;
  --el-color-primary-dark-2: #51349c;
}

.bootstrap-link {
  margin: -8px 0 0;
  color: #87818d;
  font-size: 12px;
  line-height: 1.6;
}

.bootstrap-link {
  margin-top: 28px;
  text-align: center;
}

.bootstrap-link a {
  color: #6543bf;
  text-decoration: none;
}

@media (max-width: 860px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: 320px;
    padding: 58px 44px;
  }

  .brand-mark,
  .brand-copy {
    margin-bottom: 22px;
  }

  .form-panel {
    padding: 28px 18px 48px;
  }

  .form-card {
    padding: 30px 24px;
  }
}
</style>
