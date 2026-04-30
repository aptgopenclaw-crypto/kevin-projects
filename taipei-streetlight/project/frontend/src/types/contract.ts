export type ContractStatus = 'ACTIVE' | 'EXPIRED' | 'TERMINATED'

export interface ContractResponse {
  id: number
  contractCode: string
  contractName: string
  budgetYear: number | null
  procurementNumber: string | null
  contractorName: string | null
  contractorContact: string | null
  assetCategory: string | null
  quantity: number | null
  startDate: string | null
  endDate: string | null
  acceptanceDate: string | null
  warrantyYears: number | null
  warrantyExpiry: string | null
  status: ContractStatus
  attributes: Record<string, unknown> | null
  createdBy: string | null
  createdAt: string
  updatedAt: string
}

export interface ContractRequest {
  contractCode: string
  contractName: string
  budgetYear?: number
  procurementNumber?: string
  contractorName?: string
  contractorContact?: string
  assetCategory?: string
  quantity?: number
  startDate?: string
  endDate?: string
  acceptanceDate?: string
  warrantyYears?: number
  warrantyExpiry?: string
  status?: ContractStatus
  attributes?: Record<string, unknown>
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
