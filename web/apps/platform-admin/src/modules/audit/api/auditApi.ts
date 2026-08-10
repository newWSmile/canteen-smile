import type { ApiResponse, AuditLog, AuditLogPageQuery, PageResult } from '@canteen-smile/contracts'
import { http } from '@/shared/http'

/** 平台审计真实 IAM 接口路径。 */
const PLATFORM_AUDIT_PATH = '/iam/v1/platform/audit-logs'

/** @param query 精确筛选与分页条件 @return 平台身份范围内审计分页 */
export async function pagePlatformAuditLogs(query: AuditLogPageQuery): Promise<PageResult<AuditLog>> {
  const response = await http.get<ApiResponse<PageResult<AuditLog>>>(PLATFORM_AUDIT_PATH, {
    params: query,
  })
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}
