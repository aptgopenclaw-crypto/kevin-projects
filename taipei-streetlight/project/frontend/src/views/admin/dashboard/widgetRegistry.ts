import { defineAsyncComponent, type Component } from 'vue'
import type { WidgetType, WidgetConfig } from '@/types/dashboard'

export interface WidgetMeta {
  type: WidgetType
  labelKey: string          // i18n key
  defaultW: number          // grid columns
  defaultH: number          // grid rows
  minW: number
  minH: number
  component: Component
  realtime: boolean         // supports WebSocket refresh
  available: boolean        // false = stub widget
}

const registry: Record<WidgetType, WidgetMeta> = {
  'maintenance-stats': {
    type: 'maintenance-stats',
    labelKey: 'dashboard.widgets.maintenanceStats',
    defaultW: 6, defaultH: 4, minW: 4, minH: 3,
    component: defineAsyncComponent(() => import('@/views/admin/dashboard/widgets/MaintenanceStatsWidget.vue')),
    realtime: false, available: true,
  },
  'outage-alert': {
    type: 'outage-alert',
    labelKey: 'dashboard.widgets.outageAlert',
    defaultW: 6, defaultH: 4, minW: 4, minH: 3,
    component: defineAsyncComponent(() => import('@/views/admin/dashboard/widgets/OutageAlertWidget.vue')),
    realtime: true, available: true,
  },
  'fault-heatmap': {
    type: 'fault-heatmap',
    labelKey: 'dashboard.widgets.faultHeatmap',
    defaultW: 6, defaultH: 5, minW: 4, minH: 4,
    component: defineAsyncComponent(() => import('@/views/admin/dashboard/widgets/FaultHeatmapWidget.vue')),
    realtime: false, available: true,
  },
  'kpi-summary': {
    type: 'kpi-summary',
    labelKey: 'dashboard.widgets.kpiSummary',
    defaultW: 6, defaultH: 4, minW: 4, minH: 3,
    component: defineAsyncComponent(() => import('@/views/admin/dashboard/widgets/KpiSummaryWidget.vue')),
    realtime: false, available: true,
  },
  'lamp-count': {
    type: 'lamp-count',
    labelKey: 'dashboard.widgets.lampCount',
    defaultW: 6, defaultH: 4, minW: 3, minH: 3,
    component: defineAsyncComponent(() => import('@/views/admin/dashboard/widgets/LampCountWidget.vue')),
    realtime: false, available: true,
  },
  'lamp-status': {
    type: 'lamp-status',
    labelKey: 'dashboard.widgets.lampStatus',
    defaultW: 3, defaultH: 3, minW: 2, minH: 2,
    component: defineAsyncComponent(() => import('@/views/admin/dashboard/widgets/LampStatusWidget.vue')),
    realtime: true, available: true,
  },
  'panel-box': {
    type: 'panel-box',
    labelKey: 'dashboard.widgets.panelBox',
    defaultW: 6, defaultH: 4, minW: 4, minH: 3,
    component: defineAsyncComponent(() => import('@/views/admin/dashboard/widgets/PanelBoxWidget.vue')),
    realtime: false, available: false,
  },
  'attachments': {
    type: 'attachments',
    labelKey: 'dashboard.widgets.attachments',
    defaultW: 4, defaultH: 3, minW: 3, minH: 2,
    component: defineAsyncComponent(() => import('@/views/admin/dashboard/widgets/AttachmentStatsWidget.vue')),
    realtime: false, available: true,
  },
  'electricity-cost': {
    type: 'electricity-cost',
    labelKey: 'dashboard.widgets.electricityCost',
    defaultW: 6, defaultH: 4, minW: 4, minH: 3,
    component: defineAsyncComponent(() => import('@/views/admin/dashboard/widgets/ElectricityCostWidget.vue')),
    realtime: false, available: false,
  },
  'meter': {
    type: 'meter',
    labelKey: 'dashboard.widgets.meter',
    defaultW: 6, defaultH: 4, minW: 4, minH: 3,
    component: defineAsyncComponent(() => import('@/views/admin/dashboard/widgets/MeterWidget.vue')),
    realtime: false, available: false,
  },
  'gis-overview': {
    type: 'gis-overview',
    labelKey: 'dashboard.widgets.gisOverview',
    defaultW: 12, defaultH: 6, minW: 6, minH: 4,
    component: defineAsyncComponent(() => import('@/views/admin/dashboard/widgets/GisOverviewWidget.vue')),
    realtime: true, available: false,
  },
}

export function getWidgetMeta(type: WidgetType): WidgetMeta {
  return registry[type]
}

export function getAllWidgetMetas(): WidgetMeta[] {
  return Object.values(registry)
}

/** Build default layout with only available widgets */
export function buildDefaultLayout(): WidgetConfig[] {
  const available = getAllWidgetMetas().filter(m => m.available)
  const layout: WidgetConfig[] = []
  let y = 0
  let x = 0
  let rowMaxH = 0

  available.forEach((meta, idx) => {
    if (x + meta.defaultW > 12) {
      x = 0
      y += rowMaxH
      rowMaxH = 0
    }
    layout.push({
      i: `w${idx}`,
      x,
      y,
      w: meta.defaultW,
      h: meta.defaultH,
      type: meta.type,
    })
    x += meta.defaultW
    rowMaxH = Math.max(rowMaxH, meta.defaultH)
  })

  return layout
}

export default registry
