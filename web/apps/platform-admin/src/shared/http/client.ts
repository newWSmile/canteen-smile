import { createHttpClient } from '@canteen-smile/http-core'
import { feedback } from '@/shared/feedback'

/** 平台管理端 Token 独立存储键。 */
export const TOKEN_STORAGE_KEY = 'canteen-smile:platform-admin:token'

/** @return 当前平台管理端 Token，优先读取记住我存储 */
export function getPlatformToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY) || sessionStorage.getItem(TOKEN_STORAGE_KEY)
}

/** 保存当前设备 Token，并确保两个存储空间不会同时残留旧值。 */
export function savePlatformToken(token: string, rememberMe: boolean): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
  sessionStorage.removeItem(TOKEN_STORAGE_KEY)
  const storage = rememberMe ? localStorage : sessionStorage
  storage.setItem(TOKEN_STORAGE_KEY, token)
}

/** 清除当前设备的平台 Token。 */
export function clearPlatformToken(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
  sessionStorage.removeItem(TOKEN_STORAGE_KEY)
}

/** 平台管理端唯一 Axios 实例，不得与租户应用共享 Token。 */
export const http = createHttpClient({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  getToken: getPlatformToken,
  feedback,
})
