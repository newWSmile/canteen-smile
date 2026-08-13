import { createRouter, createWebHistory } from 'vue-router'
import { useTenantContextStore } from '@/app/store/tenantContext'
import { store } from '@/app/store'
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
          meta: { title: '机构类型与关系', permission: 'iam:org-type:view' },
        },
        {
          path: 'organizations',
          name: 'organizations',
          component: () => import('@/modules/organization/pages/OrganizationsPage.vue'),
          meta: { title: '机构树', permission: 'iam:org:view' },
        },
        {
          path: 'roles',
          name: 'roles',
          component: () => import('@/modules/role/pages/RolesPage.vue'),
          meta: { title: '角色与授权', permission: 'iam:role:view' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/modules/user/pages/UsersPage.vue'),
          meta: { title: '用户管理', permission: 'iam:user:view' },
        },
        {
          path: 'audit',
          name: 'tenant-audit',
          component: () => import('@/modules/audit/pages/AuditLogsPage.vue'),
          meta: { title: '审计日志', permission: 'iam:audit:view' },
        },
        {
          path: 'profile/security',
          name: 'profile-security',
          component: () => import('@/modules/profile/pages/ProfileSecurityPage.vue'),
          meta: { title: '个人安全' },
        },
        {
          path: 'profile/account',
          name: 'account-profile',
          component: () => import('@/modules/profile/pages/AccountProfilePage.vue'),
          meta: { title: '账号资料' },
        },
        {
          path: 'security/sessions',
          name: 'device-sessions',
          component: () => import('@/modules/security/pages/DeviceSessionsPage.vue'),
          meta: { title: '登录设备' },
        },
        {
          path: 'tenant/security',
          name: 'tenant-security-policy',
          component: () => import('@/modules/security/pages/TenantSecurityPolicyPage.vue'),
          meta: { title: '租户安全策略', permission: 'iam:tenant-security:view' },
        },
        {
          path: 'tenant/navigation',
          name: 'tenant-navigation-settings',
          component: () => import('@/modules/navigation/pages/TenantNavigationSettingsPage.vue'),
          meta: { title: '功能与菜单', permission: 'iam:tenant-navigation:view' },
        },
        {
          path: 'profile/menu-preferences',
          name: 'menu-preferences',
          component: () => import('@/modules/navigation/pages/MenuPreferencesPage.vue'),
          meta: { title: '菜单偏好' },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const publicRoute = to.name === 'login' || to.name === 'activate'
    || to.name === 'reset-password' || to.name === 'forgot-password'
  if (!publicRoute && !getTenantAdminToken()) {
    return { name: 'login' }
  }
  if (to.name === 'login' && getTenantAdminToken()) return { name: 'home' }
  if (!publicRoute) {
    const tenantContext = useTenantContextStore(store)
    if (!tenantContext.context) {
      try {
        await tenantContext.load()
      } catch {
        tenantContext.clear()
        return { name: 'login' }
      }
    }
    if (typeof to.meta.permission === 'string' && !tenantContext.hasPermission(to.meta.permission)) {
      return { name: 'home' }
    }
  }
  return true
})
