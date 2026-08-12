import type { ApiResponse, DeviceSession, PageResult } from '@canteen-smile/contracts'
import type {
  MobileBindingChallengeRequest,
  MobileBindingConfirmRequest,
  MobileBindingStatus,
  SmsChallenge,
  CurrentMobileChallengeRequest,
  CurrentMobileVerificationRequest,
  MobileChangeChallengeRequest,
  MobileChangeConfirmRequest,
  MobileUnbindConfirmRequest,
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
  SmsPasswordResetAccountSelectionRequest,
  SmsPasswordResetResult,
  SmsPasswordResetVerificationRequest,
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

/** 创建调用方明确指定用途的短信验证码挑战。 */
export async function createSmsChallenge(
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

/** 消费 PASSWORD_RESET 用途短信验证码并开始自助找回密码。 */
export async function verifySmsPasswordReset(
  request: SmsPasswordResetVerificationRequest,
): Promise<SmsPasswordResetResult> {
  const response = await http.post<ApiResponse<SmsPasswordResetResult>>(
    '/auth/v1/password-resets/sms/verification',
    request,
  )
  return requireData(response.data)
}

/** 多账号场景选择本次需要找回密码的具体账号。 */
export async function selectSmsPasswordResetAccount(
  request: SmsPasswordResetAccountSelectionRequest,
): Promise<SmsPasswordResetResult> {
  const response = await http.post<ApiResponse<SmsPasswordResetResult>>(
    '/auth/v1/password-resets/sms/account-selection',
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

/** 分页查询当前账号在两个租户端的有效设备会话。 */
export async function getDeviceSessions(
  pageNo: number,
  pageSize: number,
): Promise<PageResult<DeviceSession>> {
  const response = await http.get<ApiResponse<PageResult<DeviceSession>>>('/auth/v1/sessions', {
    params: { pageNo, pageSize },
  })
  return requireData(response.data)
}

/** 下线当前账号指定设备会话。 */
export async function logoutDeviceSession(sessionId: string, version: number): Promise<void> {
  await http.delete<ApiResponse<null>>(`/auth/v1/sessions/${encodeURIComponent(sessionId)}`, {
    data: { version },
  })
}

/** 下线当前请求设备之外的全部有效设备。 */
export async function logoutOtherDeviceSessions(): Promise<void> {
  await http.post<ApiResponse<null>>('/auth/v1/sessions/actions/logout-others')
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

/** 向当前账号已验证手机号发送换绑或解绑验证码。 */
export async function createCurrentMobileChallenge(
  request: CurrentMobileChallengeRequest,
): Promise<SmsChallenge> {
  const response = await http.post<ApiResponse<SmsChallenge>>(
    '/auth/v1/mobile/binding/current-mobile/challenges',
    request,
  )
  return requireData(response.data)
}

/** 使用当前手机号验证码取得换绑或解绑单用途票据。 */
export async function verifyCurrentMobile(
  request: CurrentMobileVerificationRequest,
): Promise<ReauthTicket> {
  const response = await http.post<ApiResponse<ReauthTicket>>(
    '/auth/v1/mobile/binding/current-mobile/verification',
    request,
  )
  return requireData(response.data)
}

/** 向与当前号码不同的新手机号发送换绑验证码。 */
export async function createMobileChangeChallenge(
  request: MobileChangeChallengeRequest,
): Promise<SmsChallenge> {
  const response = await http.post<ApiResponse<SmsChallenge>>(
    '/auth/v1/mobile/binding/change/challenges',
    request,
  )
  return requireData(response.data)
}

/** 使用再认证票据与新手机号验证码完成换绑。 */
export async function confirmMobileChange(
  request: MobileChangeConfirmRequest,
): Promise<MobileBindingStatus> {
  const response = await http.post<ApiResponse<MobileBindingStatus>>(
    '/auth/v1/mobile/binding/change/confirm',
    request,
  )
  return requireData(response.data)
}

/** 使用单用途再认证票据完成手机号解绑。 */
export async function confirmMobileUnbind(
  request: MobileUnbindConfirmRequest,
): Promise<MobileBindingStatus> {
  const response = await http.post<ApiResponse<MobileBindingStatus>>(
    '/auth/v1/mobile/binding/unbind/confirm',
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
