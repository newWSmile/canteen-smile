/** 租户管理端启动上下文。 */
export interface TenantManagementContext {
  accountId: string
  username: string
  displayName: string | null
  tenantId: string
  tenantName: string
  organizationId: string
  organizationName: string
  rootOrganizationId: string
  rootOwner: boolean
  permissions: string[]
}
