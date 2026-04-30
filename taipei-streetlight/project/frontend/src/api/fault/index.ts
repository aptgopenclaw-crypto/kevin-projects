import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  FaultTicketResponse,
  FaultTicketRequest,
  FaultCorrelationResponse,
  FaultTicketStatus,
  PageResponse,
} from '@/types/fault'

export const getFaultTickets = (params: {
  status?: FaultTicketStatus
  keyword?: string
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<FaultTicketResponse>>>('/auth/faults', { params })

export const getFaultTicketById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<FaultTicketResponse>>(`/auth/faults/${id}`)

export const createFaultTicket = (payload: FaultTicketRequest) =>
  axiosIns.post<unknown, BaseResponse<FaultTicketResponse>>('/auth/faults', payload)

export const resolveFaultTicket = (id: number, resolutionNote?: string) =>
  axiosIns.post<unknown, BaseResponse<FaultTicketResponse>>(
    `/auth/faults/${id}/resolve`,
    null,
    { params: { resolutionNote } },
  )

export const getFaultCorrelations = (params: {
  status?: string
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<FaultCorrelationResponse>>>(
    '/auth/faults/correlations',
    { params },
  )
