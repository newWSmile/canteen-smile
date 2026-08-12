/** 平台管理端真实设备请求契约。 */
export interface DeviceRequest {
  deviceId: string
  deviceType: string
  deviceName: string
  userAgentSummary?: string
}

/** 密码信封绑定的后端业务用途。 */
export type PasswordEnvelopePurpose =
  | 'PLATFORM_BOOTSTRAP'
  | 'PLATFORM_PASSWORD_LOGIN'
  | 'PLATFORM_REAUTH_PASSWORD'

/** Auth 使用轮换公钥签发的一次性短期挑战。 */
export interface PasswordEncryptionChallenge {
  purpose: PasswordEnvelopePurpose
  keyId: string
  publicKey: string
  nonce: string
  timestamp: number
  expiresAt: string
  keyAlgorithm: 'RSA-OAEP-256'
  contentAlgorithm: 'A256GCM'
}

/** 浏览器使用 Web Crypto 生成的密码混合加密信封。 */
export interface PasswordEnvelopeRequest {
  keyId: string
  nonce: string
  timestamp: number
  encryptedKey: string
  iv: string
  ciphertext: string
}

/** 用户名密码登录第一阶段请求。 */
export interface PasswordLoginRequest {
  appCode: 'PLATFORM_ADMIN'
  username: string
  passwordEnvelope: PasswordEnvelopeRequest
  rememberMe: boolean
  device: DeviceRequest
  captchaTicket?: string
}

/** 平台恢复码二次验证请求。 */
export interface PlatformRecoveryLoginRequest {
  secondFactorTicket: string
  recoveryCode: string
}

/** 当前密码登录结果状态；后续高风险场景可重新启用分步骤认证。 */
export type LoginNextStep = 'AUTHENTICATED' | 'SECOND_FACTOR_REQUIRED'

/** 密码登录第一阶段响应。 */
export interface LoginResult {
  nextStep: LoginNextStep
  session?: Session | null
  secondFactorTicket?: string | null
}

/** 平台设备会话。 */
export interface Session {
  tokenName: string
  tokenValue: string
  sessionId: string
  appCode: 'PLATFORM_ADMIN'
  identityType: 'PLATFORM_IDENTITY'
  accountId: string
  tenantId?: null
  organizationId?: null
  idleExpiresAt: string
  absoluteExpiresAt: string
}

/** 首位平台超级管理员引导请求。 */
export interface PlatformBootstrapRequest {
  username: string
  displayName?: string
  passwordEnvelope: PasswordEnvelopeRequest
}

/** 只在首次引导响应展示一次的结果。 */
export interface PlatformBootstrapResult {
  platformIdentityId: string
  username: string
  recoveryCodes: string[]
  nextStep: string
}

/** 平台敏感操作再认证允许的动作。 */
export type ReauthAction =
  | 'TENANT_OWNER_PASSWORD_RESET'
  | 'PLATFORM_SMS_POLICY_UPDATE'
  | 'PLATFORM_TENANT_GOVERNANCE'

/** 平台当前密码再认证请求。 */
export interface PasswordReauthRequest {
  passwordEnvelope: PasswordEnvelopeRequest
  allowedAction: ReauthAction
}

/** 五分钟一次性敏感操作再认证票据。 */
export interface ReauthTicket {
  reauthTicket: string
  expiresAt: string
}
