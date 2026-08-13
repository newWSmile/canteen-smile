/** 租户账号生命周期状态。 */
export type TenantUserStatus =
  | 'PENDING_ACTIVATION'
  | 'ACTIVE'
  | 'PASSWORD_RESET_REQUIRED'
  | 'DISABLED'
  | 'CANCELLED'

/** 用户当前有效角色摘要。 */
export interface TenantUserRole {
  id: string
  name: string
}

/** 当前机构用户。 */
export interface TenantUser {
  id: string
  username: string
  displayName: string | null
  employeeNumber: string | null
  organizationId: string
  organizationName: string
  status: TenantUserStatus
  validityMode: 'LONG_TERM' | 'FIXED_PERIOD'
  effectiveAt: string | null
  expiresAt: string | null
  roles: TenantUserRole[]
  owner: boolean
  authzVersion: number
  createdTime: string
  version: number
}

/** 创建本机构待激活用户的真实后端契约。 */
export interface CreateTenantUserRequest {
  username: string
  displayName?: string
  employeeNumber?: string
  organizationId: string
  roleIds: string[]
  validityMode: 'LONG_TERM' | 'FIXED_PERIOD'
  effectiveAt?: string
  expiresAt?: string
  reauthTicket: string
  reason: string
}

/** 整版替换用户角色请求。 */
export interface ReplaceTenantUserRolesRequest {
  roleIds: string[]
  reauthTicket: string
  reason: string
  version: number
}

/** 修改用户显示资料和有效期请求。 */
export interface UpdateTenantUserRequest {
  displayName?: string
  employeeNumber?: string
  validityMode: 'LONG_TERM' | 'FIXED_PERIOD'
  effectiveAt?: string
  expiresAt?: string
  reason: string
  version: number
}

/** 只展示一次的激活票据。 */
export interface TenantUserActivationLink {
  activationTicket: string
  expiresAt: string
}

/** 管理员生成的一次性密码重置票据。 */
export interface TenantUserPasswordResetLink {
  resetTicket: string
  expiresAt: string
}

/** 当前机构所有者摘要。 */
export interface OrganizationOwner {
  organizationId: string
  accountId: string
  username: string
  displayName: string
  effectiveTime: string
  version: number
}
