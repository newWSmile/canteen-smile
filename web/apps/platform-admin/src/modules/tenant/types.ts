/** IAM 已发布的租户生命周期状态。 */
export type TenantStatus = 'INITIALIZING' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED' | 'CANCELLED'

/** IAM 已发布的租户初始化编排状态。 */
export type TenantProvisionStatus = 'INITIALIZING' | 'ACTIVE' | 'PROVISION_FAILED'

/** IAM 已发布的租户账号生命周期状态。 */
export type AccountStatus =
  | 'PENDING_ACTIVATION'
  | 'ACTIVE'
  | 'PASSWORD_RESET_REQUIRED'
  | 'DISABLED'
  | 'CANCELLED'

/** 平台租户分页摘要。 */
export interface TenantSummary {
  id: string
  tenantCode: string
  name: string
  status: TenantStatus
  rootOrganizationId: string | null
  securityVersion: number
  templateVersion: number
  provisionStatus: TenantProvisionStatus
  ownerUsername: string | null
  ownerAccountStatus: AccountStatus | null
  createdTime: string
  version: number
}

/** 平台租户分页查询。 */
export interface TenantPageQuery {
  pageNo: number
  pageSize: number
  status?: TenantStatus
}

/** 平台修改租户显示名称的乐观锁请求。 */
export interface UpdatePlatformTenantRequest {
  name: string
  version: number
}

/** 平台执行租户生命周期敏感操作的请求。 */
export interface PlatformTenantStatusRequest {
  reauthTicket: string
  reason: string
  version: number
}

/** 已发布机构类型模板条目。 */
export interface OrgTypeTemplateItem {
  typeCode: string
  name: string
  sortOrder: number
}

/** 已发布机构类型允许父子关系。 */
export interface OrgTypeTemplateRelation {
  parentTypeCode: string
  childTypeCode: string
}

/** 平台机构类型模板版本。 */
export interface OrgTypeTemplate {
  templateVersion: number
  status: 'PUBLISHED'
  types: OrgTypeTemplateItem[]
  relations: OrgTypeTemplateRelation[]
}

/** 发布完整不可变模板版本的请求。 */
export interface PublishOrgTypeTemplateRequest {
  types: OrgTypeTemplateItem[]
  relations: OrgTypeTemplateRelation[]
}

/** 创建租户时的根机构参数。 */
export interface RootOrganizationRequest {
  typeCode: string
  businessCode: string
  name: string
  adminRegionId?: string
}

/** 创建租户时的首位机构所有者参数。 */
export interface TenantOwnerRequest {
  username: string
  displayName?: string
  employeeNumber?: string
}

/** 租户登录与审计安全策略。 */
export interface TenantSecurityPolicyRequest {
  concurrentLoginEnabled: boolean
  maxDevices: number
  rememberMeEnabled: boolean
  idleSeconds: number
  absoluteSeconds: number
  rememberIdleSeconds: number
  rememberAbsoluteSeconds: number
  passwordExpiryEnabled: boolean
  passwordExpiryDays?: number
  auditRetentionDays: number
}

/** 平台五步创建租户请求。 */
export interface CreateTenantRequest {
  tenantCode: string
  name: string
  templateVersion: number
  rootOrganization: RootOrganizationRequest
  owner: TenantOwnerRequest
  securityPolicy: TenantSecurityPolicyRequest
}

/** 租户初始化响应。 */
export interface TenantCreation {
  tenant: TenantSummary
  ownerAccountId: string
  ownerStatus: 'PENDING_ACTIVATION'
}

/** 只在平台签发动作响应中展示一次的租户所有者激活信息。 */
export interface TenantOwnerActivationLink {
  tenantId: string
  accountId: string
  activationTicket: string
  expiresAt: string
}

/** 平台发起租户所有者密码恢复的敏感操作参数。 */
export interface TenantOwnerPasswordResetRequest {
  reauthTicket: string
  reason: string
}

/** 只在平台签发动作响应中展示一次的租户所有者密码恢复信息。 */
export interface TenantOwnerPasswordResetLink {
  tenantId: string
  accountId: string
  resetTicket: string
  expiresAt: string
}
