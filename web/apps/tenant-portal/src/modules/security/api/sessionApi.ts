import type { ApiResponse, DeviceSession, PageResult } from '@canteen-smile/contracts'
import { http } from '@/shared/http'

/** 分页查询当前租户用户账号的有效设备会话。 */
export async function getDeviceSessions(
  pageNo: number,
  pageSize: number,
): Promise<PageResult<DeviceSession>> {
  const response = await http.get<ApiResponse<PageResult<DeviceSession>>>('/auth/v1/sessions', {
    params: { pageNo, pageSize },
  })
  if (response.data.data === null) throw new Error('设备会话响应缺少 data')
  return response.data.data
}

/** 下线指定设备会话。 */
export async function logoutDeviceSession(sessionId: string, version: number): Promise<void> {
  await http.delete<ApiResponse<null>>(`/auth/v1/sessions/${encodeURIComponent(sessionId)}`, {
    data: { version },
  })
}

/** 下线当前设备之外的全部有效设备。 */
export async function logoutOtherDeviceSessions(): Promise<void> {
  await http.post<ApiResponse<null>>('/auth/v1/sessions/actions/logout-others')
}
