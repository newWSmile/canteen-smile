import { createHttpClient } from '@canteen-smile/http-core'
import { feedback } from '@/shared/feedback'

export const TOKEN_STORAGE_KEY = 'canteen-smile:tenant-admin:token'

/** @return 当前租户管理端 Token */
export function getTenantAdminToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY) || sessionStorage.getItem(TOKEN_STORAGE_KEY)
}

/** 保存租户管理端 Token，并清理另一个存储空间的旧值。 */
export function saveTenantAdminToken(token: string, rememberMe: boolean): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
  sessionStorage.removeItem(TOKEN_STORAGE_KEY)
  ;(rememberMe ? localStorage : sessionStorage).setItem(TOKEN_STORAGE_KEY, token)
}

/** 清理租户管理端 Token。 */
export function clearTenantAdminToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
  sessionStorage.removeItem(TOKEN_STORAGE_KEY)
}

/** 认证失效时清理本端状态并使用整页跳转终止所有在途页面请求。 */
function handleUnauthorized(message: string): void {
  clearTenantAdminToken()
  feedback.error(message || '登录状态已失效，请重新登录')
  const loginUrl = new URL(`${import.meta.env.BASE_URL}login`, window.location.origin)
  if (window.location.pathname !== loginUrl.pathname) window.location.replace(loginUrl)
}

/** 租户管理端唯一 Axios 实例，Token 与租户业务端隔离。 */
export const http = createHttpClient({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  getToken: getTenantAdminToken,
  feedback,
  onUnauthorized: handleUnauthorized,
})
