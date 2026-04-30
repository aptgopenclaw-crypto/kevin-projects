export type CircuitStatus = 'ACTIVE' | 'INACTIVE'
export type CircuitUsageType = 'LIGHTING' | 'TRAFFIC_SIGNAL' | 'OTHER'

export interface CircuitResponse {
  id: number
  panelBoxDeviceId: number | null
  circuitNumber: string
  circuitName: string | null
  taipowerAccount: string | null
  usageType: string | null
  status: string
  createdAt: string
  updatedAt: string
}

export interface CircuitRequest {
  panelBoxDeviceId?: number
  circuitNumber: string
  circuitName?: string
  taipowerAccount?: string
  usageType?: string
  status?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
