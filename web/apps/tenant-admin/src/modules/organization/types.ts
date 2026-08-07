import type { PageResult } from '@canteen-smile/contracts'

/** 租户机构类型。 */
export interface OrganizationType {
  id: string
  typeCode: string
  name: string
  sortOrder: number
  status: 'ACTIVE' | 'DISABLED'
  sourceTemplateVersion: number | null
  version: number
}

/** 允许的机构父子类型关系。 */
export interface OrganizationTypeRelation {
  id: string
  parentTypeId: string
  childTypeId: string
  version: number
}

/** 租户机构树节点。 */
export interface Organization {
  id: string
  parentId: string | null
  organizationTypeId: string
  typeCode: string
  typeName: string
  businessCode: string
  name: string
  adminRegionId: string | null
  ownStatus: 'ACTIVE' | 'DISABLED'
  effectiveStatus: 'ACTIVE' | 'DISABLED'
  pathVersion: number
  hasChildren: boolean
  version: number
}

/** 有界机构搜索结果。 */
export interface OrganizationSearchResult {
  id: string
  parentId: string | null
  organizationTypeId: string
  typeName: string
  businessCode: string
  name: string
  effectiveStatus: 'ACTIVE' | 'DISABLED'
  breadcrumb: string
}

/** 机构类型表单。 */
export interface OrganizationTypeForm {
  typeCode: string
  name: string
  sortOrder: number
}

/** 机构表单。 */
export interface OrganizationForm {
  parentId: string
  organizationTypeId: string
  businessCode: string
  name: string
  adminRegionId: string | null
}

export type OrganizationPage = PageResult<Organization>
export type OrganizationTypePage = PageResult<OrganizationType>
