<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppBreadcrumb, type BreadcrumbItem } from '@canteen-smile/ui'

const route = useRoute()
const router = useRouter()

/** 根据嵌套路由生成可点击的租户业务面包屑。 */
const breadcrumbItems = computed<BreadcrumbItem[]>(() => {
  const records = route.matched.filter((record) => typeof record.meta.title === 'string')
  return records.map((record, index) => ({
    label: record.meta.title as string,
    to: index < records.length - 1
      ? record.name
        ? router.resolve({ name: record.name }).path
        : record.path
      : undefined,
  }))
})

const currentTitle = computed(() =>
  typeof route.meta.title === 'string' ? route.meta.title : '租户业务端',
)
</script>

<template>
  <div class="portal-shell">
    <aside class="portal-sidebar">
      <div class="brand"><span>CS</span><strong>Canteen Smile</strong></div>
      <p class="workspace">TENANT BUSINESS PORTAL</p>
      <nav aria-label="租户业务端主导航">
        <RouterLink :class="{ active: route.name === 'home' }" :to="{ name: 'home' }">业务工作台</RouterLink>
        <span class="coming">个人工作台 · 待业务契约</span>
        <span class="coming">安全中心 · 待身份契约</span>
      </nav>
      <div class="boundary">
        <strong>业务访问边界</strong>
        <small>菜单、按钮和数据范围以服务端授权为准</small>
      </div>
    </aside>

    <main class="portal-main">
      <header class="portal-header">
        <div class="page-identity">
          <AppBreadcrumb :items="breadcrumbItems" @navigate="router.push" />
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="application-status"><i /> 契约优先</div>
      </header>
      <section class="portal-content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<style scoped>
.portal-shell { min-height: 100vh; display: grid; grid-template-columns: 244px minmax(0, 1fr); color: #18201c; background: #f2f4f1; }
.portal-sidebar { position: sticky; top: 0; height: 100vh; padding: 28px 20px; display: flex; flex-direction: column; color: #ecf6ef; background: #173a2a; }
.brand { display: flex; align-items: center; gap: 12px; }
.brand span { width: 38px; height: 38px; display: grid; place-items: center; border: 1px solid #5f8270; border-radius: 12px; font-size: 12px; }
.workspace { margin: 30px 8px 12px; color: #85a994; font-size: 10px; letter-spacing: .12em; }
nav { display: grid; gap: 6px; }
nav a, nav .coming { min-height: 44px; padding: 13px 14px; color: #adc8b8; border-radius: 10px; text-decoration: none; }
nav a.active { color: #173a2a; background: #d8f36b; font-weight: 700; }
nav .coming { opacity: .55; }
.boundary { margin-top: auto; padding: 16px; display: grid; gap: 7px; border: 1px solid #355d48; border-radius: 14px; }
.boundary small { color: #8fb29d; line-height: 1.55; }
.portal-main { min-width: 0; }
.portal-header { position: sticky; top: 0; z-index: 20; min-height: 92px; padding: 18px 36px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #dce3de; background: rgba(255,255,255,.94); backdrop-filter: blur(12px); }
.page-identity { display: grid; gap: 4px; --breadcrumb-link: #317151; --breadcrumb-link-hover: #1f5339; }
.page-identity h1 { margin: 0; font-size: 22px; }
.application-status { display: flex; align-items: center; gap: 8px; color: #53705f; font-size: 13px; }
.application-status i { width: 8px; height: 8px; border-radius: 50%; background: #45b97c; box-shadow: 0 0 0 5px #e4f5eb; }
.portal-content { min-width: 0; padding: 36px; }
@media (max-width: 760px) { .portal-shell { grid-template-columns: 1fr; } .portal-sidebar { display: none; } .portal-header, .portal-content { padding-left: 20px; padding-right: 20px; } }
</style>
