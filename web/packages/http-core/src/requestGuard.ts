import type { AxiosRequestConfig } from 'axios'

/** 相同请求仍在执行时抛出的错误。 */
export class DuplicateRequestError extends Error {
  public constructor() {
    super('请勿重复提交')
    this.name = 'DuplicateRequestError'
  }
}

function stableSerialize(value: unknown): string {
  if (value === undefined) return ''
  if (value === null || typeof value !== 'object') return JSON.stringify(value)
  if (Array.isArray(value)) return `[${value.map(stableSerialize).join(',')}]`

  return `{${Object.entries(value as Record<string, unknown>)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, entry]) => `${JSON.stringify(key)}:${stableSerialize(entry)}`)
    .join(',')}}`
}

/** 根据方法、地址、参数和请求体生成并发请求指纹。 */
export function requestFingerprint(config: AxiosRequestConfig): string {
  return [
    config.method?.toUpperCase() ?? 'GET',
    config.baseURL ?? '',
    config.url ?? '',
    stableSerialize(config.params),
    stableSerialize(config.data),
  ].join('|')
}
