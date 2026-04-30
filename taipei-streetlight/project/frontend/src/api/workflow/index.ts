import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  WorkflowInstanceResponse,
  WorkflowStepLogResponse,
  WorkflowTransitionRequest,
  DelegateSettingResponse,
  DelegateSettingRequest,
  DelegateCandidateDto,
  PageResponse,
} from '@/types/workflow'

// ── Workflow ──

export const getPendingTasks = (params: { page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<WorkflowInstanceResponse>>>(
    '/auth/workflow/pending',
    { params },
  )

export const getStepLogs = (instanceId: number) =>
  axiosIns.get<unknown, BaseResponse<WorkflowStepLogResponse[]>>(
    `/auth/workflow/${instanceId}/logs`,
  )

export const transitionWorkflow = (instanceId: number, payload: WorkflowTransitionRequest) =>
  axiosIns.post<unknown, BaseResponse<void>>(
    `/auth/workflow/${instanceId}/transition`,
    payload,
  )

export const cancelWorkflow = (instanceId: number) =>
  axiosIns.post<unknown, BaseResponse<void>>(`/auth/workflow/${instanceId}/cancel`)

// ── Delegate ──

export const getDelegateCandidates = () =>
  axiosIns.get<unknown, BaseResponse<DelegateCandidateDto[]>>('/auth/delegates/candidates')

export const getDelegates = () =>
  axiosIns.get<unknown, BaseResponse<DelegateSettingResponse[]>>('/auth/delegates')

export const createDelegate = (payload: DelegateSettingRequest) =>
  axiosIns.post<unknown, BaseResponse<DelegateSettingResponse>>('/auth/delegates', payload)

export const deleteDelegate = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/delegates/${id}`)
