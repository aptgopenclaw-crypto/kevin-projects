import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  InspectionTaskResponse,
  InspectionTaskRequest,
  InspectionRecordResponse,
  InspectionRecordRequest,
  PageResponse,
} from '@/types/repair'

// ── Tasks ──

export const getInspectionTasks = (params: { page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<InspectionTaskResponse>>>('/auth/inspection/tasks', { params })

export const getInspectionTaskById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<InspectionTaskResponse>>(`/auth/inspection/tasks/${id}`)

export const createInspectionTask = (payload: InspectionTaskRequest) =>
  axiosIns.post<unknown, BaseResponse<InspectionTaskResponse>>('/auth/inspection/tasks', payload)

export const updateInspectionTask = (id: number, payload: InspectionTaskRequest) =>
  axiosIns.put<unknown, BaseResponse<InspectionTaskResponse>>(`/auth/inspection/tasks/${id}`, payload)

export const deactivateInspectionTask = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/inspection/tasks/${id}`)

// ── Records ──

export const getInspectionRecords = (taskId: number, params: { page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<InspectionRecordResponse>>>(
    `/auth/inspection/tasks/${taskId}/records`,
    { params },
  )

export const getInspectionRecordById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<InspectionRecordResponse>>(`/auth/inspection/records/${id}`)

export const createInspectionRecord = (payload: InspectionRecordRequest) =>
  axiosIns.post<unknown, BaseResponse<InspectionRecordResponse>>('/auth/inspection/records', payload)
