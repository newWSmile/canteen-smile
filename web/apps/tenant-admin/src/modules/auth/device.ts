/** 租户管理端设备 ID 本地存储键。 */
const DEVICE_ID_KEY = 'canteen-smile:tenant-admin:device-id'

/** @return 当前浏览器稳定设备 ID */
export function getOrCreateDeviceId(): string {
  const existing = localStorage.getItem(DEVICE_ID_KEY)
  if (existing) return existing
  const created = crypto.randomUUID()
  localStorage.setItem(DEVICE_ID_KEY, created)
  return created
}
