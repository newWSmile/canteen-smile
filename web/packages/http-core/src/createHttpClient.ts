import type { ApiResponse } from '@canteen-smile/contracts'
import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { DuplicateRequestError, requestFingerprint } from './requestGuard'

const REQUEST_FINGERPRINT = Symbol('requestFingerprint')

type GuardedRequestConfig = InternalAxiosRequestConfig & {
  [REQUEST_FINGERPRINT]?: string
}

/** HTTP 层使用的统一反馈出口。 */
export interface HttpFeedback {
  error(message: string): void
  warning(message: string): void
}

/** 创建应用级 Axios 实例所需配置。 */
export interface HttpClientOptions {
  baseURL: string
  getToken: () => string | null
  feedback: HttpFeedback
  timeout?: number
  tokenHeaderName?: string
}

/** 创建带统一响应处理和重复请求保护的应用级 Axios 实例。 */
export function createHttpClient(options: HttpClientOptions): AxiosInstance {
  const pendingRequests = new Set<string>()
  const http = axios.create({
    baseURL: options.baseURL,
    timeout: options.timeout ?? 15_000,
    headers: { 'Content-Type': 'application/json' },
  })

  const releaseRequest = (config?: GuardedRequestConfig): void => {
    const fingerprint = config?.[REQUEST_FINGERPRINT]
    if (fingerprint) pendingRequests.delete(fingerprint)
  }

  http.interceptors.request.use((config) => {
    const guardedConfig = config as GuardedRequestConfig
    if (!config.allowDuplicateRequest) {
      const fingerprint = requestFingerprint(config)
      if (pendingRequests.has(fingerprint)) throw new DuplicateRequestError()
      pendingRequests.add(fingerprint)
      guardedConfig[REQUEST_FINGERPRINT] = fingerprint
    }

    const token = options.getToken()
    if (token) config.headers.set(options.tokenHeaderName ?? 'satoken', token)
    return config
  })

  http.interceptors.response.use(
    (response: AxiosResponse<ApiResponse<unknown>>) => {
      releaseRequest(response.config as GuardedRequestConfig)
      if (response.data.code !== '0') {
        const message = response.data.message || '请求处理失败'
        options.feedback.error(message)
        return Promise.reject(new Error(message))
      }
      return response
    },
    (error: unknown) => {
      if (error instanceof DuplicateRequestError) {
        options.feedback.warning(error.message)
      } else if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
        releaseRequest(error.config as GuardedRequestConfig | undefined)
        if (!error.config?.silentError) {
          options.feedback.error(error.response?.data?.message || error.message || '网络请求失败')
        }
      } else {
        options.feedback.error('请求处理异常')
      }
      return Promise.reject(error)
    },
  )

  return http
}
