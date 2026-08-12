import { createRouter, createWebHistory } from 'vue-router'

/** 租户业务端路由；业务页面统一装配在应用布局的右侧内容区。 */
export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'tenant-business',
      component: () => import('@/app/layouts/TenantPortalLayout.vue'),
      meta: { title: '业务工作台' },
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/modules/home/pages/HomePage.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'security/sessions',
          name: 'device-sessions',
          component: () => import('@/modules/security/pages/DeviceSessionsPage.vue'),
          meta: { title: '登录设备' },
        },
      ],
    },
  ],
})
