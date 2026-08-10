import type { ApiResponse, PageResult } from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type { DataScopeType, GrantBoundary, Role, RoleDataPolicy, RolePermission } from '../types'

const ROLES_PATH = '/iam/v1/tenant/roles'

/** 分页查询当前机构角色。 */
export async function pageRoles(pageNo = 1, pageSize = 20): Promise<PageResult<Role>> {
  const response = await http.get<ApiResponse<PageResult<Role>>>(ROLES_PATH, { params: { pageNo, pageSize } })
  return requireData(response.data)
}

/** 创建当前机构自定义角色。 */
export async function createRole(request: {
  name: string
  description?: string
  defaultScopeType: DataScopeType
  specifiedOrganizationIds: string[]
}): Promise<Role> {
  const response = await http.post<ApiResponse<Role>>(ROLES_PATH, request)
  return requireData(response.data)
}

/** 修改自定义角色资料。 */
export async function updateRole(role: Role, request: { name: string; description?: string }): Promise<Role> {
  const response = await http.put<ApiResponse<Role>>(`${ROLES_PATH}/${role.id}`, { ...request, version: role.version })
  return requireData(response.data)
}

/** 启用或停用角色。 */
export async function changeRoleStatus(role: Role, reason: string): Promise<Role> {
  const action = role.status === 'ACTIVE' ? 'disable' : 'enable'
  const response = await http.post<ApiResponse<Role>>(`${ROLES_PATH}/${role.id}/actions/${action}`, {
    version: role.version,
    reason,
  })
  return requireData(response.data)
}

/** 删除没有关联账号的自定义角色。 */
export async function deleteRole(role: Role, reason: string): Promise<void> {
  await http.post<ApiResponse<null>>(`${ROLES_PATH}/${role.id}/actions/delete`, {
    version: role.version,
    reason,
  })
}

/** 查询当前角色权限树。 */
export async function getRolePermissions(roleId: string): Promise<RolePermission[]> {
  const response = await http.get<ApiResponse<RolePermission[]>>(`${ROLES_PATH}/${roleId}/permissions`)
  return requireData(response.data)
}

/** 整版替换角色权限。 */
export async function replaceRolePermissions(
  role: Role,
  permissionIds: string[],
  reason: string,
): Promise<RolePermission[]> {
  const response = await http.put<ApiResponse<RolePermission[]>>(`${ROLES_PATH}/${role.id}/permissions`, {
    permissionIds,
    version: role.version,
    reason,
  })
  return requireData(response.data)
}

/** 查询角色默认范围和模块覆盖。 */
export async function getRoleDataPolicies(roleId: string): Promise<RoleDataPolicy[]> {
  const response = await http.get<ApiResponse<RoleDataPolicy[]>>(`${ROLES_PATH}/${roleId}/data-policy`)
  return requireData(response.data)
}

/** 整版替换角色默认范围和模块覆盖。 */
export async function replaceRoleDataPolicies(
  role: Role,
  policies: RoleDataPolicy[],
  reason: string,
): Promise<RoleDataPolicy[]> {
  const response = await http.put<ApiResponse<RoleDataPolicy[]>>(`${ROLES_PATH}/${role.id}/data-policy`, {
    policies: policies.map(({ moduleCode, scopeType, organizationIds }) => ({ moduleCode, scopeType, organizationIds })),
    version: role.version,
    reason,
  })
  return requireData(response.data)
}

/** 查询当前操作者授权上限。 */
export async function getGrantBoundary(): Promise<GrantBoundary> {
  const response = await http.get<ApiResponse<GrantBoundary>>('/iam/v1/tenant/grant-boundary')
  return requireData(response.data)
}

/** @return 统一成功响应中的非空 data */
function requireData<T>(response: ApiResponse<T>): T {
  if (response.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data
}
