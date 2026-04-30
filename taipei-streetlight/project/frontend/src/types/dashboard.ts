// ── Widget Type ──
export type WidgetType =
  | 'maintenance-stats'
  | 'outage-alert'
  | 'fault-heatmap'
  | 'kpi-summary'
  | 'lamp-count'
  | 'lamp-status'
  | 'panel-box'
  | 'attachments'
  | 'electricity-cost'
  | 'meter'
  | 'gis-overview'

// ── Layout ──
export interface WidgetConfig {
  i: string           // unique widget id (e.g. "w1")
  x: number
  y: number
  w: number
  h: number
  type: WidgetType
  title?: string
}

export interface LayoutRequest {
  layoutJson: string
}

export interface LayoutResponse {
  id: number
  layoutJson: string
  isDefault: boolean
  updatedAt: string
}

export interface DefaultLayoutRequest {
  layoutJson: string
  roleType?: string | null
}

export interface DefaultLayoutResponse {
  id: number
  layoutJson: string
  roleType: string | null
  createdAt: string
}

// ── Maintenance ──
export interface MaintenanceStatsResponse {
  totalRepairs: number
  completedRepairs: number
  pendingRepairs: number
  completionRate: number
  avgRepairHours: number
  illuminationRate: number
  sourceDistribution: Record<string, number>
  faultCategoryDistribution: Record<string, number>
}

export interface MonthlyPoint {
  month: string
  repairCount: number
  completionRate: number
}

export interface MaintenanceTrendResponse {
  months: MonthlyPoint[]
}

// ── Outage ──
export interface OutageZone {
  zone: string
  affectedCount: number
  since: string
}

export interface OutageAlertResponse {
  currentOutageCount: number
  outageZones: OutageZone[]
}

export interface MonthlyOutage {
  month: string
  outageCount: number
  avgRecoveryHours: number
}

export interface OutageTrendResponse {
  months: MonthlyOutage[]
}

// ── Fault ──
export interface CategoryItem {
  category: string
  count: number
  percentage: number
}

export interface FaultCategoryResponse {
  categories: CategoryItem[]
}

// ── KPI ──
export interface KpiIndicatorSummary {
  code: string
  name: string
  value: number
  target: number
  achievement: number
  grade: string
}

export interface KpiSummaryResponse {
  indicators: KpiIndicatorSummary[]
}

export interface IndicatorValue {
  code: string
  value: number
}

export interface MonthlyKpi {
  month: string
  indicators: IndicatorValue[]
}

export interface KpiTrendResponse {
  months: MonthlyKpi[]
}

// ── Device / Lamp ──
export interface LampCountResponse {
  total: number
  byContractor: Record<string, number>
  byType: Record<string, number>
  byLightSource: Record<string, number>
  byFacilityType: Record<string, number>
}

export interface LampStatusResponse {
  online: number
  offline: number
  onlineRate: number
  updatedAt: string
}

// ── Attachment ──
export interface AttachmentStatsResponse {
  totalCount: number
  totalSizeMB: number
  byType: Record<string, number>
}

// ── Unavailable ──
export interface WidgetUnavailableResponse {
  widgetType: string
  available: boolean
  message: string
}

// ── Multi-page & Theme ──

export type DashboardTheme = 'light' | 'dark' | 'custom'

export interface DashboardPage {
  name: string
  widgets: WidgetConfig[]
}

export interface DashboardData {
  version: 2
  theme: DashboardTheme
  customColor?: string
  activePageIndex: number
  pages: DashboardPage[]
}
