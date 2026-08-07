<script setup lang="ts">
import { useTenantContextStore } from '@/app/store/tenantContext'

const tenantContext = useTenantContextStore()
</script>

<template>
  <section class="page-content">
    <p class="eyebrow">TENANT GOVERNANCE READY</p>
    <h2>在当前租户边界内治理机构、角色和账号。</h2>
    <p class="intro">权限由 Gateway 与 IAM 双重校验；当前页面数据来自 IAM 启动上下文。</p>
    <div class="cards">
      <article><span>租户</span><strong>{{ tenantContext.context?.tenantName || '—' }}</strong></article>
      <article><span>所属机构</span><strong>{{ tenantContext.context?.organizationName || '—' }}</strong></article>
      <article><span>身份</span><strong>{{ tenantContext.context?.rootOwner ? '租户根机构所有者' : '授权管理员' }}</strong></article>
    </div>
    <el-alert type="success" :closable="false" title="机构类型和机构树已进入可执行治理阶段。" />
  </section>
</template>

<style scoped>
.page-content { padding: 48px 36px; }
.eyebrow { color: #176b8c; font-size: 11px; font-weight: 700; letter-spacing: .15em; }
h2 { max-width: 900px; margin: 12px 0; font-size: clamp(30px,4vw,48px); line-height: 1.18; }
.intro { color: #73807f; }
.cards { margin: 32px 0; display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 16px; }
.cards article { padding: 24px; display: grid; gap: 12px; border: 1px solid #dce4e0; border-radius: 16px; background: #fff; }
.cards span { color: #7c8887; } .cards strong { font-size: 20px; overflow-wrap: anywhere; }
@media (max-width: 760px) { .page-content { padding: 28px 20px; } .cards { grid-template-columns: 1fr; } }
</style>
