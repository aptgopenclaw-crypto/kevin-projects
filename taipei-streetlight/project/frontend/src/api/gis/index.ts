import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type { GeoJsonResponse, GmlImportDiff } from '@/types/gis'

export const getGisDevices = (params?: { deviceType?: string }) =>
  axiosIns.get<unknown, BaseResponse<GeoJsonResponse>>('/gis/devices', { params })

export const getGisDevicesBounds = (params: {
  minLng: number
  minLat: number
  maxLng: number
  maxLat: number
  deviceType?: string
  zoom?: number
}) =>
  axiosIns.get<unknown, BaseResponse<GeoJsonResponse>>('/gis/devices/bounds', { params })

export const getGisDevicesNearby = (params: {
  lng: number
  lat: number
  radius?: number
}) =>
  axiosIns.get<unknown, BaseResponse<GeoJsonResponse>>('/gis/devices/nearby', { params })

export const getGisZones = (params: { type: string }) =>
  axiosIns.get<unknown, BaseResponse<GeoJsonResponse>>('/gis/zones', { params })

export const getGisZoneDevices = (zoneId: number) =>
  axiosIns.get<unknown, BaseResponse<GeoJsonResponse>>(`/gis/zones/${zoneId}/devices`)

// ── Export ──

export const exportGisGml = (params?: { deviceType?: string; district?: string }) =>
  axiosIns.get('/gis/devices/export/gml', { params, responseType: 'blob' })

export const exportGisOpenData = (params?: { deviceType?: string }) =>
  axiosIns.get('/gis/devices/export/open-data', { params, responseType: 'blob' })

// ── Import ──

export const importGisGmlPreview = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return axiosIns.post<unknown, BaseResponse<GmlImportDiff>>('/gis/devices/import/gml', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const importGisGmlConfirm = (diff: GmlImportDiff) =>
  axiosIns.post<unknown, BaseResponse<number>>('/gis/devices/import/gml/confirm', diff)
