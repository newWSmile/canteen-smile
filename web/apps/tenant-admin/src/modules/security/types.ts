/** 当前租户安全策略。 */
export interface TenantSecurityPolicy {
  tenantId: string
  tenantName: string
  concurrentLoginEnabled: boolean
  maxDevices: number
  rememberMeEnabled: boolean
  idleSeconds: number
  absoluteSeconds: number
  rememberIdleSeconds: number
  rememberAbsoluteSeconds: number
  passwordExpiryEnabled: boolean
  passwordExpiryDays: number | null
  auditRetentionDays: number
  securityVersion: number
  version: number
}

/** 修改租户安全策略的敏感命令。 */
export interface UpdateTenantSecurityPolicyRequest {
  concurrentLoginEnabled: boolean
  maxDevices: number
  rememberMeEnabled: boolean
  idleSeconds: number
  absoluteSeconds: number
  rememberIdleSeconds: number
  rememberAbsoluteSeconds: number
  passwordExpiryEnabled: boolean
  passwordExpiryDays: number | null
  auditRetentionDays: number
  version: number
  reauthTicket: string
  reason: string
}
