import type { ApiResponse, PageResult } from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type {
  Organization,
  OrganizationForm,
  OrganizationSearchResult,
  OrganizationType,
  OrganizationTypeForm,
  OrganizationTypeRelation,
} from '../types'

const TYPES_PATH = '/iam/v1/tenant/organization-types'
const RELATIONS_PATH = '/iam/v1/tenant/organization-type-relations'
const ORGANIZATIONS_PATH = '/iam/v1/tenant/organizations'

/** 分页查询租户机构类型。 */
export async function getOrganizationTypes(
  pageNo = 1,
  pageSize = 100,
): Promise<PageResult<OrganizationType>> {
  const response = await http.get<ApiResponse<PageResult<OrganizationType>>>(TYPES_PATH, {
    params: { pageNo, pageSize },
  })
  return requireData(response.data)
}

/** 查询可用于机构表单的有效类型。 */
export async function getActiveOrganizationTypes(): Promise<OrganizationType[]> {
  const response = await http.get<ApiResponse<OrganizationType[]>>(`${TYPES_PATH}/active`)
  return requireData(response.data)
}

/** 新增租户机构类型。 */
export async function createOrganizationType(form: OrganizationTypeForm): Promise<OrganizationType> {
  const response = await http.post<ApiResponse<OrganizationType>>(TYPES_PATH, form)
  return requireData(response.data)
}

/** 修改租户机构类型名称和排序。 */
export async function updateOrganizationType(
  type: OrganizationType,
  form: Pick<OrganizationTypeForm, 'name' | 'sortOrder'>,
): Promise<OrganizationType> {
  const response = await http.put<ApiResponse<OrganizationType>>(`${TYPES_PATH}/${type.id}`, {
    ...form,
    version: type.version,
  })
  return requireData(response.data)
}

/** 停用或恢复机构类型。 */
export async function changeOrganizationTypeStatus(type: OrganizationType): Promise<OrganizationType> {
  const status = type.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const response = await http.put<ApiResponse<OrganizationType>>(`${TYPES_PATH}/${type.id}/status`, {
    status,
    version: type.version,
  })
  return requireData(response.data)
}

/** 查询完整机构类型允许关系。 */
export async function getOrganizationTypeRelations(): Promise<OrganizationTypeRelation[]> {
  const response = await http.get<ApiResponse<OrganizationTypeRelation[]>>(RELATIONS_PATH)
  return requireData(response.data)
}

/** 整版替换机构类型允许关系。 */
export async function replaceOrganizationTypeRelations(
  pairs: Array<{ parentTypeId: string; childTypeId: string }>,
): Promise<OrganizationTypeRelation[]> {
  const response = await http.put<ApiResponse<OrganizationTypeRelation[]>>(RELATIONS_PATH, {
    relations: pairs,
  })
  return requireData(response.data)
}

/** 查询租户根机构。 */
export async function getRootOrganization(): Promise<Organization> {
  const response = await http.get<ApiResponse<Organization>>(`${ORGANIZATIONS_PATH}/root`)
  return requireData(response.data)
}

/** 分页查询直属子机构。 */
export async function getChildOrganizations(
  parentId: string,
  pageNo: number,
  pageSize: number,
): Promise<PageResult<Organization>> {
  const response = await http.get<ApiResponse<PageResult<Organization>>>(ORGANIZATIONS_PATH, {
    params: { parentId, pageNo, pageSize },
  })
  return requireData(response.data)
}

/** 查询机构详情。 */
export async function getOrganization(organizationId: string): Promise<Organization> {
  const response = await http.get<ApiResponse<Organization>>(`${ORGANIZATIONS_PATH}/${organizationId}`)
  return requireData(response.data)
}

/** 搜索机构，用于迁移目标选择。 */
export async function searchOrganizations(keyword: string): Promise<OrganizationSearchResult[]> {
  const response = await http.get<ApiResponse<OrganizationSearchResult[]>>(`${ORGANIZATIONS_PATH}/search`, {
    params: { keyword },
  })
  return requireData(response.data)
}

/** 新增机构。 */
export async function createOrganization(form: OrganizationForm): Promise<Organization> {
  const response = await http.post<ApiResponse<Organization>>(ORGANIZATIONS_PATH, {
    parentId: form.parentId,
    organizationTypeId: form.organizationTypeId,
    businessCode: form.businessCode,
    name: form.name,
    adminRegionId: form.adminRegionId,
  })
  return requireData(response.data)
}

/** 修改机构资料。 */
export async function updateOrganization(
  organization: Organization,
  form: Pick<OrganizationForm, 'organizationTypeId' | 'name' | 'adminRegionId'>,
): Promise<Organization> {
  const response = await http.put<ApiResponse<Organization>>(`${ORGANIZATIONS_PATH}/${organization.id}`, {
    organizationTypeId: form.organizationTypeId,
    name: form.name,
    adminRegionId: form.adminRegionId,
    version: organization.version,
  })
  return requireData(response.data)
}

/** 迁移机构到新的合法父机构。 */
export async function moveOrganization(
  organization: Organization,
  newParentId: string,
  reason: string,
): Promise<Organization> {
  const response = await http.put<ApiResponse<Organization>>(
    `${ORGANIZATIONS_PATH}/${organization.id}/parent`,
    { newParentId, version: organization.version, reason },
  )
  return requireData(response.data)
}

/** 停用或恢复机构。 */
export async function changeOrganizationStatus(
  organization: Organization,
  reason: string,
): Promise<Organization> {
  const status = organization.ownStatus === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const response = await http.put<ApiResponse<Organization>>(
    `${ORGANIZATIONS_PATH}/${organization.id}/status`,
    { status, version: organization.version, reason },
  )
  return requireData(response.data)
}

/** 删除从未使用的空白机构。 */
export async function deleteOrganization(organization: Organization, reason: string): Promise<void> {
  await http.delete<ApiResponse<null>>(`${ORGANIZATIONS_PATH}/${organization.id}`, {
    data: { version: organization.version, reason },
  })
}

/** @return 统一成功响应中的非空数据 */
function requireData<T>(response: ApiResponse<T>): T {
  if (response.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data
}
