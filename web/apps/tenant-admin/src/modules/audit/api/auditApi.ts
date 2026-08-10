import type { ApiResponse, AuditLog, AuditLogPageQuery, PageResult } from '@canteen-smile/contracts'
import { http } from '@/shared/http'

/** 租户审计真实 IAM 接口路径。 */
const TENANT_AUDIT_PATH = '/iam/v1/tenant/audit-logs'

/** @param query 精确筛选与分页条件 @return 当前租户授权范围内审计分页 */
export async function pageTenantAuditLogs(query: AuditLogPageQuery): Promise<PageResult<AuditLog>> {
  const response = await http.get<ApiResponse<PageResult<AuditLog>>>(TENANT_AUDIT_PATH, {
    params: query,
  })
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}
