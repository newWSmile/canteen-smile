<script setup lang="ts">
import type { BreadcrumbItem } from './types'

defineProps<{
  /** 按从应用根节点到当前页面排列的面包屑节点。 */
  items: readonly BreadcrumbItem[]
}>()

const emit = defineEmits<{
  /** 请求宿主应用通过自身路由执行站内导航。 */
  navigate: [to: string]
}>()

function navigate(event: MouseEvent, to: string): void {
  event.preventDefault()
  emit('navigate', to)
}
</script>

<template>
  <nav class="app-breadcrumb" aria-label="面包屑导航">
    <template v-for="(item, index) in items" :key="`${item.label}-${index}`">
      <span v-if="index > 0" class="app-breadcrumb__separator" aria-hidden="true">/</span>
      <a v-if="item.to" class="app-breadcrumb__link" :href="item.to" @click="navigate($event, item.to)">
        {{ item.label }}
      </a>
      <span v-else class="app-breadcrumb__current" aria-current="page">{{ item.label }}</span>
    </template>
  </nav>
</template>

<style scoped>
.app-breadcrumb {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  color: var(--breadcrumb-muted, #8a8790);
  font-size: 12px;
  line-height: 1.5;
}

.app-breadcrumb__link {
  color: var(--breadcrumb-link, #6550a7);
  text-decoration: none;
  transition: color .18s ease;
}

.app-breadcrumb__link:hover {
  color: var(--breadcrumb-link-hover, #4d348f);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.app-breadcrumb__separator {
  color: var(--breadcrumb-separator, #bbb7c0);
}

.app-breadcrumb__current {
  color: var(--breadcrumb-current, #77737d);
}
</style>
