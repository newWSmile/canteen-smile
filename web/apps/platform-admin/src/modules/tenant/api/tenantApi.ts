import type { ApiResponse, PageResult } from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type { TenantPageQuery, TenantSummary } from '../types'

const PLATFORM_TENANTS_PATH = '/iam/v1/platform/tenants'

/** 分页查询当前平台身份可见的租户。 */
export async function pagePlatformTenants(
  query: TenantPageQuery,
): Promise<PageResult<TenantSummary>> {
  const response = await http.get<ApiResponse<PageResult<TenantSummary>>>(PLATFORM_TENANTS_PATH, {
    params: query,
  })
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}
