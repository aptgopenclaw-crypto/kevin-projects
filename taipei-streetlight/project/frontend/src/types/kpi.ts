// ===== KPI Enums =====

export type KpiCategory = 'MAINTENANCE' | 'POWER' | 'RESPONSE' | 'QUALITY' | 'CUSTOM'
export type FormulaType = 'SPEL' | 'JS'
export type KpiDataSource = 'SYSTEM_AUTO' | 'MANUAL_IMPORT' | 'IOT_DEVICE'
export type KpiRawDataSource = 'AUTO' | 'MANUAL_IMPORT'
export type KpiIndicatorStatus = 'ACTIVE' | 'INACTIVE' | 'DRAFT'

// ===== Indicator =====

export interface KpiIndicatorRequest {
  indicatorCode: string
  indicatorName: string
  category: KpiCategory
  description?: string
  formulaType: FormulaType
  formula: string
  targetValue?: number
  weight?: number
  dataSource: KpiDataSource
  unit?: string
  bonusCondition?: string
  penaltyCondition?: string
}

export interface KpiIndicatorResponse {
  id: number
  indicatorCode: string
  indicatorName: string
  category: KpiCategory
  description: string | null
  formulaType: FormulaType
  formula: string
  targetValue: number | null
  weight: number | null
  dataSource: KpiDataSource
  unit: string | null
  bonusCondition: string | null
  penaltyCondition: string | null
  status: KpiIndicatorStatus
  createdAt: string
  updatedAt: string
}

// ===== Formula Test =====

export interface FormulaTestRequest {
  formulaType: FormulaType
  formula: string
  variables: Record<string, number>
}

export interface FormulaTestResponse {
  success: boolean
  result: number | null
  error: string | null
}

// ===== Raw Data =====

export interface KpiRawDataResponse {
  id: number
  indicatorId: number
  indicatorCode: string
  indicatorName: string
  periodYear: number
  periodMonth: number
  contractId: number | null
  rawValue: number
  source: KpiRawDataSource
  collectedAt: string
}

// ===== Result =====

export interface KpiResultResponse {
  id: number
  indicatorId: number
  indicatorCode: string
  indicatorName: string
  category: string
  periodYear: number
  periodMonth: number
  contractId: number | null
  resultValue: number
  targetValue: number | null
  achievement: number | null
  weight: number | null
  calculatedAt: string
}

// ===== Period =====

export interface PeriodResponse {
  id: number
  periodYear: number
  periodMonth: number
  locked: boolean
  lockedAt: string | null
  lockedBy: string | null
  unlockReason: string | null
}

// ===== Report =====

export interface IndicatorScore {
  indicatorCode: string
  indicatorName: string
  rawValue: number | null
  resultValue: number
  targetValue: number | null
  achievement: number | null
  weight: number
  weightedScore: number
}

export interface MonthlyReportResponse {
  periodYear: number
  periodMonth: number
  contractId: number | null
  totalWeightedScore: number
  indicators: IndicatorScore[]
}

export interface MonthSummary {
  month: number
  totalScore: number
}

export interface IndicatorTrend {
  indicatorCode: string
  indicatorName: string
  monthlyValues: number[]
}

export interface YearlyReportResponse {
  periodYear: number
  contractId: number | null
  months: MonthSummary[]
  indicators: IndicatorTrend[]
}

export interface ContractScore {
  contractId: number
  contractName: string
  totalScore: number
  indicators: IndicatorScore[]
}

export interface CompareReportResponse {
  periodYear: number
  periodMonth: number
  contracts: ContractScore[]
}

// ===== Pagination =====

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
