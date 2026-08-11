/** 密码信封绑定的真实后端用途。 */
export type PasswordEnvelopePurpose =
  | 'TENANT_ACCOUNT_ACTIVATION'
  | 'TENANT_PASSWORD_LOGIN'
  | 'TENANT_PASSWORD_RESET'
  | 'TENANT_REAUTH_PASSWORD'

/** Auth 签发的短期密码加密挑战。 */
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

/** 浏览器使用 Web Crypto 创建的混合加密密码信封。 */
export interface PasswordEnvelopeRequest {
  keyId: string
  nonce: string
  timestamp: number
  encryptedKey: string
  iv: string
  ciphertext: string
}

/** 激活页展示的脱敏账号上下文。 */
export interface ActivationContext {
  username: string
  displayName: string
  tenantName: string
  organizationName: string
  expiresAt: string
}

/** 账号激活完成结果。 */
export interface ActivationCompleteResult {
  username: string
  nextStep: 'LOGIN'
}

/** 密码恢复页展示的脱敏账号上下文。 */
export interface PasswordResetContext {
  username: string
  displayName: string
  tenantName: string
  organizationName: string
  expiresAt: string
}

/** 密码恢复完成结果。 */
export interface PasswordResetCompleteResult {
  username: string
  nextStep: 'LOGIN'
}

/** 租户管理端设备描述。 */
export interface DeviceRequest {
  deviceId: string
  deviceType: string
  deviceName: string
  userAgentSummary?: string
}

/** 租户管理端用户名密码登录请求。 */
export interface PasswordLoginRequest {
  appCode: 'TENANT_ADMIN'
  username: string
  passwordEnvelope: PasswordEnvelopeRequest
  rememberMe: boolean
  device: DeviceRequest
  captchaTicket?: string
}

/** 匿名创建短信验证码挑战请求。 */
export interface SmsChallengeCreateRequest {
  purpose: 'LOGIN' | 'PASSWORD_RESET'
  mobile: string
  deviceId: string
  captchaTicket?: string
}

/** 手机号验证码登录请求。 */
export interface SmsLoginRequest {
  appCode: 'TENANT_ADMIN'
  challengeId: string
  code: string
  rememberMe: boolean
  device: DeviceRequest
}

/** 手机号登录多账号选择请求。 */
export interface AccountSelectionLoginRequest {
  appCode: 'TENANT_ADMIN'
  accountSelectorTicket: string
  accountId: string
  rememberMe: boolean
  device: DeviceRequest
}

/** Auth 返回的租户设备会话。 */
export interface TenantSession {
  tokenName: string
  tokenValue: string
  sessionId: string
  appCode: 'TENANT_ADMIN'
  identityType: 'TENANT_ACCOUNT'
  accountId: string
  tenantId: string
  organizationId: string
  idleExpiresAt: string
  absoluteExpiresAt: string
}

/** 手机号验证后可供用户选择的真实账号摘要。 */
export interface MobileLoginCandidate {
  accountId: string
  tenantName: string
  organizationName: string
  username: string
  displayName: string
  latestLoginTime: string | null
}

/** 密码或短信登录的分步响应。 */
export interface LoginResult {
  nextStep: 'AUTHENTICATED' | 'SECOND_FACTOR_REQUIRED' | 'ACCOUNT_SELECTION_REQUIRED'
  session: TenantSession | null
  secondFactorTicket?: null
  accountSelectorTicket?: string | null
  accountCandidates?: MobileLoginCandidate[]
}

/** 消费 PASSWORD_RESET 用途短信验证码的自助找回请求。 */
export interface SmsPasswordResetVerificationRequest {
  appCode: 'TENANT_ADMIN'
  challengeId: string
  code: string
}

/** 手机号绑定多个账号时选择具体找回账号的请求。 */
export interface SmsPasswordResetAccountSelectionRequest {
  appCode: 'TENANT_ADMIN'
  accountSelectorTicket: string
  accountId: string
}

/** 手机号自助找回密码的真实分步响应。 */
export interface SmsPasswordResetResult {
  nextStep: 'RESET_PASSWORD' | 'ACCOUNT_SELECTION_REQUIRED'
  passwordResetTicket: string | null
  accountSelectorTicket: string | null
  accountCandidates: MobileLoginCandidate[]
}

/** 租户管理员敏感操作再认证允许的动作。 */
export type ReauthAction =
  | 'TENANT_USER_CREATE'
  | 'TENANT_USER_ROLE_ASSIGN'
  | 'MOBILE_CHANGE'
  | 'MOBILE_UNBIND'

/** 当前密码再认证请求。 */
export interface PasswordReauthRequest {
  passwordEnvelope: PasswordEnvelopeRequest
  allowedAction: ReauthAction
}

/** 五分钟有效且只能消费一次的敏感操作票据。 */
export interface ReauthTicket {
  reauthTicket: string
  expiresAt: string
}
