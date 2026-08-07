<script setup lang="ts">
import { ApplicationBoundary, type BoundaryCard } from '@canteen-smile/ui'

const navigation = ['管理边界', '机构与账号', '角色与权限']
const cards: BoundaryCard[] = [
  { label: '机构治理', value: '租户内自治', detail: '机构类型、机构树与所有者保持租户隔离' },
  { label: '身份治理', value: '用户与角色', detail: '账号只归属一个机构，授权不越过管理边界' },
  { label: '授权治理', value: '功能与数据', detail: '菜单、按钮、接口和 SQL 数据范围统一约束' },
]
const nextSteps = [
  '等待 smile-iam-service 的真实接口契约',
  '按 modules 分包接入机构、用户和角色能力',
  '所有写操作保留单次提交与 Axios 并发保护',
]
</script>

<template>
  <ApplicationBoundary
    product-name="Canteen Smile"
    subtitle="租户管理端"
    workspace-label="Tenant Administration"
    heading="租户自治独立成端，管理能力不混入业务页面。"
    summary="机构所有者和授权管理员在这里管理本机构账号、角色与权限。当前不伪造 IAM 接口，先固定应用、Token 和模块边界。"
    :navigation="navigation"
    :cards="cards"
    :next-steps="nextSteps"
    accent="#176b8c"
    accent-soft="#bcecff"
  />
</template>
