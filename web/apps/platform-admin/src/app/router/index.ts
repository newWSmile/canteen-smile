import { createRouter, createWebHistory } from 'vue-router'
import { getPlatformToken } from '@/shared/http/client'

/** 平台管理端路由；仅装配平台身份可以访问的模块。 */
export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/modules/home/pages/HomePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/modules/auth/pages/LoginPage.vue'),
    },
    {
      path: '/bootstrap',
      name: 'bootstrap',
      component: () => import('@/modules/auth/pages/BootstrapPage.vue'),
    },
  ],
})

router.beforeEach((to) => {
  const authenticated = Boolean(getPlatformToken())
  if (to.meta.requiresAuth && !authenticated) return { name: 'login' }
  if (to.name === 'login' && authenticated) return { name: 'home' }
  return true
})
