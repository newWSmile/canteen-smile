import type { ApiResponse, PageResult } from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type { CreatePermissionResourceRequest, PermissionResource, PermissionResourcePageQuery } from '../types'

const PERMISSION_RESOURCES_PATH = '/iam/v1/platform/permission-resources'

/** 分页查询平台权限资源。 */
export async function pagePermissionResources(
  query: PermissionResourcePageQuery,
): Promise<PageResult<PermissionResource>> {
  const response = await http.get<ApiResponse<PageResult<PermissionResource>>>(PERMISSION_RESOURCES_PATH, {
    params: query,
  })
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}

/** 创建不可复用权限码对应的草稿资源。 */
export async function createPermissionResource(
  request: CreatePermissionResourceRequest,
): Promise<PermissionResource> {
  const response = await http.post<ApiResponse<PermissionResource>>(PERMISSION_RESOURCES_PATH, request)
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}

/** 发布权限资源。 */
export async function publishPermissionResource(resource: PermissionResource): Promise<PermissionResource> {
  const response = await http.post<ApiResponse<PermissionResource>>(
    `${PERMISSION_RESOURCES_PATH}/${resource.id}/actions/publish`,
    undefined,
    { params: { version: resource.version } },
  )
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}

/** 永久废弃已发布权限资源。 */
export async function deprecatePermissionResource(resource: PermissionResource): Promise<PermissionResource> {
  const response = await http.post<ApiResponse<PermissionResource>>(
    `${PERMISSION_RESOURCES_PATH}/${resource.id}/actions/deprecate`,
    undefined,
    { params: { version: resource.version } },
  )
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}
