import { createRouter, createWebHistory } from 'vue-router'

/** 租户业务端路由；业务页面必须继续按 modules 领域分包。 */
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
