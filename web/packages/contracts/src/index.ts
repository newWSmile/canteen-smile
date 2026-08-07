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
