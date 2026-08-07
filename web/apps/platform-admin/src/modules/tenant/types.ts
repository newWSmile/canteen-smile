/** IAM 已发布的租户生命周期状态。 */
export type TenantStatus = 'INITIALIZING' | 'ACTIVE' | 'SUSPENDED' | 'EXPIRED' | 'CANCELLED'

/** IAM 已发布的租户初始化编排状态。 */
export type TenantProvisionStatus = 'INITIALIZING' | 'ACTIVE' | 'PROVISION_FAILED'

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
  createdTime: string
  version: number
}

/** 平台租户分页查询。 */
export interface TenantPageQuery {
  pageNo: number
  pageSize: number
  status?: TenantStatus
}
