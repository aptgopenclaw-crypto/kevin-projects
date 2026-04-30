import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  KpiIndicatorRequest,
  KpiIndicatorResponse,
  FormulaTestRequest,
  FormulaTestResponse,
  KpiRawDataResponse,
  KpiResultResponse,
  PeriodResponse,
  MonthlyReportResponse,
  YearlyReportResponse,
  CompareReportResponse,
  PageResponse,
  KpiCategory,
  KpiIndicatorStatus,
} from '@/types/kpi'

// ── Indicators ──

export const getIndicators = (params?: {
  category?: KpiCategory
  status?: KpiIndicatorStatus
  keyword?: string
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<KpiIndicatorResponse>>>('/auth/kpi/indicators', { params })

export const getIndicatorById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<KpiIndicatorResponse>>(`/auth/kpi/indicators/${id}`)

export const createIndicator = (payload: KpiIndicatorRequest) =>
  axiosIns.post<unknown, BaseResponse<KpiIndicatorResponse>>('/auth/kpi/indicators', payload)

export const updateIndicator = (id: number, payload: KpiIndicatorRequest) =>
  axiosIns.put<unknown, BaseResponse<KpiIndicatorResponse>>(`/auth/kpi/indicators/${id}`, payload)

export const deleteIndicator = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/kpi/indicators/${id}`)

export const testFormula = (payload: FormulaTestRequest) =>
  axiosIns.post<unknown, BaseResponse<FormulaTestResponse>>('/auth/kpi/indicators/test-formula', payload)

// ── Raw Data ──

export const getRawData = (params?: {
  periodYear?: number
  periodMonth?: number
  contractId?: number
  indicatorId?: number
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<KpiRawDataResponse>>>('/auth/kpi/data', { params })

export const importRawData = (indicatorId: number, periodYear: number, periodMonth: number, file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('indicatorId', String(indicatorId))
  formData.append('periodYear', String(periodYear))
  formData.append('periodMonth', String(periodMonth))
  return axiosIns.post<unknown, BaseResponse<number>>('/auth/kpi/data/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// ── Calculation ──

export const calculateKpi = (year: number, month: number, indicatorId?: number) =>
  axiosIns.post<unknown, BaseResponse<number>>('/auth/kpi/calculate', null, {
    params: { year, month, indicatorId },
  })

export const getResults = (params?: {
  periodYear?: number
  periodMonth?: number
  contractId?: number
  indicatorId?: number
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<KpiResultResponse>>>('/auth/kpi/results', { params })

// ── Periods ──

export const getPeriods = () =>
  axiosIns.get<unknown, BaseResponse<PeriodResponse[]>>('/auth/kpi/periods')

export const lockPeriod = (year: number, month: number) =>
  axiosIns.put<unknown, BaseResponse<PeriodResponse>>(`/auth/kpi/periods/${year}-${month}/lock`)

export const unlockPeriod = (year: number, month: number, reason: string) =>
  axiosIns.put<unknown, BaseResponse<PeriodResponse>>(`/auth/kpi/periods/${year}-${month}/unlock`, null, {
    params: { reason },
  })

// ── Reports ──

export const getMonthlyReport = (year: number, month: number, contractId?: number) =>
  axiosIns.get<unknown, BaseResponse<MonthlyReportResponse>>('/auth/kpi/reports/monthly', {
    params: { year, month, contractId },
  })

export const getYearlyReport = (year: number, contractId?: number) =>
  axiosIns.get<unknown, BaseResponse<YearlyReportResponse>>('/auth/kpi/reports/yearly', {
    params: { year, contractId },
  })

export const getCompareReport = (year: number, month: number, contractIds: number[]) =>
  axiosIns.get<unknown, BaseResponse<CompareReportResponse>>('/auth/kpi/reports/compare', {
    params: { year, month, contractIds },
  })

export const exportReportXls = (year: number, month: number, contractId?: number) =>
  axiosIns.get('/auth/kpi/reports/export/xls', {
    params: { year, month, contractId },
    responseType: 'blob',
  })

export const exportReportCsv = (year: number, month: number, contractId?: number) =>
  axiosIns.get('/auth/kpi/reports/export/csv', {
    params: { year, month, contractId },
    responseType: 'blob',
  })

// ── Contractor ──

export const getContractorResults = (params?: { year?: number; contractId?: number }) =>
  axiosIns.get<unknown, BaseResponse<KpiResultResponse[]>>('/auth/kpi/contractor/results', { params })
