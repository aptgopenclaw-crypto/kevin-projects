import { createRouter, createWebHistory } from 'vue-router'
import { publicRoutes } from '@/router/publicRoutes'

// noauth routes — no login required
const noauthRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/select-tenant',
    name: 'SelectTenant',
    component: () => import('@/views/tenant/SelectTenantView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/login/ForgotPasswordView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/login/ResetPasswordView.vue'),
    meta: { requiresAuth: false },
  },
]

// Static admin routes (kept for backward compatibility — also injected via menus dynamically)
const staticAdminRoutes = [
  {
    path: '/admin/users',
    name: 'UserList',
    component: () => import('@/views/admin/UserListView.vue'),
  },
  {
    path: '/admin/users/create',
    name: 'CreateUser',
    component: () => import('@/views/admin/CreateUserView.vue'),
  },
  {
    path: '/admin/users/:userId/edit',
    name: 'EditUser',
    component: () => import('@/views/admin/EditUserView.vue'),
  },
  {
    path: '/admin/system/menus',
    name: 'MenuManage',
    component: () => import('@/views/admin/menu/MenuManageView.vue'),
  },
  {
    path: '/admin/system/tenants',
    name: 'TenantManage',
    component: () => import('@/views/admin/tenant/TenantManageView.vue'),
    meta: { superAdminOnly: true },
  },
  {
    path: '/admin/system/roles',
    name: 'RolePermission',
    component: () => import('@/views/admin/role/RolePermissionView.vue'),
  },
  {
    path: '/admin/audit/history',
    name: 'AuditHistory',
    component: () => import('@/views/admin/audit/AuditHistoryView.vue'),
  },
  {
    path: '/admin/system/settings',
    name: 'SystemSettings',
    component: () => import('@/views/admin/setting/SystemSettingsView.vue'),
  },
  {
    path: '/admin/system/announcements',
    name: 'AnnouncementManagement',
    component: () => import('@/views/admin/announcement/AnnouncementManagementView.vue'),
  },
  // ── IoT / Smart Streetlight ──
  {
    path: '/admin/iot/devices',
    name: 'IoTDevice',
    component: () => import('@/views/admin/iot/DeviceListView.vue'),
  },
  {
    path: '/admin/iot/telemetry-formats',
    name: 'TelemetryFormat',
    component: () => import('@/views/admin/iot/TelemetryFormatView.vue'),
  },
  {
    path: '/admin/iot/telemetry',
    name: 'TelemetryData',
    component: () => import('@/views/admin/iot/TelemetryDataView.vue'),
  },
  {
    path: '/admin/iot/event-rules',
    name: 'EventRule',
    component: () => import('@/views/admin/iot/EventRuleView.vue'),
  },
  {
    path: '/admin/iot/alerts',
    name: 'IoTAlertHistory',
    component: () => import('@/views/admin/iot/AlertHistoryView.vue'),
  },
  {
    path: '/admin/iot/dimming',
    name: 'DimmingControl',
    component: () => import('@/views/admin/iot/DimmingControlView.vue'),
  },
  {
    path: '/admin/iot/dimming/groups',
    name: 'DimmingGroup',
    component: () => import('@/views/admin/iot/DimmingGroupView.vue'),
  },
  {
    path: '/admin/iot/dimming/schedules',
    name: 'DimmingSchedule',
    component: () => import('@/views/admin/iot/DimmingScheduleView.vue'),
  },
  {
    path: '/admin/iot/map',
    name: 'IoTMap',
    component: () => import('@/views/admin/iot/IoTMapView.vue'),
  },
  {
    path: '/admin/iot/meters',
    name: 'MeterStatus',
    component: () => import('@/views/admin/iot/MeterStatusView.vue'),
  },
  // ── Tender 招標/決標 ──
  {
    path: '/tender/dashboard',
    name: 'TenderDashboard',
    component: () => import('@/views/tender/TenderDashboardView.vue'),
  },
  {
    path: '/tender/announcements',
    name: 'TenderAnnouncements',
    component: () => import('@/views/tender/TenderAnnouncementView.vue'),
  },
  {
    path: '/tender/awards',
    name: 'TenderAwards',
    component: () => import('@/views/tender/TenderAwardView.vue'),
  },
  {
    path: '/tender/vendor-dashboard',
    name: 'VendorDashboard',
    component: () => import('@/views/tender/VendorDashboardView.vue'),
  },
  {
    path: '/tender/search-keywords',
    name: 'TenderSearchKeywords',
    component: () => import('@/views/tender/SearchKeywordView.vue'),
  },
  {
    path: '/tender/agency-filters',
    name: 'TenderAgencyFilters',
    component: () => import('@/views/tender/AgencyFilterView.vue'),
  },
  {
    path: '/tender/mail-recipients',
    name: 'TenderMailRecipients',
    component: () => import('@/views/tender/MailRecipientView.vue'),
  },
  {
    path: '/tender/ai-chat',
    name: 'TenderAiChat',
    component: () => import('@/views/tender/TenderAiChatView.vue'),
  },
  {
    path: '/tender/solution-competitor',
    name: 'SolutionCompetitor',
    component: () => import('@/views/tender/SolutionCompetitorView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    ...noauthRoutes,
    ...publicRoutes,
    ...staticAdminRoutes,
  ],
})

router.beforeEach(async (to, _from, next) => {
  // 1. noauth routes → pass through
  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  // 2. Not logged in → /login
  const passExam = sessionStorage.getItem('passExam')
  if (!passExam) {
    next('/login')
    return
  }

  // 3. Bootstrap: first protected route access — load user info + menus
  const { useAuthStore } = await import('@/stores/authStore')
  const authStore = useAuthStore()

  authStore._initAxiosCallbacks()

  const { useMenuStore } = await import('@/stores/menuStore')
  const menuStore = useMenuStore()

  if (!menuStore.initialized) {
    // Restore session: if access token was lost (page refresh), use refresh token cookie
    if (!authStore.accessToken) {
      try {
        await authStore.restoreSession()
      } catch {
        authStore.clearAuth()
        next('/login')
        return
      }
    }

    try {
      if (!authStore.userInfo) {
        await authStore.getPermission()
      }
      await menuStore.fetchMyMenus()

      // Load dept options once after login (cached in store)
      const { useDeptStore } = await import('@/stores/deptStore')
      const deptStore = useDeptStore()
      if (!deptStore.initialized) {
        try {
          await deptStore.fetchDeptOptions()
        } catch {
          // Degradation: dept options failed — forms will show empty dept selector
        }
      }
    } catch {
      // Non-auth error: degrade gracefully (show fallback menus)
      if (!menuStore.initialized) {
        menuStore.initialized = true
      }
    }
    // Re-enter to match dynamically added routes
    next({ ...to, replace: true })
    return
  }

  // 4. publicRoutes → only check login (already checked above)
  if (to.meta.isPublic) {
    next()
    return
  }

  // 5. super-admin-only routes (not in backend menu system)
  if (to.meta.superAdminOnly) {
    const isSuperAdmin = authStore.userInfo?.isSuperAdmin === true
    if (!isSuperAdmin) next('/')
    else next()
    return
  }

  // 6. private routes → check menu-based access
  if (!menuStore.hasRouteAccess(to.name as string)) {
    next('/')
    return
  }

  next()
})

export default router
