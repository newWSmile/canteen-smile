import type { ApiResponse } from '@canteen-smile/contracts'
import { http } from '@/shared/http'
import type { SensitiveNavigationUpdate, TenantMenuSetting, TenantNavigationSettings } from '../types'

const SETTINGS_PATH = '/iam/v1/tenant/navigation-settings'
const PREFERENCES_PATH = '/iam/v1/me/menu-preferences'

/** 查询租户功能和统一菜单显示配置。 */
export async function getTenantNavigationSettings(): Promise<TenantNavigationSettings> {
  const response = await http.get<ApiResponse<TenantNavigationSettings>>(SETTINGS_PATH)
  return requireData(response.data)
}

/** 修改租户功能启停状态。 */
export async function updateTenantFeature(
  featureCode: string,
  enabled: boolean,
  command: SensitiveNavigationUpdate,
): Promise<TenantNavigationSettings> {
  const response = await http.put<ApiResponse<TenantNavigationSettings>>(
    `${SETTINGS_PATH}/features/${encodeURIComponent(featureCode)}`,
    { enabled, ...command },
  )
  return requireData(response.data)
}

/** 修改租户统一菜单隐藏状态。 */
export async function updateTenantMenu(
  permissionCode: string,
  hidden: boolean,
  command: SensitiveNavigationUpdate,
): Promise<TenantNavigationSettings> {
  const response = await http.put<ApiResponse<TenantNavigationSettings>>(
    `${SETTINGS_PATH}/menus/${encodeURIComponent(permissionCode)}`,
    { hidden, ...command },
  )
  return requireData(response.data)
}

/** 查询当前账号个人菜单偏好。 */
export async function getMenuPreferences(): Promise<TenantMenuSetting[]> {
  const response = await http.get<ApiResponse<TenantMenuSetting[]>>(PREFERENCES_PATH)
  return requireData(response.data)
}

/** 修改当前账号个人菜单隐藏偏好。 */
export async function updateMenuPreference(
  menu: TenantMenuSetting,
  hidden: boolean,
): Promise<TenantMenuSetting[]> {
  const response = await http.put<ApiResponse<TenantMenuSetting[]>>(
    `${PREFERENCES_PATH}/${encodeURIComponent(menu.permissionCode)}`,
    { hidden, version: menu.preferenceVersion },
  )
  return requireData(response.data)
}

/** 读取统一响应非空数据。 */
function requireData<T>(response: ApiResponse<T>): T {
  if (response.data === null) throw new Error('服务端成功响应缺少 data')
  return response.data
}
