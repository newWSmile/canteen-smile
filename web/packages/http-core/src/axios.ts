import 'axios'

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 是否允许同一时刻发送完全相同的请求，默认 false。 */
    allowDuplicateRequest?: boolean
    /** 是否禁止全局错误提示，默认 false。 */
    silentError?: boolean
  }
}
