<script setup lang="ts">
import type { BoundaryCard } from '@canteen-smile/ui'

const cards: BoundaryCard[] = [
  { label: '应用边界', value: '租户业务', detail: '普通业务功能与平台、租户管理能力分离。' },
  { label: '授权边界', value: '菜单与按钮', detail: '只展示有效角色获得且租户允许显示的能力。' },
  { label: '数据边界', value: 'SQL 最终校验', detail: '服务端按租户、机构与业务归属限制数据范围。' },
]

const nextSteps = [
  '等待真实登录和账号选择接口契约',
  '按已确认业务领域建立独立 modules',
  '禁止以静态假数据替代后端业务实现',
]
</script>

<template>
  <section class="portal-page">
    <div class="hero">
      <div>
        <span class="eyebrow">TENANT BUSINESS PORTAL</span>
        <h2>日常业务独立运行，只承载当前账号获准的能力。</h2>
        <p>登录、菜单和数据范围必须依据真实 Auth/IAM 契约；当前页面只表明应用边界，不代表业务接口已经存在。</p>
      </div>
      <div class="signal" aria-hidden="true"><span>APP</span><b>→</b><span>GATEWAY</span><b>→</b><span>SERVICE</span></div>
    </div>

    <div class="card-grid">
      <article v-for="item in cards" :key="item.label">
        <small>{{ item.label }}</small>
        <strong>{{ item.value }}</strong>
        <p>{{ item.detail }}</p>
      </article>
    </div>

    <section class="implementation-gate">
      <div><span class="eyebrow">IMPLEMENTATION GATE</span><h3>真实契约确认后接入</h3></div>
      <ol>
        <li v-for="(step, index) in nextSteps" :key="step"><span>0{{ index + 1 }}</span>{{ step }}</li>
      </ol>
    </section>
  </section>
</template>

<style scoped>
.portal-page { max-width: 1280px; margin: 0 auto; }
.hero { min-height: 300px; padding: 38px; display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(300px, .6fr); gap: 32px; align-items: center; color: #eff8f1; border-radius: 24px; background: linear-gradient(135deg, #173a2a, #265d43); }
.eyebrow { color: #d8f36b; font-size: 11px; font-weight: 800; letter-spacing: .15em; }
.hero h2 { max-width: 760px; margin: 14px 0; font-size: clamp(30px, 4vw, 52px); line-height: 1.08; letter-spacing: -.04em; }
.hero p { max-width: 720px; margin: 0; color: #c3d8ca; line-height: 1.8; }
.signal { display: flex; align-items: center; justify-content: center; gap: 10px; flex-wrap: wrap; }
.signal span { padding: 12px 15px; color: #173a2a; border-radius: 11px; background: #d8f36b; font-size: 11px; font-weight: 800; }
.signal b { color: #8eb7a0; }
.card-grid { margin: 22px 0; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.card-grid article { min-height: 172px; padding: 24px; display: grid; align-content: start; gap: 10px; border: 1px solid #dbe3dd; border-radius: 18px; background: #fff; }
.card-grid small { color: #74837a; }
.card-grid strong { font-size: 24px; }
.card-grid p { margin: 0; color: #6c786f; line-height: 1.65; }
.implementation-gate { padding: 28px; display: grid; grid-template-columns: .7fr 1.3fr; gap: 30px; border: 1px solid #dbe3dd; border-radius: 20px; background: #fff; }
.implementation-gate .eyebrow { color: #317151; }
.implementation-gate h3 { margin: 8px 0 0; font-size: 25px; }
.implementation-gate ol { margin: 0; padding: 0; display: grid; gap: 10px; list-style: none; }
.implementation-gate li { display: flex; align-items: center; gap: 12px; color: #5f6f65; }
.implementation-gate li span { width: 34px; height: 34px; display: grid; place-items: center; flex: none; color: #24573e; border-radius: 10px; background: #eef8c9; font-weight: 700; }
@media (max-width: 900px) { .hero { grid-template-columns: 1fr; } .signal { justify-content: flex-start; } .card-grid { grid-template-columns: 1fr; } .implementation-gate { grid-template-columns: 1fr; } }
@media (max-width: 600px) { .hero { padding: 26px; } }
</style>
