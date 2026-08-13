import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getTenantManagementContext } from '@/modules/tenant/api/tenantApi'
import type { TenantManagementContext } from '@/modules/tenant/types'

/** 当前租户管理端身份与权限的唯一状态源。 */
export const useTenantContextStore = defineStore('tenantContext', () => {
  const context = ref<TenantManagementContext | null>(null)
  const loading = ref(false)

  /** 加载真实 IAM 租户上下文。 */
  async function load(): Promise<TenantManagementContext> {
    loading.value = true
    try {
      context.value = await getTenantManagementContext()
      return context.value
    } finally {
      loading.value = false
    }
  }

  /** 清理退出账号留下的内存上下文。 */
  function clear(): void {
    context.value = null
  }

  /** 判断后端返回的最终权限集合。 */
  function hasPermission(permission: string): boolean {
    return context.value?.permissions.includes(permission) ?? false
  }

  /** 判断菜单是否同时未被租户统一配置和当前账号个人偏好隐藏。 */
  function isMenuVisible(permission: string): boolean {
    if (permission === 'iam:tenant-navigation:view') return true
    return !(context.value?.hiddenMenuPermissionCodes.includes(permission) ?? false)
  }

  return {
    context,
    loading,
    tenantName: computed(() => context.value?.tenantName ?? '租户管理端'),
    displayName: computed(() => context.value?.displayName || context.value?.username || '—'),
    load,
    clear,
    hasPermission,
    isMenuVisible,
  }
})
