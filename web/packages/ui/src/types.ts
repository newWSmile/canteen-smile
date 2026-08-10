/** 应用边界概览卡片。 */
export interface BoundaryCard {
  label: string
  value: string
  detail: string
}

/** 后台应用面包屑节点。 */
export interface BreadcrumbItem {
  /** 节点显示名称。 */
  label: string
  /** 非当前节点的站内跳转地址；缺省时仅展示文本。 */
  to?: string
}
