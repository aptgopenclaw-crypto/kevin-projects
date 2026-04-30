export type FaultTicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'MERGED'
export type FaultTicketSource = 'CITIZEN_REPORT' | 'PATROL' | 'AUTO_ALERT'
export type RootCauseType = 'CIRCUIT' | 'GATEWAY' | 'AREA'

export interface FaultTicketResponse {
  id: number
  deviceId: number | null
  circuitId: number | null
  correlationId: number | null
  source: FaultTicketSource
  status: FaultTicketStatus
  priority: string
  description: string | null
  reportedBy: string | null
  reportedAt: string
  resolvedAt: string | null
  resolvedBy: string | null
  resolutionNote: string | null
  createdAt: string
  updatedAt: string
}

export interface FaultTicketRequest {
  deviceId?: number
  circuitId?: number
  source: FaultTicketSource
  priority?: string
  description?: string
}

export interface FaultCorrelationResponse {
  id: number
  rootCauseType: RootCauseType
  rootCauseId: number
  affectedCount: number
  status: string
  detectedAt: string
  confirmedAt: string | null
  resolvedAt: string | null
  resolutionNote: string | null
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
