const DEVICE_ID_KEY = 'canteen-smile:platform-admin:device-id'

/** @return 当前浏览器稳定设备 ID，不包含硬件指纹 */
export function getOrCreateDeviceId(): string {
  const existing = localStorage.getItem(DEVICE_ID_KEY)
  if (existing) return existing
  const created = crypto.randomUUID()
  localStorage.setItem(DEVICE_ID_KEY, created)
  return created
}
