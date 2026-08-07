<script setup lang="ts">
import { ApplicationBoundary, type BoundaryCard } from '@canteen-smile/ui'

const navigation = ['业务边界', '个人工作台', '安全中心']
const cards: BoundaryCard[] = [
  { label: '应用边界', value: '租户业务', detail: '普通业务功能与平台、租户管理能力分离' },
  { label: '授权边界', value: '菜单与按钮', detail: '只展示有效角色获得且租户允许显示的能力' },
  { label: '数据边界', value: 'SQL 最终校验', detail: '服务端按租户、机构与业务归属限制数据范围' },
]
const nextSteps = [
  '等待真实登录和账号选择接口契约',
  '按已确认业务领域建立独立 modules',
  '禁止以静态假数据代替后端业务实现',
]
</script>

<template>
  <ApplicationBoundary
    product-name="Canteen Smile"
    subtitle="租户业务端"
    workspace-label="Tenant Business Portal"
    heading="日常业务独立运行，只承载当前账号获准的能力。"
    summary="本应用面向租户业务人员。登录、菜单和数据范围必须依据真实 Auth/IAM 契约，当前页面只表明应用边界，不代表业务接口已经存在。"
    :navigation="navigation"
    :cards="cards"
    :next-steps="nextSteps"
    accent="#1f6a43"
    accent-soft="#d8f36b"
  />
</template>
