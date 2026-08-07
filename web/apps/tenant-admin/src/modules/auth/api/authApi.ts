import type { ApiResponse } from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type {
  ActivationCompleteResult,
  ActivationContext,
  PasswordEncryptionChallenge,
  PasswordEnvelopePurpose,
  PasswordEnvelopeRequest,
  PasswordLoginRequest,
  LoginResult,
  TenantSession,
  PasswordResetContext,
  PasswordResetCompleteResult,
} from '../types'

/** 创建绑定业务用途的一次性密码加密挑战。 */
export async function createPasswordEncryptionChallenge(
  purpose: PasswordEnvelopePurpose,
): Promise<PasswordEncryptionChallenge> {
  const response = await http.post<ApiResponse<PasswordEncryptionChallenge>>(
    '/auth/v1/password-encryption/challenges',
    { purpose },
  )
  return requireData(response.data)
}

/** 查询一次性票据对应的脱敏激活上下文。 */
export async function getActivationContext(ticket: string): Promise<ActivationContext> {
  const response = await http.get<ApiResponse<ActivationContext>>(
    `/auth/v1/activations/${encodeURIComponent(ticket)}/context`,
  )
  return requireData(response.data)
}

/** 使用加密初始密码完成账号激活。 */
export async function completeActivation(
  ticket: string,
  passwordEnvelope: PasswordEnvelopeRequest,
): Promise<ActivationCompleteResult> {
  const response = await http.post<ApiResponse<ActivationCompleteResult>>(
    `/auth/v1/activations/${encodeURIComponent(ticket)}/complete`,
    { passwordEnvelope },
  )
  return requireData(response.data)
}

/** 查询一次性密码恢复票据对应的脱敏账号上下文。 */
export async function getPasswordResetContext(ticket: string): Promise<PasswordResetContext> {
  const response = await http.get<ApiResponse<PasswordResetContext>>(
    `/auth/v1/password-resets/${encodeURIComponent(ticket)}/context`,
  )
  return requireData(response.data)
}

/** 使用加密新密码完成一次性密码恢复。 */
export async function completePasswordReset(
  ticket: string,
  passwordEnvelope: PasswordEnvelopeRequest,
): Promise<PasswordResetCompleteResult> {
  const response = await http.post<ApiResponse<PasswordResetCompleteResult>>(
    `/auth/v1/password-resets/${encodeURIComponent(ticket)}/complete`,
    { passwordEnvelope },
  )
  return requireData(response.data)
}

/** 使用租户账号用户名和加密密码建立设备会话。 */
export async function passwordLogin(request: PasswordLoginRequest): Promise<LoginResult> {
  const response = await http.post<ApiResponse<LoginResult>>('/auth/v1/login/password', request)
  return requireData(response.data)
}

/** 查询当前租户设备会话。 */
export async function getCurrentSession(): Promise<TenantSession> {
  const response = await http.get<ApiResponse<TenantSession>>('/auth/v1/session')
  return requireData(response.data)
}

/** 退出当前设备会话。 */
export async function logoutCurrentSession(): Promise<void> {
  await http.post<ApiResponse<null>>('/auth/v1/logout')
}

/** @param response 统一响应 @return 非空数据 */
function requireData<T>(response: ApiResponse<T>): T {
  if (response.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data
}
