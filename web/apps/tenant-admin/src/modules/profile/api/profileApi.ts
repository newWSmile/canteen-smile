import type { ApiResponse } from '@canteen-smile/contracts'
import { http } from '@/shared/http'

/** 用户名修改结果。 */
export interface ChangedUsername {
  username: string
}

/** 当前账号经再认证修改平台唯一用户名。 */
export async function changeCurrentUsername(request: {
  username: string
  reauthTicket: string
  reason: string
}): Promise<ChangedUsername> {
  const response = await http.post<ApiResponse<ChangedUsername>>('/iam/v1/me/username/actions/change', request)
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}
