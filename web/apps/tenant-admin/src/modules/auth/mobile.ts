/** 将手机号输入限制为最多十一位数字。 */
export function normalizeMobileInput(value: string): string {
  return value.replace(/\D/g, '').slice(0, 11)
}

/** 判断是否为中国大陆十一位手机号。 */
export function isMainlandMobile(value: string): boolean {
  return /^1[3-9]\d{9}$/.test(value)
}
