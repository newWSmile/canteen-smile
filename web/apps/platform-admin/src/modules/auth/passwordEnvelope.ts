import { createPasswordEncryptionChallenge } from './api/authApi'
import type {
  PasswordEncryptionChallenge,
  PasswordEnvelopePurpose,
  PasswordEnvelopeRequest,
} from './types'

/** 浏览器无法安全完成密码信封加密时抛出的本地错误。 */
export class PasswordEnvelopeError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'PasswordEnvelopeError'
  }
}

/** 将标准 Base64 文本转换为独立 ArrayBuffer。 */
function decodeBase64(value: string): ArrayBuffer {
  const binary = window.atob(value)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes.buffer
}

/** 将二进制数据转换为标准 Base64 文本。 */
function encodeBase64(value: ArrayBuffer): string {
  const bytes = new Uint8Array(value)
  let binary = ''
  for (const byte of bytes) binary += String.fromCharCode(byte)
  return window.btoa(binary)
}

/** 构建与 Auth 完全一致且不可跨业务复用的附加认证数据。 */
function createAdditionalData(challenge: PasswordEncryptionChallenge): Uint8Array {
  return new TextEncoder().encode(
    [challenge.purpose, challenge.keyId, challenge.nonce, challenge.timestamp.toString()].join('\n'),
  )
}

/** 校验服务端没有返回前端不支持的算法，禁止静默降级。 */
function requireSupportedAlgorithms(challenge: PasswordEncryptionChallenge): void {
  if (challenge.keyAlgorithm !== 'RSA-OAEP-256' || challenge.contentAlgorithm !== 'A256GCM') {
    throw new PasswordEnvelopeError('服务端返回了不受支持的密码加密算法')
  }
}

/**
 * 使用短期 RSA-OAEP 公钥包装随机 AES-256 密钥，并通过 AES-GCM 加密密码。
 * 密码明文不会进入 Axios 请求对象。
 */
export async function encryptPassword(
  password: string,
  purpose: PasswordEnvelopePurpose,
): Promise<PasswordEnvelopeRequest> {
  if (!window.isSecureContext || !window.crypto?.subtle) {
    throw new PasswordEnvelopeError('当前页面不是安全上下文，无法执行密码加密')
  }
  const challenge = await createPasswordEncryptionChallenge(purpose)
  requireSupportedAlgorithms(challenge)
  if (challenge.purpose !== purpose) {
    throw new PasswordEnvelopeError('密码加密挑战用途不匹配')
  }

  const additionalData = createAdditionalData(challenge)
  const iv = window.crypto.getRandomValues(new Uint8Array(12))
  const passwordBytes = new TextEncoder().encode(password)
  const rsaPublicKey = await window.crypto.subtle.importKey(
    'spki',
    decodeBase64(challenge.publicKey),
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    false,
    ['encrypt'],
  )
  const aesKey = await window.crypto.subtle.generateKey(
    { name: 'AES-GCM', length: 256 },
    true,
    ['encrypt'],
  )
  const rawAesKey = await window.crypto.subtle.exportKey('raw', aesKey)
  try {
    const encryptedKey = await window.crypto.subtle.encrypt(
      { name: 'RSA-OAEP', label: additionalData },
      rsaPublicKey,
      rawAesKey,
    )
    const ciphertext = await window.crypto.subtle.encrypt(
      { name: 'AES-GCM', iv, additionalData, tagLength: 128 },
      aesKey,
      passwordBytes,
    )
    return {
      keyId: challenge.keyId,
      nonce: challenge.nonce,
      timestamp: challenge.timestamp,
      encryptedKey: encodeBase64(encryptedKey),
      iv: encodeBase64(iv.buffer),
      ciphertext: encodeBase64(ciphertext),
    }
  } catch {
    throw new PasswordEnvelopeError('密码加密失败，请刷新页面后重试')
  } finally {
    new Uint8Array(rawAesKey).fill(0)
    passwordBytes.fill(0)
  }
}
