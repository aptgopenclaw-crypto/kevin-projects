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
  // ── Dashboard ──
  {
    path: '/admin/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/admin/dashboard/DashboardView.vue'),
  },
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
  {
    path: '/admin/asset/devices',
    name: 'DeviceManagement',
    component: () => import('@/views/admin/asset/DeviceManagementView.vue'),
  },
  {
    path: '/admin/asset/circuits',
    name: 'CircuitManagement',
    component: () => import('@/views/admin/asset/CircuitManagementView.vue'),
  },
  {
    path: '/admin/asset/contracts',
    name: 'ContractManagement',
    component: () => import('@/views/admin/asset/ContractManagementView.vue'),
  },
  {
    path: '/admin/asset/topology',
    name: 'DeviceTopology',
    component: () => import('@/views/admin/asset/DeviceTopologyView.vue'),
  },
  {
    path: '/admin/asset/schema-designer',
    name: 'SchemaDesigner',
    component: () => import('@/views/admin/asset/SchemaDesignerView.vue'),
  },
  {
    path: '/admin/asset/faults',
    name: 'FaultTicket',
    component: () => import('@/views/admin/asset/FaultTicketView.vue'),
  },
  {
    path: '/admin/asset/fault-correlations',
    name: 'FaultCorrelation',
    component: () => import('@/views/admin/asset/FaultCorrelationView.vue'),
  },
  {
    path: '/admin/workflow/pending',
    name: 'PendingTasks',
    component: () => import('@/views/admin/workflow/PendingTasksView.vue'),
  },
  {
    path: '/admin/workflow/delegates',
    name: 'DelegateSettings',
    component: () => import('@/views/admin/workflow/DelegateSettingsView.vue'),
  },
  {
    path: '/admin/repair/tickets',
    name: 'RepairTicket',
    component: () => import('@/views/admin/repair/RepairTicketView.vue'),
  },
  {
    path: '/admin/repair/tickets/:id',
    name: 'RepairTicketDetail',
    component: () => import('@/views/admin/repair/RepairTicketDetailView.vue'),
  },
  {
    path: '/admin/repair/inspection',
    name: 'InspectionTask',
    component: () => import('@/views/admin/repair/InspectionView.vue'),
  },
  {
    path: '/admin/repair/inspection/:taskId/records',
    name: 'InspectionRecord',
    component: () => import('@/views/admin/repair/InspectionRecordView.vue'),
  },
  // ── Material Management ──
  {
    path: '/admin/material/specs',
    name: 'MaterialSpec',
    component: () => import('@/views/admin/material/MaterialSpecView.vue'),
  },
  {
    path: '/admin/material/warehouses',
    name: 'WarehouseManagement',
    component: () => import('@/views/admin/material/WarehouseView.vue'),
  },
  {
    path: '/admin/material/suppliers',
    name: 'SupplierManagement',
    component: () => import('@/views/admin/material/SupplierView.vue'),
  },
  {
    path: '/admin/material/inventory',
    name: 'InventoryManagement',
    component: () => import('@/views/admin/material/InventoryView.vue'),
  },
  {
    path: '/admin/material/purchase-orders',
    name: 'PurchaseOrder',
    component: () => import('@/views/admin/material/PurchaseOrderView.vue'),
  },
  {
    path: '/admin/material/approved-materials',
    name: 'ApprovedMaterial',
    component: () => import('@/views/admin/material/ApprovedMaterialView.vue'),
  },
  {
    path: '/admin/material/receiving',
    name: 'ReceivingRecord',
    component: () => import('@/views/admin/material/ReceivingView.vue'),
  },
  {
    path: '/admin/material/issue-requests',
    name: 'IssueRequest',
    component: () => import('@/views/admin/material/IssueRequestView.vue'),
  },
  {
    path: '/admin/material/adjustments',
    name: 'InventoryAdjustment',
    component: () => import('@/views/admin/material/AdjustmentView.vue'),
  },
  {
    path: '/admin/material/disposals',
    name: 'DisposalRecord',
    component: () => import('@/views/admin/material/DisposalView.vue'),
  },
  // ── Replacement Maintenance ──
  {
    path: '/admin/replacement/orders',
    name: 'ReplacementOrder',
    component: () => import('@/views/admin/replacement/ReplacementOrderView.vue'),
  },
  {
    path: '/admin/replacement/orders/:id',
    name: 'ReplacementOrderDetail',
    component: () => import('@/views/admin/replacement/ReplacementOrderDetailView.vue'),
  },
  {
    path: '/admin/replacement/orders/:id/self-check',
    name: 'ReplacementSelfCheck',
    component: () => import('@/views/admin/replacement/SelfCheckView.vue'),
  },
  {
    path: '/admin/replacement/pole-numbers',
    name: 'PoleNumber',
    component: () => import('@/views/admin/replacement/PoleNumberView.vue'),
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
  // ── GIS ──
  {
    path: '/admin/gis/map',
    name: 'GisMap',
    component: () => import('@/views/admin/gis/GisMapView.vue'),
  },
  // ── KPI / Performance Management ──
  {
    path: '/admin/kpi/indicators',
    name: 'KpiIndicators',
    component: () => import('@/views/admin/kpi/IndicatorView.vue'),
  },
  {
    path: '/admin/kpi/data',
    name: 'KpiData',
    component: () => import('@/views/admin/kpi/DataView.vue'),
  },
  {
    path: '/admin/kpi/calculate',
    name: 'KpiCalculate',
    component: () => import('@/views/admin/kpi/CalculateView.vue'),
  },
  {
    path: '/admin/kpi/reports',
    name: 'KpiReports',
    component: () => import('@/views/admin/kpi/ReportView.vue'),
  },
  {
    path: '/admin/kpi/periods',
    name: 'KpiPeriods',
    component: () => import('@/views/admin/kpi/PeriodView.vue'),
  },
  {
    path: '/admin/kpi/contractor',
    name: 'ContractorKpi',
    component: () => import('@/views/admin/kpi/ContractorKpiView.vue'),
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
