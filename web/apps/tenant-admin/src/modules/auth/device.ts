/** 租户管理端设备 ID 本地存储键。 */
const DEVICE_ID_KEY = 'canteen-smile:tenant-admin:device-id'

/** @return 使用 Web Crypto 随机数生成的 RFC 4122 v4 UUID。 */
function createDeviceId(): string {
  const bytes = window.crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6]! & 0x0f) | 0x40
  bytes[8] = (bytes[8]! & 0x3f) | 0x80
  const hex = Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

/** @return 当前浏览器稳定设备 ID */
export function getOrCreateDeviceId(): string {
  const existing = localStorage.getItem(DEVICE_ID_KEY)
  if (existing) return existing
  const created = createDeviceId()
  localStorage.setItem(DEVICE_ID_KEY, created)
  return created
}
