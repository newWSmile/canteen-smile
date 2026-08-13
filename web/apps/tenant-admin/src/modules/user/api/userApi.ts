import type { ApiResponse, PageResult } from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type {
  CreateTenantUserRequest,
  ReplaceTenantUserRolesRequest,
  TenantUser,
  TenantUserActivationLink,
  TenantUserPasswordResetLink,
  OrganizationOwner,
  TenantUserStatus,
  UpdateTenantUserRequest,
} from '../types'

const USERS_PATH = '/iam/v1/tenant/users'

/** 分页查询当前机构用户。 */
export async function pageTenantUsers(query: {
  pageNo: number
  pageSize: number
  keyword?: string
  status?: TenantUserStatus
}): Promise<PageResult<TenantUser>> {
  const response = await http.get<ApiResponse<PageResult<TenantUser>>>(USERS_PATH, { params: query })
  return requireData(response.data)
}

/** 创建本机构待激活用户。 */
export async function createTenantUser(request: CreateTenantUserRequest): Promise<TenantUser> {
  const response = await http.post<ApiResponse<TenantUser>>(USERS_PATH, request)
  return requireData(response.data)
}

/** 整版替换用户角色。 */
export async function replaceTenantUserRoles(
  accountId: string,
  request: ReplaceTenantUserRolesRequest,
): Promise<TenantUser> {
  const response = await http.put<ApiResponse<TenantUser>>(`${USERS_PATH}/${accountId}/roles`, request)
  return requireData(response.data)
}

/** 修改本机构用户非归属资料。 */
export async function updateTenantUser(
  accountId: string,
  request: UpdateTenantUserRequest,
): Promise<TenantUser> {
  const response = await http.patch<ApiResponse<TenantUser>>(`${USERS_PATH}/${accountId}`, request)
  return requireData(response.data)
}

/** 停用或恢复本机构普通用户。 */
export async function changeTenantUserStatus(
  user: TenantUser,
  reason: string,
): Promise<TenantUser> {
  const action = user.status === 'DISABLED' ? 'enable' : 'disable'
  const response = await http.post<ApiResponse<TenantUser>>(`${USERS_PATH}/${user.id}/actions/${action}`, {
    reason,
    version: user.version,
  })
  return requireData(response.data)
}

/** 不可恢复注销本机构普通用户。 */
export async function cancelTenantUser(user: TenantUser, reason: string): Promise<void> {
  await http.post<ApiResponse<null>>(`${USERS_PATH}/${user.id}/actions/cancel`, {
    reason,
    version: user.version,
  })
}

/** 为待激活用户生成新的 24 小时一次性激活票据。 */
export async function issueTenantUserActivationLink(accountId: string): Promise<TenantUserActivationLink> {
  const response = await http.post<ApiResponse<TenantUserActivationLink>>(
    `${USERS_PATH}/${accountId}/activation-links`,
  )
  return requireData(response.data)
}

/** 为授权范围内账号生成三十分钟一次性密码重置票据。 */
export async function issueTenantUserPasswordResetLink(
  accountId: string,
  request: { reauthTicket: string; reason: string },
): Promise<TenantUserPasswordResetLink> {
  const response = await http.post<ApiResponse<TenantUserPasswordResetLink>>(
    `${USERS_PATH}/${accountId}/password-reset-links`, request,
  )
  return requireData(response.data)
}

/** 查询当前机构唯一所有者。 */
export async function getOrganizationOwner(): Promise<OrganizationOwner> {
  const response = await http.get<ApiResponse<OrganizationOwner>>('/iam/v1/tenant/organization-owner')
  return requireData(response.data)
}

/** 当前所有者经再认证转让机构所有权。 */
export async function transferOrganizationOwner(request: {
  targetAccountId: string
  reauthTicket: string
  reason: string
  version: number
}): Promise<OrganizationOwner> {
  const response = await http.post<ApiResponse<OrganizationOwner>>(
    '/iam/v1/tenant/organization-owner/actions/transfer', request,
  )
  return requireData(response.data)
}

/** @return 统一成功响应中的非空 data */
function requireData<T>(response: ApiResponse<T>): T {
  if (response.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data
}
