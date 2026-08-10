/** 支持的数据范围类型。 */
export type DataScopeType = 'SELF' | 'CURRENT_ORG' | 'CURRENT_ORG_AND_DESCENDANTS' | 'SPECIFIED_ORGS' | 'SPECIFIED_ORGS_AND_DESCENDANTS' | 'TENANT_ALL'

/** 当前机构角色。 */
export interface Role {
  id: string
  roleCode: string
  name: string
  description: string | null
  roleType: 'OWNER' | 'CUSTOM'
  status: 'ACTIVE' | 'DISABLED'
  authzVersion: number
  accountCount: number
  defaultScopeType: DataScopeType
  createdTime: string
  version: number
}

/** 可分配权限资源节点。 */
export interface RolePermission {
  id: string
  parentId: string | null
  permissionCode: string
  name: string
  resourceType: 'DIRECTORY' | 'MENU' | 'BUTTON'
  appCode: 'TENANT_ADMIN' | 'TENANT_PORTAL' | 'SERVICE'
  featureCode: string | null
  sortOrder: number
  granted: boolean
}

/** 角色数据范围策略。 */
export interface RoleDataPolicy {
  moduleCode: string
  moduleName: string
  scopeType: DataScopeType
  organizationIds: string[]
}

/** 当前操作者授权上限。 */
export interface GrantBoundary {
  organizationId: string
  rootOwner: boolean
  permissionIds: string[]
  scopeTypes: DataScopeType[]
}
