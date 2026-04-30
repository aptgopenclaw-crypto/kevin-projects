import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  ReplacementOrderResponse,
  ReplacementOrderRequest,
  ReplacementOrderQueryParams,
  ReplacementItemResponse,
  ReplacementItemRequest,
  SelfCheckRequest,
  PoleNumberResponse,
  PoleNumberRequest,
  PageResponse,
} from '@/types/replacement'

// ── Replacement Orders ──

export const getReplacementOrders = (params: ReplacementOrderQueryParams & { page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<ReplacementOrderResponse>>>('/auth/replacement/orders', { params })

export const getReplacementOrderById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<ReplacementOrderResponse>>(`/auth/replacement/orders/${id}`)

export const createReplacementOrder = (payload: ReplacementOrderRequest) =>
  axiosIns.post<unknown, BaseResponse<ReplacementOrderResponse>>('/auth/replacement/orders', payload)

export const createReplacementOrderFromRepair = (repairTicketId: number, payload: ReplacementOrderRequest) =>
  axiosIns.post<unknown, BaseResponse<ReplacementOrderResponse>>(`/auth/replacement/orders/from-repair/${repairTicketId}`, payload)

export const updateReplacementOrder = (id: number, payload: ReplacementOrderRequest) =>
  axiosIns.put<unknown, BaseResponse<ReplacementOrderResponse>>(`/auth/replacement/orders/${id}`, payload)

export const dispatchReplacementOrder = (id: number, payload: ReplacementOrderRequest) =>
  axiosIns.post<unknown, BaseResponse<void>>(`/auth/replacement/orders/${id}/dispatch`, payload)

export const startWorkReplacementOrder = (id: number) =>
  axiosIns.post<unknown, BaseResponse<void>>(`/auth/replacement/orders/${id}/start-work`)

export const selfCheckReplacementOrder = (id: number, payload: SelfCheckRequest) =>
  axiosIns.post<unknown, BaseResponse<void>>(`/auth/replacement/orders/${id}/self-check`, payload)

export const submitReviewReplacementOrder = (id: number) =>
  axiosIns.post<unknown, BaseResponse<void>>(`/auth/replacement/orders/${id}/submit-review`)

export const approveReplacementOrder = (id: number, comment?: string) =>
  axiosIns.post<unknown, BaseResponse<void>>(`/auth/replacement/orders/${id}/approve`, { comment })

export const returnReplacementOrder = (id: number, comment: string) =>
  axiosIns.post<unknown, BaseResponse<void>>(`/auth/replacement/orders/${id}/return`, { comment })

export const resubmitReplacementOrder = (id: number) =>
  axiosIns.post<unknown, BaseResponse<void>>(`/auth/replacement/orders/${id}/resubmit`)

// ── Replacement Items ──

export const getReplacementItems = (orderId: number) =>
  axiosIns.get<unknown, BaseResponse<ReplacementItemResponse[]>>(`/auth/replacement/orders/${orderId}/items`)

export const addReplacementItem = (orderId: number, payload: ReplacementItemRequest) =>
  axiosIns.post<unknown, BaseResponse<ReplacementItemResponse>>(`/auth/replacement/orders/${orderId}/items`, payload)

export const updateReplacementItem = (orderId: number, itemId: number, payload: ReplacementItemRequest) =>
  axiosIns.put<unknown, BaseResponse<ReplacementItemResponse>>(`/auth/replacement/orders/${orderId}/items/${itemId}`, payload)

export const deleteReplacementItem = (orderId: number, itemId: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/replacement/orders/${orderId}/items/${itemId}`)

// ── Pole Numbers ──

export const getPoleNumbers = (params: { keyword?: string; page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<PoleNumberResponse>>>('/auth/replacement/pole-numbers', { params })

export const generatePoleNumber = (payload: PoleNumberRequest) =>
  axiosIns.post<unknown, BaseResponse<PoleNumberResponse>>('/auth/replacement/pole-numbers', payload)

export const getPoleNumberQrCode = (id: number) =>
  axiosIns.get<unknown, Blob>(`/auth/replacement/pole-numbers/${id}/qr-code`, { responseType: 'blob' })

export const batchExportQrCodePdf = (ids: number[]) =>
  axiosIns.post<unknown, Blob>('/auth/replacement/pole-numbers/qr-codes/batch-pdf', ids, { responseType: 'blob' })
