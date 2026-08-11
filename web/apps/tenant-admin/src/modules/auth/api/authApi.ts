import type { ApiResponse } from '@canteen-smile/contracts'
import type {
  MobileBindingChallengeRequest,
  MobileBindingConfirmRequest,
  MobileBindingStatus,
  SmsChallenge,
} from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type {
  ActivationCompleteResult,
  ActivationContext,
  PasswordEncryptionChallenge,
  PasswordEnvelopePurpose,
  PasswordEnvelopeRequest,
  PasswordLoginRequest,
  LoginResult,
  SmsChallengeCreateRequest,
  SmsLoginRequest,
  AccountSelectionLoginRequest,
  TenantSession,
  PasswordResetContext,
  PasswordResetCompleteResult,
  PasswordReauthRequest,
  ReauthTicket,
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

/** 创建 LOGIN 用途短信验证码挑战。 */
export async function createSmsLoginChallenge(
  request: SmsChallengeCreateRequest,
): Promise<SmsChallenge> {
  const response = await http.post<ApiResponse<SmsChallenge>>('/auth/v1/sms/challenges', request)
  return requireData(response.data)
}

/** 消费短信验证码；单账号直接登录，多账号返回选择票据。 */
export async function smsLogin(request: SmsLoginRequest): Promise<LoginResult> {
  const response = await http.post<ApiResponse<LoginResult>>('/auth/v1/login/sms', request)
  return requireData(response.data)
}

/** 使用短期一次性票据选择具体租户账号并完成登录。 */
export async function accountSelectionLogin(
  request: AccountSelectionLoginRequest,
): Promise<LoginResult> {
  const response = await http.post<ApiResponse<LoginResult>>(
    '/auth/v1/login/account-selection',
    request,
  )
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

/** 查询当前账号不泄露完整手机号的绑定状态。 */
export async function getMobileBindingStatus(): Promise<MobileBindingStatus> {
  const response = await http.get<ApiResponse<MobileBindingStatus>>('/auth/v1/mobile/binding')
  return requireData(response.data)
}

/** 为当前账号创建首次绑定手机号验证码挑战。 */
export async function createMobileBindingChallenge(
  request: MobileBindingChallengeRequest,
): Promise<SmsChallenge> {
  const response = await http.post<ApiResponse<SmsChallenge>>(
    '/auth/v1/mobile/binding/challenges',
    request,
  )
  return requireData(response.data)
}

/** 原子消费验证码并完成当前账号首次手机号绑定。 */
export async function confirmMobileBinding(
  request: MobileBindingConfirmRequest,
): Promise<MobileBindingStatus> {
  const response = await http.post<ApiResponse<MobileBindingStatus>>(
    '/auth/v1/mobile/binding/confirm',
    request,
  )
  return requireData(response.data)
}

/** 使用当前密码取得绑定单一敏感操作的五分钟一次性票据。 */
export async function reauthenticatePassword(request: PasswordReauthRequest): Promise<ReauthTicket> {
  const response = await http.post<ApiResponse<ReauthTicket>>('/auth/v1/reauth/password', request)
  return requireData(response.data)
}

/** @param response 统一响应 @return 非空数据 */
function requireData<T>(response: ApiResponse<T>): T {
  if (response.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data
}
