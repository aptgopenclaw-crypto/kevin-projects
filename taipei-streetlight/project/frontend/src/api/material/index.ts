import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  WarehouseRequest,
  WarehouseResponse,
  MaterialSpecRequest,
  MaterialSpecResponse,
  SupplierRequest,
  SupplierResponse,
  InventoryResponse,
  InventorySummaryResponse,
  ApprovedMaterialRequest,
  ApprovedMaterialResponse,
  ImportResult,
  PurchaseOrderRequest,
  PurchaseOrderResponse,
  ReceivingRequest,
  ReceivingResponse,
  IssueRequestRequest,
  IssueRequestResponse,
  IssueRecordRequest,
  IssueRecordResponse,
  InventoryAdjustmentRequest,
  InventoryAdjustmentResponse,
  DisposalRequest,
  DisposalResponse,
  PageResponse,
  WarehouseStatus,
  MaterialCategory,
  PurchaseOrderStatus,
  IssueRequestStatus,
} from '@/types/material'

// ── Warehouses ──

export const getWarehouses = (params?: { status?: WarehouseStatus; keyword?: string; page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<WarehouseResponse>>>('/auth/material/warehouses', { params })

export const getActiveWarehouses = () =>
  axiosIns.get<unknown, BaseResponse<WarehouseResponse[]>>('/auth/material/warehouses/active')

export const createWarehouse = (payload: WarehouseRequest) =>
  axiosIns.post<unknown, BaseResponse<WarehouseResponse>>('/auth/material/warehouses', payload)

export const updateWarehouse = (id: number, payload: WarehouseRequest) =>
  axiosIns.put<unknown, BaseResponse<WarehouseResponse>>(`/auth/material/warehouses/${id}`, payload)

export const deleteWarehouse = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/material/warehouses/${id}`)

// ── Material Specs ──

export const getMaterialSpecs = (params?: { category?: MaterialCategory; keyword?: string; page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<MaterialSpecResponse>>>('/auth/material/specs', { params })

export const getMaterialSpecById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<MaterialSpecResponse>>(`/auth/material/specs/${id}`)

export const createMaterialSpec = (payload: MaterialSpecRequest) =>
  axiosIns.post<unknown, BaseResponse<MaterialSpecResponse>>('/auth/material/specs', payload)

export const updateMaterialSpec = (id: number, payload: MaterialSpecRequest) =>
  axiosIns.put<unknown, BaseResponse<MaterialSpecResponse>>(`/auth/material/specs/${id}`, payload)

// ── Suppliers ──

export const getSuppliers = (params?: { keyword?: string; page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<SupplierResponse>>>('/auth/material/suppliers', { params })

export const getActiveSuppliers = () =>
  axiosIns.get<unknown, BaseResponse<SupplierResponse[]>>('/auth/material/suppliers/active')

export const createSupplier = (payload: SupplierRequest) =>
  axiosIns.post<unknown, BaseResponse<SupplierResponse>>('/auth/material/suppliers', payload)

export const updateSupplier = (id: number, payload: SupplierRequest) =>
  axiosIns.put<unknown, BaseResponse<SupplierResponse>>(`/auth/material/suppliers/${id}`, payload)

// ── Inventory ──

export const getInventory = (params?: {
  warehouseId?: number
  category?: MaterialCategory
  keyword?: string
  belowSafetyStock?: boolean
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<InventoryResponse>>>('/auth/material/inventory', { params })

export const getInventorySummary = () =>
  axiosIns.get<unknown, BaseResponse<InventorySummaryResponse[]>>('/auth/material/inventory/summary')

export const getInventoryAlerts = () =>
  axiosIns.get<unknown, BaseResponse<InventoryResponse[]>>('/auth/material/inventory/alerts')

// ── Approved Materials ──

export const getApprovedMaterials = (params?: { keyword?: string; page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<ApprovedMaterialResponse>>>('/auth/material/approved-materials', { params })

export const getApprovedMaterialById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<ApprovedMaterialResponse>>(`/auth/material/approved-materials/${id}`)

export const createApprovedMaterial = (payload: ApprovedMaterialRequest) =>
  axiosIns.post<unknown, BaseResponse<ApprovedMaterialResponse>>('/auth/material/approved-materials', payload)

export const updateApprovedMaterial = (id: number, payload: ApprovedMaterialRequest) =>
  axiosIns.put<unknown, BaseResponse<ApprovedMaterialResponse>>(`/auth/material/approved-materials/${id}`, payload)

export const importApprovedMaterials = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return axiosIns.post<unknown, BaseResponse<ImportResult>>(
    '/auth/material/approved-materials/import',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
}

// ── Purchase Orders ──

export const getPurchaseOrders = (params?: { status?: PurchaseOrderStatus; keyword?: string; page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<PurchaseOrderResponse>>>('/auth/material/purchase-orders', { params })

export const getPurchaseOrderById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<PurchaseOrderResponse>>(`/auth/material/purchase-orders/${id}`)

export const createPurchaseOrder = (payload: PurchaseOrderRequest) =>
  axiosIns.post<unknown, BaseResponse<PurchaseOrderResponse>>('/auth/material/purchase-orders', payload)

export const updatePurchaseOrder = (id: number, payload: PurchaseOrderRequest) =>
  axiosIns.put<unknown, BaseResponse<PurchaseOrderResponse>>(`/auth/material/purchase-orders/${id}`, payload)

export const submitPurchaseOrder = (id: number) =>
  axiosIns.post<unknown, BaseResponse<PurchaseOrderResponse>>(`/auth/material/purchase-orders/${id}/submit`)

// ── Receiving ──

export const getReceivingRecords = (params?: { page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<ReceivingResponse>>>('/auth/material/receiving', { params })

export const createReceiving = (payload: ReceivingRequest) =>
  axiosIns.post<unknown, BaseResponse<ReceivingResponse>>('/auth/material/receiving', payload)

// ── Issue Requests ──

export const getIssueRequests = (params?: { status?: IssueRequestStatus; page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<IssueRequestResponse>>>('/auth/material/issue-requests', { params })

export const createIssueRequest = (payload: IssueRequestRequest) =>
  axiosIns.post<unknown, BaseResponse<IssueRequestResponse>>('/auth/material/issue-requests', payload)

export const approveIssueRequest = (id: number) =>
  axiosIns.post<unknown, BaseResponse<IssueRequestResponse>>(`/auth/material/issue-requests/${id}/approve`)

export const rejectIssueRequest = (id: number) =>
  axiosIns.post<unknown, BaseResponse<IssueRequestResponse>>(`/auth/material/issue-requests/${id}/reject`)

export const issueFromRequest = (id: number, records: IssueRecordRequest[]) =>
  axiosIns.post<unknown, BaseResponse<IssueRecordResponse[]>>(`/auth/material/issue-requests/${id}/issue`, records)

// ── Inventory Adjustments ──

export const getAdjustments = (params?: { page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<InventoryAdjustmentResponse>>>('/auth/material/adjustments', { params })

export const countInventory = (payload: InventoryAdjustmentRequest) =>
  axiosIns.post<unknown, BaseResponse<InventoryAdjustmentResponse>>('/auth/material/adjustments/count', payload)

export const transferInventory = (payload: InventoryAdjustmentRequest) =>
  axiosIns.post<unknown, BaseResponse<InventoryAdjustmentResponse>>('/auth/material/adjustments/transfer', payload)

export const correctInventory = (payload: InventoryAdjustmentRequest) =>
  axiosIns.post<unknown, BaseResponse<InventoryAdjustmentResponse>>('/auth/material/adjustments/correction', payload)

// ── Disposals ──

export const getDisposals = (params?: { page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<DisposalResponse>>>('/auth/material/disposals', { params })

export const createDisposal = (payload: DisposalRequest) =>
  axiosIns.post<unknown, BaseResponse<DisposalResponse>>('/auth/material/disposals', payload)
