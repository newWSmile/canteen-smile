import type {
  ApiResponse,
  PageResult,
  SmsDeliveryPageQuery,
  SmsDeliveryRecord,
  SmsRateLimitSettingsUpdateRequest,
  SmsRuntimePolicy,
  SmsSecuritySettingsUpdateRequest,
} from '@canteen-smile/contracts'
import { http } from '@/shared/http'

/** 平台短信发送记录真实 IAM 接口路径。 */
const PLATFORM_SMS_DELIVERY_SEARCH_PATH = '/iam/v1/platform/sms-deliveries/search'
const PLATFORM_SMS_SETTINGS_PATH = '/iam/v1/platform/sms/settings'
const PLATFORM_SMS_SECURITY_PATH = '/iam/v1/platform/sms/security'

/** @param query 手机号精确筛选、时间范围和分页条件 @return 短信发送记录分页 */
export async function pageSmsDeliveries(
  query: SmsDeliveryPageQuery,
): Promise<PageResult<SmsDeliveryRecord>> {
  const response = await http.post<ApiResponse<PageResult<SmsDeliveryRecord>>>(
    PLATFORM_SMS_DELIVERY_SEARCH_PATH,
    query,
  )
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}

/** @return 当前短信验证码与多维限流设置 */
export async function getSmsSettings(): Promise<SmsRuntimePolicy> {
  const response = await http.get<ApiResponse<SmsRuntimePolicy>>(PLATFORM_SMS_SETTINGS_PATH)
  return requireData(response.data)
}

/** @param request 限流设置、原因和再认证票据 @return 更新后设置 */
export async function updateSmsSettings(
  request: SmsRateLimitSettingsUpdateRequest,
): Promise<SmsRuntimePolicy> {
  const response = await http.put<ApiResponse<SmsRuntimePolicy>>(PLATFORM_SMS_SETTINGS_PATH, request)
  return requireData(response.data)
}

/** @return 当前短信安全设置 */
export async function getSmsSecurity(): Promise<SmsRuntimePolicy> {
  const response = await http.get<ApiResponse<SmsRuntimePolicy>>(PLATFORM_SMS_SECURITY_PATH)
  return requireData(response.data)
}

/** @param request 安全开关、原因和再认证票据 @return 更新后设置 */
export async function updateSmsSecurity(
  request: SmsSecuritySettingsUpdateRequest,
): Promise<SmsRuntimePolicy> {
  const response = await http.put<ApiResponse<SmsRuntimePolicy>>(PLATFORM_SMS_SECURITY_PATH, request)
  return requireData(response.data)
}

/** 提取统一成功响应的非空 data。 */
function requireData<T>(response: ApiResponse<T>): T {
  if (response.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data
}
