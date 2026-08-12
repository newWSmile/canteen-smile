import type { ApiResponse } from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type { TenantSecurityPolicy, UpdateTenantSecurityPolicyRequest } from '../types'

/** 查询当前租户安全策略。 */
export async function getTenantSecurityPolicy(): Promise<TenantSecurityPolicy> {
  const response = await http.get<ApiResponse<TenantSecurityPolicy>>('/iam/v1/tenant/security-policy')
  return requireData(response.data)
}

/** 使用再认证票据修改当前租户安全策略。 */
export async function updateTenantSecurityPolicy(
  request: UpdateTenantSecurityPolicyRequest,
): Promise<TenantSecurityPolicy> {
  const response = await http.put<ApiResponse<TenantSecurityPolicy>>(
    '/iam/v1/tenant/security-policy',
    request,
  )
  return requireData(response.data)
}

/** @return 统一成功响应中的非空数据。 */
function requireData<T>(response: ApiResponse<T>): T {
  if (response.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data
}
