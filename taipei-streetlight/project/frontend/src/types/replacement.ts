// ===== Replacement Order Enums =====

export type ReplacementOrderType =
  | 'NEW_INSTALL'
  | 'REPLACEMENT'
  | 'RELOCATION'
  | 'DECOMMISSION'
  | 'ADJUSTMENT'
  | 'SHADE_INSTALL'

export type ReplacementOrderStatus =
  | 'DRAFT'
  | 'DISPATCHED'
  | 'IN_PROGRESS'
  | 'SELF_CHECKED'
  | 'PENDING_REVIEW'
  | 'RETURNED'
  | 'CLOSED'

export type ReplacementItemStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'SKIPPED'

export type PoleNumberStatus = 'ACTIVE' | 'DECOMMISSIONED' | 'LOST'

// ===== Replacement Order =====

export interface ReplacementOrderResponse {
  id: number
  orderNumber: string
  repairTicketId: number | null
  contractId: number | null
  orderType: ReplacementOrderType
  dispatchReason: string | null
  location: string | null
  expectedQuantity: number | null
  workPeriodStart: string | null
  workPeriodEnd: string | null
  assignedContractor: string | null
  status: ReplacementOrderStatus
  deptId: number | null
  createdBy: string | null
  createdAt: string
  updatedAt: string
  items?: ReplacementItemResponse[]
  currentStep?: string | null
}

export interface ReplacementOrderRequest {
  orderType: ReplacementOrderType
  repairTicketId?: number | null
  contractId?: number | null
  dispatchReason?: string
  location?: string
  expectedQuantity?: number
  workPeriodStart?: string
  workPeriodEnd?: string
  assignedContractor?: string
  deptId?: number
}

export interface ReplacementOrderQueryParams {
  status?: ReplacementOrderStatus
  orderType?: ReplacementOrderType
  contractId?: number
  keyword?: string
}

// ===== Replacement Item =====

export interface ReplacementItemResponse {
  id: number
  orderId: number
  parentDeviceId: number
  oldDeviceId: number
  newDeviceId: number | null
  parentDeviceCode?: string
  oldDeviceCode?: string
  newDeviceCode?: string
  beforeDeviceType: string | null
  beforeSpec: Record<string, unknown> | null
  afterDeviceType: string | null
  afterSpec: Record<string, unknown> | null
  materialSpecId: number | null
  approvedMaterialId: number | null
  status: ReplacementItemStatus
  completedAt: string | null
  completedBy: string | null
  notes: string | null
  createdAt: string
}

export interface ReplacementItemRequest {
  parentDeviceId: number
  oldDeviceId: number
  afterDeviceType?: string
  afterSpec?: Record<string, unknown>
  materialSpecId?: number
  approvedMaterialId?: number
}

// ===== Self-Check =====

export interface SelfCheckItemRequest {
  itemId: number
  deviceCode: string
  newDeviceId?: number
  notes?: string
}

export interface SelfCheckRequest {
  items: SelfCheckItemRequest[]
}

// ===== Light Pole Number =====

export interface PoleNumberResponse {
  id: number
  poleNumber: string
  deviceId: number | null
  qrCodeUrl: string | null
  issuedAt: string | null
  status: PoleNumberStatus
  createdAt: string
}

export interface PoleNumberRequest {
  poleNumber: string
  deviceId?: number
}

// ===== Page Response (reuse) =====

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
