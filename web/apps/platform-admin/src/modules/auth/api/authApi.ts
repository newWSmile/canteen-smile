import type { ApiResponse } from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type {
  LoginResult,
  PasswordLoginRequest,
  PasswordEncryptionChallenge,
  PasswordEnvelopePurpose,
  PlatformBootstrapRequest,
  PlatformBootstrapResult,
  PlatformRecoveryLoginRequest,
  Session,
} from '../types'

/** 后端已经实现的 Auth 外部路径。 */
const AUTH_PATHS = {
  passwordEncryptionChallenges: '/auth/v1/password-encryption/challenges',
  bootstrap: '/auth/v1/platform/bootstrap',
  passwordLogin: '/auth/v1/login/password',
  recoveryLogin: '/auth/v1/login/platform-recovery-code',
  session: '/auth/v1/session',
  logout: '/auth/v1/logout',
} as const

/** 创建绑定用途且只能消费一次的短期密码加密挑战。 */
export async function createPasswordEncryptionChallenge(
  purpose: PasswordEnvelopePurpose,
): Promise<PasswordEncryptionChallenge> {
  const response = await http.post<ApiResponse<PasswordEncryptionChallenge>>(
    AUTH_PATHS.passwordEncryptionChallenges,
    { purpose },
  )
  return requireData(response.data)
}

/** 提取统一成功响应的非空 data。 */
function requireData<T>(response: ApiResponse<T>): T {
  if (response.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data
}

/** 首次创建平台超级管理员。 */
export async function bootstrapPlatform(
  secret: string,
  request: PlatformBootstrapRequest,
): Promise<PlatformBootstrapResult> {
  const response = await http.post<ApiResponse<PlatformBootstrapResult>>(AUTH_PATHS.bootstrap, request, {
    headers: { 'X-Bootstrap-Secret': secret },
  })
  return requireData(response.data)
}

/** 校验平台用户名和密码。 */
export async function passwordLogin(request: PasswordLoginRequest): Promise<LoginResult> {
  const response = await http.post<ApiResponse<LoginResult>>(AUTH_PATHS.passwordLogin, request)
  return requireData(response.data)
}

/** 使用一次性恢复码完成平台二次验证。 */
export async function platformRecoveryLogin(
  request: PlatformRecoveryLoginRequest,
): Promise<Session> {
  const response = await http.post<ApiResponse<Session>>(AUTH_PATHS.recoveryLogin, request)
  return requireData(response.data)
}

/** 查询当前设备会话。 */
export async function getCurrentSession(): Promise<Session> {
  const response = await http.get<ApiResponse<Session>>(AUTH_PATHS.session)
  return requireData(response.data)
}

/** 退出当前设备。 */
export async function logoutCurrentSession(): Promise<void> {
  await http.post<ApiResponse<null>>(AUTH_PATHS.logout)
}
