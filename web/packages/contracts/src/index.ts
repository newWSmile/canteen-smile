/** 后端统一响应协议。字段必须与 server 中 ApiResponse 保持一致。 */
export interface ApiResponse<T> {
  code: string
  message: string
  data: T | null
  timestamp: string
  traceId: string | null
}

/** 后端统一分页协议；bigint 业务 ID 仍由具体契约声明为 string。 */
export interface PageResult<T> {
  items: T[]
  pageNo: number
  pageSize: number
  total: number
}

/** 普通分页允许的最大每页数量。 */
export const MAX_PAGE_SIZE = 100

/** 可查询的审计数据来源。 */
export type AuditSource = 'IAM' | 'AUTH'

/** 审计动作结果。 */
export type AuditResult = 'SUCCESS' | 'FAILURE' | 'DENIED'

/** IAM 聚合并对三个前端稳定输出的审计日志契约。 */
export interface AuditLog {
  id: string
  source: AuditSource
  tenantId: string | null
  operatorType: string
  operatorTypeName: string
  operatorId: string | null
  operatorUsername: string | null
  operatorDisplayName: string | null
  actionCode: string
  actionName: string
  targetType: string | null
  targetTypeName: string
  targetId: string | null
  targetName: string | null
  targetCode: string | null
  result: AuditResult
  reason: string | null
  loginMethod: string | null
  loginMethodName: string | null
  failureReasonCode: string | null
  failureReason: string | null
  maskedMobile: string | null
  deviceSummary: string | null
  ipAddress: string | null
  traceId: string | null
  occurredTime: string
  appCode: string | null
  categoryPath: string[]
  durationMs: number | null
}

/** 当前账号有效设备会话的脱敏视图。 */
export interface DeviceSession {
  sessionId: string
  appCode: 'TENANT_ADMIN' | 'TENANT_PORTAL'
  deviceType: string
  deviceName: string | null
  loginMethod: 'PASSWORD' | 'SMS' | string
  loginIpAddress: string | null
  loginTime: string
  lastActiveTime: string
  idleExpiresAt: string
  absoluteExpiresAt: string
  current: boolean
  version: number
}

/** 审计日志精确筛选与分页参数。 */
export interface AuditLogPageQuery {
  pageNo: number
  pageSize: number
  source: AuditSource
  actionCode?: string
  result?: AuditResult
  operatorId?: string
  startTime?: string
  endTime?: string
}

/** 短信投递处理状态。 */
export type SmsDeliveryStatus = 'PROCESSING' | 'ACCEPTED' | 'FAILED'

/** 已确认的短信业务用途。 */
export type SmsPurpose =
  | 'LOGIN'
  | 'ACTIVATION'
  | 'PASSWORD_RESET'
  | 'MOBILE_BIND'
  | 'MOBILE_CHANGE'
  | 'ADMIN_REAUTH'
  | 'PLATFORM_SECOND_FACTOR'

/** 平台短信发送记录安全展示契约。 */
export interface SmsDeliveryRecord {
  id: string
  requestId: string
  challengeId: string | null
  providerCode: string
  purpose: SmsPurpose
  maskedMobile: string
  templateCode: string | null
  content: string
  sensitiveContentRetained: boolean
  status: SmsDeliveryStatus
  providerMessageId: string | null
  failureCode: string | null
  failureMessage: string | null
  acceptedTime: string | null
  createdTime: string
}

/** 平台短信发送记录精确筛选与分页参数。 */
export interface SmsDeliveryPageQuery {
  pageNo: number
  pageSize: number
  mobile?: string
  startTime?: string
  endTime?: string
}

/** 平台当前生效的短信验证码、限流和安全设置。 */
export interface SmsRuntimePolicy {
  challengeTtlSeconds: number
  resendIntervalSeconds: number
  maxVerificationAttempts: number
  mobileHourlyLimit: number
  mobileDailyLimit: number
  ipHourlyLimit: number
  ipDailyLimit: number
  deviceHourlyLimit: number
  deviceDailyLimit: number
  plaintextCodeRetentionEnabled: boolean
  updatedTime: string | null
  version: number
}

/** Auth 创建的短信验证码挑战安全摘要。 */
export interface SmsChallenge {
  challengeId: string
  maskedMobile: string
  expiresAt: string
  resendAt: string
}

/** 当前账号已验证手机号绑定状态。 */
export interface MobileBindingStatus {
  bound: boolean
  maskedMobile: string | null
  verifiedTime: string | null
}

/** 当前账号首次绑定手机号挑战请求。 */
export interface MobileBindingChallengeRequest {
  mobile: string
  deviceId: string
  captchaTicket?: string
}

/** 当前账号首次绑定手机号确认请求。 */
export interface MobileBindingConfirmRequest {
  mobile: string
  challengeId: string
  code: string
}

/** 向当前已验证手机号发送敏感操作验证码的请求。 */
export interface CurrentMobileChallengeRequest {
  deviceId: string
  captchaTicket?: string
}

/** 当前手机号验证码可签发的单用途再认证动作。 */
export type MobileSecurityAction = 'MOBILE_CHANGE' | 'MOBILE_UNBIND'

/** 使用当前手机号验证码取得单用途再认证票据的请求。 */
export interface CurrentMobileVerificationRequest {
  challengeId: string
  code: string
  allowedAction: MobileSecurityAction
}

/** 完成换绑再认证后创建新手机号验证码挑战的请求。 */
export interface MobileChangeChallengeRequest {
  reauthTicket: string
  mobile: string
  deviceId: string
  captchaTicket?: string
}

/** 使用再认证票据和新手机号验证码完成换绑的请求。 */
export interface MobileChangeConfirmRequest {
  reauthTicket: string
  newMobile: string
  newChallengeId: string
  newCode: string
}

/** 使用单用途再认证票据完成手机号解绑的请求。 */
export interface MobileUnbindConfirmRequest {
  reauthTicket: string
}

/** 修改短信限流设置的敏感命令。 */
export interface SmsRateLimitSettingsUpdateRequest {
  challengeTtlSeconds: number
  resendIntervalSeconds: number
  maxVerificationAttempts: number
  mobileHourlyLimit: number
  mobileDailyLimit: number
  ipHourlyLimit: number
  ipDailyLimit: number
  deviceHourlyLimit: number
  deviceDailyLimit: number
  version: number
  reauthTicket: string
  reason: string
}

/** 修改短信明文留存安全开关的敏感命令。 */
export interface SmsSecuritySettingsUpdateRequest {
  plaintextCodeRetentionEnabled: boolean
  version: number
  reauthTicket: string
  reason: string
}
