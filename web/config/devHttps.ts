import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath, URL } from 'node:url'
import type { ServerOptions } from 'node:https'

/** Vite 开发 HTTPS 可覆盖路径环境变量。 */
interface DevHttpsEnvironment {
  VITE_DEV_HTTPS_CERT?: string
  VITE_DEV_HTTPS_KEY?: string
}

/**
 * 读取三个前端应用共用的本地开发证书。
 * 证书和私钥只能保存在已忽略的 web/.certs，不得提交到代码仓库。
 */
export function createDevHttpsConfig(env: DevHttpsEnvironment): ServerOptions {
  const certificatePath = env.VITE_DEV_HTTPS_CERT?.trim()
    || fileURLToPath(new URL('../.certs/dev-cert.pem', import.meta.url))
  const privateKeyPath = env.VITE_DEV_HTTPS_KEY?.trim()
    || fileURLToPath(new URL('../.certs/dev-key.pem', import.meta.url))

  if (!existsSync(certificatePath) || !existsSync(privateKeyPath)) {
    throw new Error(
      '未找到本地 HTTPS 开发证书。请在 web 目录执行 pnpm https:setup，然后重新启动前端。',
    )
  }

  return {
    cert: readFileSync(certificatePath),
    key: readFileSync(privateKeyPath),
  }
}
