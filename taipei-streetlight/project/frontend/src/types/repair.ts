// ===== Repair Ticket Enums =====

export type RepairTicketStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'DISPATCHED'
  | 'IN_PROGRESS'
  | 'COMPLETION_REPORTED'
  | 'PENDING_REVIEW'
  | 'RETURNED'
  | 'TRANSFERRED'
  | 'TRACKING'
  | 'CLOSED'

export type RepairTicketSource =
  | 'FAULT_TICKET'
  | 'CITIZEN_WEB'
  | 'EXTERNAL_1999'
  | 'PATROL'
  | 'PHONE'

export type RepairTicketPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

export type RepairDispatchStatus = 'DISPATCHED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export type TicketType = 'FAULT_TICKET' | 'REPAIR_TICKET' | 'REPLACEMENT_ORDER'

export type AttachmentPhase = 'BEFORE' | 'DURING' | 'AFTER' | 'REPORT'

export type ScanStatus = 'PENDING' | 'CLEAN' | 'INFECTED'

// ===== Inspection Enums =====

export type InspectionTaskType = 'ONE_TIME' | 'RECURRING'

export type InspectionTaskStatus = 'ACTIVE' | 'INACTIVE'

export type InspectionResult = 'NORMAL' | 'ABNORMAL' | 'NEED_REPAIR'

// ===== Repair Ticket =====

export interface RepairTicketResponse {
  id: number
  ticketNumber: string
  faultTicketId: number | null
  deviceId: number | null
  circuitId: number | null
  contractId: number | null

  source: RepairTicketSource
  reporterName: string | null
  reporterPhone: string | null
  reporterEmail: string | null
  reportAddress: string | null
  reportDescription: string | null
  reportedAt: string

  faultCategory: string | null
  faultCause: string | null
  repairDescription: string | null
  completedAt: string | null

  status: RepairTicketStatus
  priority: RepairTicketPriority
  deptId: number | null

  createdBy: string | null
  createdAt: string
  updatedAt: string

  // 關聯資訊（詳情頁載入）
  currentStep: string | null
  dispatches: DispatchResponse[] | null
  attachments: AttachmentResponse[] | null
}

export interface RepairTicketRequest {
  source: RepairTicketSource
  reporterName?: string
  reporterPhone?: string
  reporterEmail?: string
  reportAddress?: string
  reportDescription?: string
  deviceId?: number
  circuitId?: number
  contractId?: number
  faultTicketId?: number
  priority?: RepairTicketPriority
  deptId?: number
}

export interface RepairTicketQueryParams {
  status?: RepairTicketStatus
  source?: RepairTicketSource
  priority?: RepairTicketPriority
  deptId?: number
  keyword?: string
}

// ===== Dispatch =====

export interface DispatchRequest {
  assignedTo?: number
  assignedOrg?: string
  contractId: number
  dueDate?: string
  note?: string
}

export interface DispatchResponse {
  id: number
  repairTicketId: number
  contractId: number | null
  assignedTo: number | null
  assignedOrg: string | null
  dispatchNote: string | null
  dispatchedAt: string
  dispatchedBy: number
  dueDate: string | null
  status: RepairDispatchStatus
  createdAt: string
}

// ===== Completion Report =====

export interface CompletionReportRequest {
  repairDescription: string
  faultCause?: string
  attachments?: AttachmentUploadRequest[]
}

// ===== Attachment =====

export interface AttachmentUploadRequest {
  fileType: string
  fileUrl?: string
  fileName?: string
  fileSize?: number
  description?: string
  gpsLat?: number
  gpsLng?: number
  takenAt?: string
  phase?: AttachmentPhase
}

export interface AttachmentResponse {
  id: number
  ticketType: TicketType
  ticketId: number
  fileType: string
  fileUrl: string
  fileName: string | null
  fileSize: number | null
  description: string | null
  gpsLat: number | null
  gpsLng: number | null
  takenAt: string | null
  phase: AttachmentPhase | null
  scanStatus: ScanStatus
  uploadedBy: string | null
  uploadedAt: string
}

// ===== Inspection Task =====

export interface InspectionTaskRequest {
  taskName: string
  taskType: InspectionTaskType
  scheduleCron?: string
  startDate?: string
  endDate?: string
  areaScope?: Record<string, unknown>
  deptId?: number
  assignedTo?: number
}

export interface InspectionTaskResponse {
  id: number
  taskName: string
  taskType: InspectionTaskType
  scheduleCron: string | null
  startDate: string | null
  endDate: string | null
  areaScope: Record<string, unknown> | null
  deptId: number | null
  assignedTo: number | null
  status: InspectionTaskStatus
  createdBy: string | null
  createdAt: string
  updatedAt: string
}

// ===== Inspection Record =====

export interface InspectionRecordRequest {
  taskId: number
  deviceId?: number
  result: InspectionResult
  notes?: string
  attachments?: Record<string, unknown>[]
}

export interface InspectionRecordResponse {
  id: number
  taskId: number
  inspectorId: number | null
  inspectionDate: string | null
  deviceId: number | null
  result: InspectionResult
  notes: string | null
  attachments: Record<string, unknown>[] | null
  faultTicketId: number | null
  createdAt: string
}

// ===== Pagination =====

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
