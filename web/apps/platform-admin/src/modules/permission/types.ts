/** 权限资源类型。 */
export type PermissionResourceType = 'DIRECTORY' | 'MENU' | 'BUTTON' | 'API' | 'DATA_MODULE'
/** 权限资源所属应用。 */
export type PermissionAppCode = 'PLATFORM_ADMIN' | 'TENANT_ADMIN' | 'TENANT_PORTAL' | 'SERVICE'
/** 权限资源发布状态。 */
export type PermissionPublishStatus = 'DRAFT' | 'PUBLISHED' | 'DEPRECATED'

/** 平台权限资源。 */
export interface PermissionResource {
  id: string
  permissionCode: string
  resourceType: PermissionResourceType
  parentId: string | null
  name: string
  description: string | null
  appCode: PermissionAppCode
  routePath: string | null
  componentKey: string | null
  apiMethod: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | null
  apiPathPattern: string | null
  featureCode: string | null
  publishStatus: PermissionPublishStatus
  semanticVersion: number
  sortOrder: number
  createdTime: string
  version: number
}

/** 权限资源分页条件。 */
export interface PermissionResourcePageQuery {
  pageNo: number
  pageSize: number
  publishStatus?: PermissionPublishStatus
  appCode?: PermissionAppCode
  resourceType?: PermissionResourceType
}

/** 新建权限资源草稿请求。 */
export interface CreatePermissionResourceRequest {
  permissionCode: string
  resourceType: Exclude<PermissionResourceType, 'DATA_MODULE'>
  parentId?: string
  name: string
  description?: string
  appCode: PermissionAppCode
  routePath?: string
  componentKey?: string
  apiMethod?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  apiPathPattern?: string
  featureCode?: string
  semanticVersion: number
  sortOrder: number
}
