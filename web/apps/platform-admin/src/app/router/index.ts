import { createRouter, createWebHistory } from 'vue-router'
import { getPlatformToken } from '@/shared/http/client'

/** 平台管理端路由；受保护页面统一装配在后台布局的右侧内容区。 */
export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'platform',
      component: () => import('@/app/layouts/PlatformAdminLayout.vue'),
      meta: { requiresAuth: true, title: '平台治理' },
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/modules/home/pages/HomePage.vue'),
          meta: { requiresAuth: true, title: '租户治理' },
        },
        {
          path: 'org-type-templates',
          name: 'org-type-templates',
          component: () => import('@/modules/tenant/pages/OrgTypeTemplatePage.vue'),
          meta: { requiresAuth: true, title: '机构类型模板' },
        },
        {
          path: 'permission-resources',
          name: 'permission-resources',
          component: () => import('@/modules/permission/pages/PermissionResourcesPage.vue'),
          meta: { requiresAuth: true, title: '权限资源' },
        },
        {
          path: 'audit',
          name: 'platform-audit',
          component: () => import('@/modules/audit/pages/AuditLogsPage.vue'),
          meta: { requiresAuth: true, title: '平台审计' },
        },
        {
          path: 'sms',
          name: 'sms-management',
          component: () => import('@/modules/sms/layouts/SmsManagementLayout.vue'),
          redirect: { name: 'sms-deliveries' },
          meta: { requiresAuth: true, title: '短信管理' },
          children: [
            {
              path: 'deliveries',
              name: 'sms-deliveries',
              component: () => import('@/modules/sms/pages/SmsDeliveriesPage.vue'),
              meta: { requiresAuth: true, title: '短信列表' },
            },
            {
              path: 'settings',
              name: 'sms-settings',
              component: () => import('@/modules/sms/pages/SmsSettingsPage.vue'),
              meta: { requiresAuth: true, title: '短信设置' },
            },
            {
              path: 'security',
              name: 'sms-security',
              component: () => import('@/modules/sms/pages/SmsSecurityPage.vue'),
              meta: { requiresAuth: true, title: '短信安全' },
            },
          ],
        },
        {
          path: 'sms-deliveries',
          redirect: { name: 'sms-deliveries' },
        },
      ],
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
