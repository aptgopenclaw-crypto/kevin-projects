import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  RepairTicketResponse,
  RepairTicketRequest,
  RepairTicketQueryParams,
  DispatchRequest,
  DispatchResponse,
  CompletionReportRequest,
  AttachmentResponse,
  PageResponse,
} from '@/types/repair'

// ── Repair Tickets ──

export const getRepairTickets = (params: RepairTicketQueryParams & { page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<RepairTicketResponse>>>('/auth/repair/tickets', { params })

export const getRepairTicketById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<RepairTicketResponse>>(`/auth/repair/tickets/${id}`)

export const createRepairTicket = (payload: RepairTicketRequest) =>
  axiosIns.post<unknown, BaseResponse<RepairTicketResponse>>('/auth/repair/tickets', payload)

export const updateRepairTicket = (id: number, payload: RepairTicketRequest) =>
  axiosIns.put<unknown, BaseResponse<RepairTicketResponse>>(`/auth/repair/tickets/${id}`, payload)

export const acceptRepairTicket = (id: number) =>
  axiosIns.post<unknown, BaseResponse<RepairTicketResponse>>(`/auth/repair/tickets/${id}/accept`)

export const dispatchRepairTicket = (id: number, payload: DispatchRequest) =>
  axiosIns.post<unknown, BaseResponse<DispatchResponse>>(`/auth/repair/tickets/${id}/dispatch`, payload)

export const completeRepairTicket = (id: number, payload: CompletionReportRequest) =>
  axiosIns.post<unknown, BaseResponse<RepairTicketResponse>>(`/auth/repair/tickets/${id}/complete`, payload)

export const transferRepairTicket = (id: number) =>
  axiosIns.post<unknown, BaseResponse<RepairTicketResponse>>(`/auth/repair/tickets/${id}/transfer`)

export const getDispatches = (ticketId: number) =>
  axiosIns.get<unknown, BaseResponse<DispatchResponse[]>>(`/auth/repair/tickets/${ticketId}/dispatches`)

// ── Attachments ──

export const getAttachments = (ticketId: number) =>
  axiosIns.get<unknown, BaseResponse<AttachmentResponse[]>>(`/auth/repair/tickets/${ticketId}/attachments`)

export const uploadAttachment = (
  ticketId: number,
  file: File,
  meta: { phase?: string; description?: string; gpsLat?: number; gpsLng?: number },
) => {
  const formData = new FormData()
  formData.append('file', file)
  if (meta.phase) formData.append('phase', meta.phase)
  if (meta.description) formData.append('description', meta.description)
  if (meta.gpsLat != null) formData.append('gpsLat', String(meta.gpsLat))
  if (meta.gpsLng != null) formData.append('gpsLng', String(meta.gpsLng))

  return axiosIns.post<unknown, BaseResponse<AttachmentResponse>>(
    `/auth/repair/tickets/${ticketId}/attachments`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
}

export const downloadAttachment = (attachmentId: number) =>
  axiosIns.get(`/auth/repair/attachments/${attachmentId}/download`, { responseType: 'blob' })

// ── Public Repair (no auth) ──

export interface PublicRepairPayload {
  reporterName: string
  reporterPhone: string
  reporterEmail?: string
  reportDescription: string
  reportAddress: string
  poleNumber?: string
  captchaKey: string
  captchaValue: string
  privacyAgreed: boolean
}

export interface PublicRepairResult {
  ticketNumber: string
  message: string
}

export interface PublicRepairStatusResult {
  ticketNumber: string
  status: string
  statusLabel: string
  createdAt: string
  updatedAt: string
}

export const submitPublicRepair = (payload: PublicRepairPayload) =>
  axiosIns.post<unknown, BaseResponse<PublicRepairResult>>('/noauth/public/repair', payload)

export const queryRepairStatus = (ticketNo: string, phone: string) =>
  axiosIns.get<unknown, BaseResponse<PublicRepairStatusResult>>(`/noauth/public/repair/${ticketNo}/status`, { params: { phone } })
