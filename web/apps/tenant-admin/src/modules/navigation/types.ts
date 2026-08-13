/** 租户功能开关。 */
export interface TenantFeature {
  featureCode: string
  name: string
  description: string
  enabled: boolean
  version: number
}

/** 租户菜单显示和当前账号个人偏好。 */
export interface TenantMenuSetting {
  permissionCode: string
  parentPermissionCode: string | null
  name: string
  routePath: string | null
  featureCode: string | null
  featureEnabled: boolean
  tenantHidden: boolean
  tenantVersion: number
  personallyHidden: boolean
  preferenceVersion: number
  sortOrder: number
}

/** 租户功能与统一菜单配置。 */
export interface TenantNavigationSettings {
  features: TenantFeature[]
  menus: TenantMenuSetting[]
}

/** 敏感配置修改命令。 */
export interface SensitiveNavigationUpdate {
  version: number
  reauthTicket: string
  reason: string
}
