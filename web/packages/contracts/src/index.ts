/** 后端统一响应协议。字段必须与 server 中 ApiResponse 保持一致。 */
export interface ApiResponse<T> {
  code: string
  message: string
  data: T | null
  timestamp: string
  traceId: string | null
}

/** 后端统一分页协议；bigint 业务 ID 仍由具体契约声明为 string。 */
export interface PageResult<T> {
  items: T[]
  pageNo: number
  pageSize: number
  total: number
}

/** 普通分页允许的最大每页数量。 */
export const MAX_PAGE_SIZE = 100

/** 可查询的审计数据来源。 */
export type AuditSource = 'IAM' | 'AUTH'

/** 审计动作结果。 */
export type AuditResult = 'SUCCESS' | 'FAILURE' | 'DENIED'

/** IAM 聚合并对三个前端稳定输出的审计日志契约。 */
export interface AuditLog {
  id: string
  source: AuditSource
  tenantId: string | null
  operatorType: string
  operatorTypeName: string
  operatorId: string | null
  operatorUsername: string | null
  operatorDisplayName: string | null
  actionCode: string
  actionName: string
  targetType: string | null
  targetTypeName: string
  targetId: string | null
  targetName: string | null
  targetCode: string | null
  result: AuditResult
  reason: string | null
  loginMethod: string | null
  loginMethodName: string | null
  failureReasonCode: string | null
  failureReason: string | null
  maskedMobile: string | null
  deviceSummary: string | null
  traceId: string | null
  occurredTime: string
}

/** 审计日志精确筛选与分页参数。 */
export interface AuditLogPageQuery {
  pageNo: number
  pageSize: number
  source: AuditSource
  actionCode?: string
  result?: AuditResult
  operatorId?: string
  startTime?: string
  endTime?: string
}
