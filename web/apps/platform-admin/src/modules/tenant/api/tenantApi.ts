import type { ApiResponse, PageResult } from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type {
  CreateTenantRequest,
  OrgTypeTemplate,
  PublishOrgTypeTemplateRequest,
  TenantCreation,
  TenantPageQuery,
  TenantSummary,
  TenantOwnerActivationLink,
  TenantOwnerPasswordResetLink,
  TenantOwnerPasswordResetRequest,
} from '../types'

const PLATFORM_TENANTS_PATH = '/iam/v1/platform/tenants'
const ORG_TYPE_TEMPLATES_PATH = '/iam/v1/platform/org-type-templates'

/** 分页查询当前平台身份可见的租户。 */
export async function pagePlatformTenants(
  query: TenantPageQuery,
): Promise<PageResult<TenantSummary>> {
  const response = await http.get<ApiResponse<PageResult<TenantSummary>>>(PLATFORM_TENANTS_PATH, {
    params: query,
  })
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}

/** 查询全部已发布机构类型模板版本。 */
export async function listOrgTypeTemplates(): Promise<OrgTypeTemplate[]> {
  const response = await http.get<ApiResponse<OrgTypeTemplate[]>>(ORG_TYPE_TEMPLATES_PATH)
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}

/** 发布新的完整且不可变的机构类型模板版本。 */
export async function publishOrgTypeTemplate(
  request: PublishOrgTypeTemplateRequest,
): Promise<OrgTypeTemplate> {
  const response = await http.post<ApiResponse<OrgTypeTemplate>>(ORG_TYPE_TEMPLATES_PATH, request)
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}

/** 创建租户并初始化根机构、所有者和 Auth 待激活凭证。 */
export async function createPlatformTenant(
  request: CreateTenantRequest,
  idempotencyKey: string,
): Promise<TenantCreation> {
  const response = await http.post<ApiResponse<TenantCreation>>(PLATFORM_TENANTS_PATH, request, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}

/** 为租户根机构首位所有者签发新的 24 小时一次性激活票据。 */
export async function issueTenantOwnerActivationLink(
  tenantId: string,
): Promise<TenantOwnerActivationLink> {
  const response = await http.post<ApiResponse<TenantOwnerActivationLink>>(
    `${PLATFORM_TENANTS_PATH}/${tenantId}/owner/activation-links`,
  )
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}

/** 为已经激活的租户所有者签发 30 分钟一次性密码恢复票据。 */
export async function issueTenantOwnerPasswordResetLink(
  tenantId: string,
  request: TenantOwnerPasswordResetRequest,
): Promise<TenantOwnerPasswordResetLink> {
  const response = await http.post<ApiResponse<TenantOwnerPasswordResetLink>>(
    `${PLATFORM_TENANTS_PATH}/${tenantId}/owner/password-reset-links`,
    request,
  )
  if (response.data.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data.data
}
