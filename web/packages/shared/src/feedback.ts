import { ElMessage } from 'element-plus'

/** 统一页面反馈服务，业务模块不得直接调用组件库提示 API。 */
export const feedback = {
  /** 展示成功提示。 */
  success(message: string): void {
    ElMessage.success({ message, showClose: true })
  },
  /** 展示失败提示。 */
  error(message: string): void {
    ElMessage.error({ message, showClose: true })
  },
  /** 展示警告提示。 */
  warning(message: string): void {
    ElMessage.warning({ message, showClose: true })
  },
} as const
