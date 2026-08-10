import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCurrentSession } from '@/modules/auth/api/authApi'
import type { Session } from '@/modules/auth/types'

/** 平台管理端当前身份会话的唯一状态源。 */
export const usePlatformSessionStore = defineStore('platformSession', () => {
  const session = ref<Session | null>(null)
  const loading = ref(false)
  let loadingPromise: Promise<Session> | null = null

  /** 加载当前平台身份；并发调用复用同一次请求。 */
  async function load(): Promise<Session> {
    if (session.value) return session.value
    if (loadingPromise) return loadingPromise
    loading.value = true
    loadingPromise = getCurrentSession()
      .then((result) => {
        session.value = result
        return result
      })
      .finally(() => {
        loading.value = false
        loadingPromise = null
      })
    return loadingPromise
  }

  /** 清理退出登录后保留在内存中的平台身份。 */
  function clear(): void {
    session.value = null
  }

  return { session, loading, load, clear }
})
