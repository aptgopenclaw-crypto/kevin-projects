import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  LayoutRequest,
  LayoutResponse,
  DefaultLayoutRequest,
  DefaultLayoutResponse,
  MaintenanceStatsResponse,
  MaintenanceTrendResponse,
  OutageAlertResponse,
  OutageTrendResponse,
  FaultCategoryResponse,
  KpiSummaryResponse,
  KpiTrendResponse,
  LampCountResponse,
  LampStatusResponse,
  AttachmentStatsResponse,
  WidgetUnavailableResponse,
} from '@/types/dashboard'

// ── Layout ──

export const getLayout = () =>
  axiosIns.get<unknown, BaseResponse<LayoutResponse>>('/auth/dashboard/layout')

export const saveLayout = (payload: LayoutRequest) =>
  axiosIns.put<unknown, BaseResponse<LayoutResponse>>('/auth/dashboard/layout', payload)

export const resetLayout = () =>
  axiosIns.post<unknown, BaseResponse<LayoutResponse>>('/auth/dashboard/layout/reset')

// ── Default Layout (DASHBOARD_MANAGE) ──

export const getDefaultLayout = () =>
  axiosIns.get<unknown, BaseResponse<DefaultLayoutResponse>>('/auth/dashboard/layout/default')

export const saveDefaultLayout = (payload: DefaultLayoutRequest) =>
  axiosIns.put<unknown, BaseResponse<DefaultLayoutResponse>>('/auth/dashboard/layout/default', payload)

// ── Widget Data ──

export const getMaintenanceStats = (params?: { startDate?: string; endDate?: string; contractId?: number }) =>
  axiosIns.get<unknown, BaseResponse<MaintenanceStatsResponse>>('/auth/dashboard/widgets/maintenance', { params })

export const getMaintenanceTrend = (params: { startDate: string; endDate: string; contractId?: number }) =>
  axiosIns.get<unknown, BaseResponse<MaintenanceTrendResponse>>('/auth/dashboard/widgets/maintenance/trend', { params })

export const getOutageAlert = () =>
  axiosIns.get<unknown, BaseResponse<OutageAlertResponse>>('/auth/dashboard/widgets/outage')

export const getOutageTrend = (params: { startDate: string; endDate: string }) =>
  axiosIns.get<unknown, BaseResponse<OutageTrendResponse>>('/auth/dashboard/widgets/outage/trend', { params })

export const getFaultHeatmap = (params?: { startDate?: string; endDate?: string }) =>
  axiosIns.get<unknown, BaseResponse<Record<string, unknown>>>('/auth/dashboard/widgets/fault-heatmap', { params })

export const getFaultCategory = (params?: { startDate?: string; endDate?: string }) =>
  axiosIns.get<unknown, BaseResponse<FaultCategoryResponse>>('/auth/dashboard/widgets/fault-category', { params })

export const getKpiSummary = (params: { year: number; month: number }) =>
  axiosIns.get<unknown, BaseResponse<KpiSummaryResponse>>('/auth/dashboard/widgets/kpi', { params })

export const getKpiTrend = (params: { year: number; month: number; months?: number }) =>
  axiosIns.get<unknown, BaseResponse<KpiTrendResponse>>('/auth/dashboard/widgets/kpi/trend', { params })

export const getLampCount = (params?: { district?: string; contractId?: number }) =>
  axiosIns.get<unknown, BaseResponse<LampCountResponse>>('/auth/dashboard/widgets/lamp-count', { params })

export const getLampStatus = () =>
  axiosIns.get<unknown, BaseResponse<LampStatusResponse>>('/auth/dashboard/widgets/lamp-status')

export const getAttachmentStats = () =>
  axiosIns.get<unknown, BaseResponse<AttachmentStatsResponse>>('/auth/dashboard/widgets/attachments')

// ── Stub Widgets ──

export const getPanelBox = () =>
  axiosIns.get<unknown, BaseResponse<WidgetUnavailableResponse>>('/auth/dashboard/widgets/panel-box')

export const getPanelBoxAlerts = () =>
  axiosIns.get<unknown, BaseResponse<WidgetUnavailableResponse>>('/auth/dashboard/widgets/panel-box/alerts')

export const getElectricityCost = () =>
  axiosIns.get<unknown, BaseResponse<WidgetUnavailableResponse>>('/auth/dashboard/widgets/electricity-cost')

export const getElectricityCostTrend = () =>
  axiosIns.get<unknown, BaseResponse<WidgetUnavailableResponse>>('/auth/dashboard/widgets/electricity-cost/trend')

export const getMeter = () =>
  axiosIns.get<unknown, BaseResponse<WidgetUnavailableResponse>>('/auth/dashboard/widgets/meter')

export const getMeterTrend = () =>
  axiosIns.get<unknown, BaseResponse<WidgetUnavailableResponse>>('/auth/dashboard/widgets/meter/trend')

export const getGisOverview = () =>
  axiosIns.get<unknown, BaseResponse<WidgetUnavailableResponse>>('/auth/dashboard/widgets/gis')
