import { ref, type Ref } from 'vue'

/** 单次执行控制器返回值。 */
export interface SingleFlight<TArgs extends unknown[], TResult> {
  pending: Readonly<Ref<boolean>>
  run: (...args: TArgs) => Promise<TResult | undefined>
}

/**
 * 防止用户连续点击触发同一异步操作。
 * 调用方必须将 pending 绑定到按钮 loading/disabled 状态。
 */
export function useSingleFlight<TArgs extends unknown[], TResult>(
  action: (...args: TArgs) => Promise<TResult>,
): SingleFlight<TArgs, TResult> {
  const pending = ref(false)

  const run = async (...args: TArgs): Promise<TResult | undefined> => {
    if (pending.value) return undefined

    pending.value = true
    try {
      return await action(...args)
    } finally {
      pending.value = false
    }
  }

  return { pending, run }
}
