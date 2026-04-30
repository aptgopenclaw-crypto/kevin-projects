import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  DeviceResponse,
  DeviceRequest,
  ComponentReplaceRequest,
  PageResponse,
} from '@/types/device'
import type { DeviceType, DeviceStatus } from '@/types/device'

export const getDevices = (params: {
  deviceType?: DeviceType
  status?: DeviceStatus
  keyword?: string
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<DeviceResponse>>>('/auth/devices', { params })

export const getDeviceById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<DeviceResponse>>(`/auth/devices/${id}`)

export const createDevice = (payload: DeviceRequest) =>
  axiosIns.post<unknown, BaseResponse<DeviceResponse>>('/auth/devices', payload)

export const updateDevice = (id: number, payload: DeviceRequest) =>
  axiosIns.put<unknown, BaseResponse<DeviceResponse>>(`/auth/devices/${id}`, payload)

export const deleteDevice = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/devices/${id}`)

export const decommissionDevice = (id: number) =>
  axiosIns.post<unknown, BaseResponse<void>>(`/auth/devices/${id}/decommission`)

export const getComponents = (id: number, includeDecommissioned = false) =>
  axiosIns.get<unknown, BaseResponse<DeviceResponse[]>>(
    `/auth/devices/${id}/components`,
    { params: { includeDecommissioned } },
  )

export const replaceComponent = (poleId: number, payload: ComponentReplaceRequest) =>
  axiosIns.post<unknown, BaseResponse<DeviceResponse>>(
    `/auth/devices/${poleId}/components/replace`,
    payload,
  )

export const exportDevices = (params: {
  format: 'csv' | 'xlsx' | 'ods'
  deviceType?: DeviceType
  status?: DeviceStatus
  keyword?: string
}) =>
  axiosIns.get('/auth/devices/export', {
    params,
    responseType: 'blob',
  })

// ── Device Template Schema ──

export interface SchemaField {
  key: string
  title: string
  type: 'text' | 'number' | 'date' | 'select' | 'checkbox'
  required?: boolean
  options?: string[]
  minimum?: number
  maximum?: number
  placeholder?: string
}

export interface DeviceSchema {
  fields: SchemaField[]
}

export const getDeviceSchema = (deviceType: string) =>
  axiosIns.get<unknown, BaseResponse<DeviceSchema>>(`/auth/device-templates/${deviceType}/schema`)

export const updateDeviceSchema = (deviceType: string, schema: DeviceSchema) =>
  axiosIns.put<unknown, BaseResponse<DeviceSchema>>(`/auth/device-templates/${deviceType}/schema`, schema)
