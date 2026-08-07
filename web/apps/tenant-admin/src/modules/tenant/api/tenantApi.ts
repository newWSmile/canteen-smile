import type { ApiResponse } from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type { TenantManagementContext } from '../types'

/** 查询当前租户管理身份、租户边界和最终权限。 */
export async function getTenantManagementContext(): Promise<TenantManagementContext> {
  const response = await http.get<ApiResponse<TenantManagementContext>>('/iam/v1/tenant/context')
  if (response.data.data === null) throw new Error('租户上下文响应缺少 data')
  return response.data.data
}
