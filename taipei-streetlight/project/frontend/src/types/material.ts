// ===== Material Enums =====

export type WarehouseStatus = 'ACTIVE' | 'INACTIVE'
export type MaterialCategory = 'LUMINAIRE' | 'CONTROLLER' | 'POLE' | 'POLE_NUMBER' | 'CABLE' | 'OTHER'
export type MaterialStatus = 'ACTIVE' | 'DEPRECATED'
export type SupplierStatus = 'ACTIVE' | 'INACTIVE'
export type ApprovedMaterialStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED'
export type PurchaseOrderStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'RECEIVING' | 'COMPLETED' | 'CANCELLED'
export type IssueRequestStatus = 'PENDING' | 'APPROVED' | 'ISSUED' | 'REJECTED'
export type AdjustmentType = 'COUNT' | 'TRANSFER' | 'CORRECTION'
export type DisposalType = 'SCRAP' | 'RETURN_VENDOR' | 'DONATION' | 'OTHER'

// ===== Warehouse =====

export interface WarehouseRequest {
  warehouseCode: string
  warehouseName: string
  location?: string
  status?: WarehouseStatus
}

export interface WarehouseResponse {
  id: number
  warehouseCode: string
  warehouseName: string
  location: string | null
  status: WarehouseStatus
  createdAt: string
  updatedAt: string
}

// ===== Material Spec =====

export interface MaterialSpecRequest {
  specCode: string
  specName: string
  category: MaterialCategory
  unit?: string
  attributes?: Record<string, unknown>
  status?: MaterialStatus
}

export interface MaterialSpecResponse {
  id: number
  specCode: string
  specName: string
  category: MaterialCategory
  unit: string
  attributes: Record<string, unknown> | null
  status: MaterialStatus
  createdAt: string
  updatedAt: string
}

// ===== Supplier =====

export interface SupplierRequest {
  supplierCode: string
  supplierName: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  status?: SupplierStatus
}

export interface SupplierResponse {
  id: number
  supplierCode: string
  supplierName: string
  contactName: string | null
  contactPhone: string | null
  contactEmail: string | null
  address: string | null
  status: SupplierStatus
  createdAt: string
  updatedAt: string
}

// ===== Inventory =====

export interface InventoryResponse {
  id: number
  warehouseId: number
  warehouseName: string
  materialSpecId: number
  specCode: string
  specName: string
  category: MaterialCategory
  quantityOnHand: number
  safetyStock: number
  belowSafetyStock: boolean
}

export interface InventorySummaryResponse {
  category: MaterialCategory
  itemCount: number
  totalQuantity: number
}

// ===== Approved Material =====

export interface ApprovedMaterialRequest {
  materialSpecId: number
  contractId?: number
  materialNumber: string
  approvalDate: string
  batchNumber?: string
  brand?: string
  model?: string
  specDetails?: Record<string, unknown>
  status?: ApprovedMaterialStatus
}

export interface ApprovedMaterialResponse {
  id: number
  materialSpecId: number
  specCode: string
  specName: string
  contractId: number | null
  materialNumber: string
  approvalDate: string
  batchNumber: string | null
  brand: string | null
  model: string | null
  specDetails: Record<string, unknown> | null
  status: ApprovedMaterialStatus
  createdAt: string
}

export interface ImportResult {
  successCount: number
  skippedCount: number
  errors: string[]
}

// ===== Purchase Order =====

export interface PurchaseItemRequest {
  materialSpecId: number
  quantity: number
  unitPrice?: number
  notes?: string
}

export interface PurchaseItemResponse {
  id: number
  materialSpecId: number
  specCode: string
  specName: string
  quantity: number
  unitPrice: number | null
  notes: string | null
}

export interface PurchaseOrderRequest {
  supplierId: number
  contractId?: number
  notes?: string
  items: PurchaseItemRequest[]
}

export interface PurchaseOrderResponse {
  id: number
  poNumber: string
  supplierId: number
  supplierName: string
  contractId: number | null
  orderDate: string
  status: PurchaseOrderStatus
  totalAmount: number | null
  notes: string | null
  createdBy: string | null
  createdAt: string
  items: PurchaseItemResponse[]
}

// ===== Receiving =====

export interface ReceivingRequest {
  poId?: number
  warehouseId: number
  materialSpecId: number
  quantity: number
  deliveryNote?: string
}

export interface ReceivingResponse {
  id: number
  poId: number | null
  poNumber: string | null
  warehouseId: number
  warehouseName: string
  materialSpecId: number
  specName: string
  quantity: number
  receivedDate: string
  deliveryNote: string | null
  receivedBy: string | null
  createdAt: string
}

// ===== Issue Request =====

export interface IssueRequestRequest {
  repairTicketId?: number
  replacementOrderId?: number
}

export interface IssueRequestResponse {
  id: number
  requestNumber: string
  repairTicketId: number | null
  replacementOrderId: number | null
  requestedBy: string | null
  status: IssueRequestStatus
  createdAt: string
  updatedAt: string
}

// ===== Issue Record =====

export interface IssueRecordRequest {
  inventoryId: number
  materialSpecId: number
  quantity: number
}

export interface IssueRecordResponse {
  id: number
  requestId: number
  inventoryId: number
  materialSpecId: number
  specName: string
  quantity: number
  issuedBy: string | null
  issuedAt: string
}

// ===== Inventory Adjustment =====

export interface InventoryAdjustmentRequest {
  inventoryId: number
  actualQuantity?: number
  toWarehouseId?: number
  quantity?: number
  reason?: string
}

export interface InventoryAdjustmentResponse {
  id: number
  inventoryId: number
  adjustmentType: AdjustmentType
  quantityChange: number
  reason: string | null
  adjustedBy: string | null
  adjustedAt: string
}

// ===== Disposal =====

export interface DisposalRequest {
  materialSpecId: number
  quantity: number
  disposalType: DisposalType
  reason?: string
}

export interface DisposalResponse {
  id: number
  materialSpecId: number
  specName: string
  quantity: number
  disposalType: DisposalType
  reason: string | null
  disposedBy: string | null
  disposedAt: string
}

// ===== Pagination =====

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
