import { createRouter, createWebHistory } from 'vue-router'
import { getTenantAdminToken } from '@/shared/http/client'

/** 租户管理端路由；受保护页面统一装配在后台布局的右侧内容区。 */
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
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('@/modules/auth/pages/ForgotPasswordPage.vue'),
    },
    {
      path: '/',
      name: 'tenant-management',
      component: () => import('@/app/layouts/TenantAdminLayout.vue'),
      meta: { title: '租户管理' },
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/modules/home/pages/HomePage.vue'),
          meta: { title: '管理概览' },
        },
        {
          path: 'organization-types',
          name: 'organization-types',
          component: () => import('@/modules/organization/pages/OrganizationTypesPage.vue'),
          meta: { title: '机构类型与关系' },
        },
        {
          path: 'organizations',
          name: 'organizations',
          component: () => import('@/modules/organization/pages/OrganizationsPage.vue'),
          meta: { title: '机构树' },
        },
        {
          path: 'roles',
          name: 'roles',
          component: () => import('@/modules/role/pages/RolesPage.vue'),
          meta: { title: '角色与授权' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/modules/user/pages/UsersPage.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'audit',
          name: 'tenant-audit',
          component: () => import('@/modules/audit/pages/AuditLogsPage.vue'),
          meta: { title: '审计日志' },
        },
        {
          path: 'profile/security',
          name: 'profile-security',
          component: () => import('@/modules/profile/pages/ProfileSecurityPage.vue'),
          meta: { title: '个人安全' },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.name !== 'login' && to.name !== 'activate' && to.name !== 'reset-password' && to.name !== 'forgot-password' && !getTenantAdminToken()) {
    return { name: 'login' }
  }
  if (to.name === 'login' && getTenantAdminToken()) return { name: 'home' }
  return true
})
