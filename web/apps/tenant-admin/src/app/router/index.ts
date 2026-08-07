import { createRouter, createWebHistory } from 'vue-router'
import { getTenantAdminToken } from '@/shared/http/client'

/** 租户管理端路由；仅装配租户与机构管理模块。 */
export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/activate',
      name: 'activate',
      component: () => import('@/modules/auth/pages/ActivationPage.vue'),
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/modules/auth/pages/LoginPage.vue'),
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('@/modules/auth/pages/PasswordResetPage.vue'),
    },
    {
      path: '/',
      component: () => import('@/app/layouts/TenantAdminLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/modules/home/pages/HomePage.vue'),
          meta: { title: '管理概览', breadcrumb: '租户管理 / 概览' },
        },
        {
          path: 'organization-types',
          name: 'organization-types',
          component: () => import('@/modules/organization/pages/OrganizationTypesPage.vue'),
          meta: { title: '机构类型与关系', breadcrumb: '租户管理 / 机构类型' },
        },
        {
          path: 'organizations',
          name: 'organizations',
          component: () => import('@/modules/organization/pages/OrganizationsPage.vue'),
          meta: { title: '机构树', breadcrumb: '租户管理 / 机构树' },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.name !== 'login' && to.name !== 'activate' && to.name !== 'reset-password' && !getTenantAdminToken()) {
    return { name: 'login' }
  }
  if (to.name === 'login' && getTenantAdminToken()) return { name: 'home' }
  return true
})
