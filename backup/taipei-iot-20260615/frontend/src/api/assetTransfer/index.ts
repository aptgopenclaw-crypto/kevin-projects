import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  AssetTransferApplicationDto,
  AssetTransferCreateRequest,
  AssetTransferActionRequest,
  AssetTransferRejectRequest,
} from '@/types/assetTransfer'

export const createApplication = (req: AssetTransferCreateRequest) =>
  axiosIns.post<unknown, BaseResponse<AssetTransferApplicationDto>>(
    '/auth/asset-transfer/create',
    req,
  )

export const submitApplication = (id: number) =>
  axiosIns.post<unknown, BaseResponse<AssetTransferApplicationDto>>(
    `/auth/asset-transfer/submit/${id}`,
  )

export const approveApplication = (id: number, req: AssetTransferActionRequest) =>
  axiosIns.post<unknown, BaseResponse<AssetTransferApplicationDto>>(
    `/auth/asset-transfer/approve/${id}`,
    req,
  )

export const rejectApplication = (id: number, req: AssetTransferRejectRequest) =>
  axiosIns.post<unknown, BaseResponse<AssetTransferApplicationDto>>(
    `/auth/asset-transfer/reject/${id}`,
    req,
  )

export const resubmitApplication = (id: number, req: AssetTransferActionRequest) =>
  axiosIns.post<unknown, BaseResponse<AssetTransferApplicationDto>>(
    `/auth/asset-transfer/resubmit/${id}`,
    req,
  )

export const getApplication = (id: number) =>
  axiosIns.get<unknown, BaseResponse<AssetTransferApplicationDto>>(`/auth/asset-transfer/${id}`)

export const getMyApplications = () =>
  axiosIns.get<unknown, BaseResponse<AssetTransferApplicationDto[]>>('/auth/asset-transfer/my')

export const getPendingTasks = () =>
  axiosIns.get<unknown, BaseResponse<AssetTransferApplicationDto[]>>(
    '/auth/asset-transfer/pending',
  )
