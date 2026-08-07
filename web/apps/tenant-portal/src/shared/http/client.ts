import { createHttpClient } from '@canteen-smile/http-core'
import { feedback } from '@/shared/feedback'

const TOKEN_STORAGE_KEY = 'canteen-smile:tenant-portal:token'

/** 租户业务端唯一 Axios 实例，Token 与租户管理端隔离。 */
export const http = createHttpClient({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  getToken: () => sessionStorage.getItem(TOKEN_STORAGE_KEY),
  feedback,
})
