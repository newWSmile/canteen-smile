import { createRouter, createWebHistory } from 'vue-router'

/** 租户管理端路由；仅装配租户与机构管理模块。 */
export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/modules/home/pages/HomePage.vue'),
    },
  ],
})
