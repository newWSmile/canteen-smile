import { createPasswordEncryptionChallenge } from './api/authApi'
import type { PasswordEncryptionChallenge, PasswordEnvelopePurpose, PasswordEnvelopeRequest } from './types'

/** 浏览器不能安全完成密码加密时的本地错误。 */
export class PasswordEnvelopeError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'PasswordEnvelopeError'
  }
}

/** @param value Base64 文本 @return 二进制缓冲区 */
function decodeBase64(value: string): ArrayBuffer {
  const binary = window.atob(value)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index)
  return bytes.buffer
}

/** @param value 二进制数据 @return Base64 文本 */
function encodeBase64(value: ArrayBuffer): string {
  const bytes = new Uint8Array(value)
  let binary = ''
  for (const byte of bytes) binary += String.fromCharCode(byte)
  return window.btoa(binary)
}

/** @param challenge 服务端挑战 @return AES-GCM 附加认证数据 */
function additionalData(challenge: PasswordEncryptionChallenge): Uint8Array {
  return new TextEncoder().encode(
    [challenge.purpose, challenge.keyId, challenge.nonce, challenge.timestamp.toString()].join('\n'),
  )
}

/** 使用 RSA-OAEP 包装随机 AES-256 密钥并通过 AES-GCM 加密密码。 */
export async function encryptPassword(
  password: string,
  purpose: PasswordEnvelopePurpose,
): Promise<PasswordEnvelopeRequest> {
  if (!window.isSecureContext || !window.crypto?.subtle) {
    throw new PasswordEnvelopeError('当前页面不是安全上下文，无法执行密码加密')
  }
  const challenge = await createPasswordEncryptionChallenge(purpose)
  if (challenge.purpose !== purpose
    || challenge.keyAlgorithm !== 'RSA-OAEP-256'
    || challenge.contentAlgorithm !== 'A256GCM') {
    throw new PasswordEnvelopeError('服务端密码加密挑战不受支持')
  }
  const aad = additionalData(challenge)
  const iv = window.crypto.getRandomValues(new Uint8Array(12))
  const passwordBytes = new TextEncoder().encode(password)
  const publicKey = await window.crypto.subtle.importKey(
    'spki', decodeBase64(challenge.publicKey), { name: 'RSA-OAEP', hash: 'SHA-256' }, false, ['encrypt'],
  )
  const aesKey = await window.crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, true, ['encrypt'])
  const rawAesKey = await window.crypto.subtle.exportKey('raw', aesKey)
  try {
    const encryptedKey = await window.crypto.subtle.encrypt(
      { name: 'RSA-OAEP', label: aad }, publicKey, rawAesKey,
    )
    const ciphertext = await window.crypto.subtle.encrypt(
      { name: 'AES-GCM', iv, additionalData: aad, tagLength: 128 }, aesKey, passwordBytes,
    )
    return {
      keyId: challenge.keyId,
      nonce: challenge.nonce,
      timestamp: challenge.timestamp,
      encryptedKey: encodeBase64(encryptedKey),
      iv: encodeBase64(iv.buffer),
      ciphertext: encodeBase64(ciphertext),
    }
  } finally {
    new Uint8Array(rawAesKey).fill(0)
    passwordBytes.fill(0)
  }
}
