<script setup lang="ts">
import { computed, type CSSProperties } from 'vue'
import type { BoundaryCard } from './types'

const props = defineProps<{
  productName: string
  subtitle: string
  workspaceLabel: string
  heading: string
  summary: string
  navigation: readonly string[]
  cards: readonly BoundaryCard[]
  nextSteps: readonly string[]
  accent: string
  accentSoft: string
}>()

const themeStyle = computed(
  () =>
    ({
      '--boundary-accent': props.accent,
      '--boundary-accent-soft': props.accentSoft,
    }) as CSSProperties,
)
</script>

<template>
  <div class="boundary-shell" :style="themeStyle">
    <aside class="boundary-sidebar">
      <div class="boundary-brand">
        <span class="boundary-brand__mark">CS</span>
        <div>
          <strong>{{ productName }}</strong>
          <small>{{ subtitle }}</small>
        </div>
      </div>
      <nav aria-label="应用边界导航">
        <span
          v-for="(item, index) in navigation"
          :key="item"
          :class="{ 'is-active': index === 0 }"
        >
          {{ item }}
        </span>
      </nav>
      <div class="boundary-sidebar__note">Vue 3 · Vite · TypeScript<br />独立构建与部署边界</div>
    </aside>

    <main class="boundary-main">
      <header class="boundary-topbar">
        <div>
          <p>{{ workspaceLabel }}</p>
          <h1>应用边界已就绪</h1>
        </div>
        <span class="boundary-status"><i /> 契约优先</span>
      </header>

      <section class="boundary-content">
        <div class="boundary-hero">
          <div>
            <span class="boundary-eyebrow">INDEPENDENT APPLICATION</span>
            <h2>{{ heading }}</h2>
            <p>{{ summary }}</p>
          </div>
          <div class="boundary-signal" aria-hidden="true">
            <span>APP</span><b>→</b><span>GATEWAY</span><b>→</b><span>SERVICE</span>
          </div>
        </div>

        <div class="boundary-grid">
          <article v-for="item in cards" :key="item.label">
            <small>{{ item.label }}</small>
            <strong>{{ item.value }}</strong>
            <p>{{ item.detail }}</p>
          </article>
        </div>

        <div class="boundary-next">
          <div>
            <span class="boundary-eyebrow">IMPLEMENTATION GATE</span>
            <h3>真实契约确认后接入</h3>
          </div>
          <ol>
            <li v-for="(step, index) in nextSteps" :key="step">
              <span>0{{ index + 1 }}</span>{{ step }}
            </li>
          </ol>
        </div>
      </section>
    </main>
  </div>
</template>

<style src="./application-boundary.css"></style>
