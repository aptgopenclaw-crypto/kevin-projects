import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  IoTDeviceResponse,
  TelemetryFormatResponse,
  TelemetryFormatRequest,
  TelemetryFormatFieldResponse,
  TelemetryLatestResponse,
  TelemetryHistoryResponse,
  EventRuleResponse,
  EventRuleRequest,
  EventRuleConditionResponse,
  EventRuleConditionRequest,
  EventRuleRecipientResponse,
  EventRuleChannelResponse,
  AlertHistoryResponse,
  DimmingLogResponse,
  DimmingGroupResponse,
  DimmingGroupRequest,
  DimmingScheduleResponse,
  DimmingScheduleRequest,
  IoTMapGeoJson,
  MeterStatusResponse,
} from '@/types/iot'
import type { NotificationChannel } from '@/types/iot'

// ── IoT Device ──

export const registerIoTDevice = (payload: { deviceId: number }) =>
  axiosIns.post<unknown, BaseResponse<IoTDeviceResponse>>('/auth/iot/devices', payload)

export const getIoTDevices = () =>
  axiosIns.get<unknown, BaseResponse<IoTDeviceResponse[]>>('/auth/iot/devices')

// ── Telemetry Format ──

export const createTelemetryFormat = (payload: TelemetryFormatRequest) =>
  axiosIns.post<unknown, BaseResponse<TelemetryFormatResponse>>('/auth/iot/telemetry-formats', payload)

export const getTelemetryFormats = () =>
  axiosIns.get<unknown, BaseResponse<TelemetryFormatResponse[]>>('/auth/iot/telemetry-formats')

export const updateTelemetryFormat = (id: number, payload: TelemetryFormatRequest) =>
  axiosIns.put<unknown, BaseResponse<TelemetryFormatResponse>>(`/auth/iot/telemetry-formats/${id}`, payload)

export const getTelemetryFormatFields = (id: number) =>
  axiosIns.get<unknown, BaseResponse<TelemetryFormatFieldResponse[]>>(`/auth/iot/telemetry-formats/${id}/fields`)

// ── Telemetry Data ──

export const getLatestTelemetry = (deviceId: number) =>
  axiosIns.get<unknown, BaseResponse<TelemetryLatestResponse>>(`/auth/iot/devices/${deviceId}/telemetry/latest`)

export const getTelemetryHistory = (deviceId: number, params?: { from?: string; to?: string; page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<TelemetryHistoryResponse[]>>(`/auth/iot/devices/${deviceId}/telemetry/history`, { params })

// ── Event Rule ──

export const createEventRule = (payload: EventRuleRequest) =>
  axiosIns.post<unknown, BaseResponse<EventRuleResponse>>('/auth/iot/event-rules', payload)

export const getEventRules = () =>
  axiosIns.get<unknown, BaseResponse<EventRuleResponse[]>>('/auth/iot/event-rules')

export const updateEventRule = (id: number, payload: EventRuleRequest) =>
  axiosIns.put<unknown, BaseResponse<EventRuleResponse>>(`/auth/iot/event-rules/${id}`, payload)

export const deleteEventRule = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/iot/event-rules/${id}`)

export const getEventRuleConditions = (id: number) =>
  axiosIns.get<unknown, BaseResponse<EventRuleConditionResponse[]>>(`/auth/iot/event-rules/${id}/conditions`)

export const updateEventRuleConditions = (id: number, payload: EventRuleConditionRequest[]) =>
  axiosIns.put<unknown, BaseResponse<EventRuleConditionResponse[]>>(`/auth/iot/event-rules/${id}/conditions`, payload)

export const getEventRuleRecipients = (id: number) =>
  axiosIns.get<unknown, BaseResponse<EventRuleRecipientResponse[]>>(`/auth/iot/event-rules/${id}/recipients`)

export const updateEventRuleRecipients = (id: number, userIds: string[]) =>
  axiosIns.put<unknown, BaseResponse<EventRuleRecipientResponse[]>>(`/auth/iot/event-rules/${id}/recipients`, userIds)

export const getEventRuleChannels = (id: number) =>
  axiosIns.get<unknown, BaseResponse<EventRuleChannelResponse[]>>(`/auth/iot/event-rules/${id}/channels`)

export const updateEventRuleChannels = (id: number, channels: NotificationChannel[]) =>
  axiosIns.put<unknown, BaseResponse<EventRuleChannelResponse[]>>(`/auth/iot/event-rules/${id}/channels`, channels)

// ── Alert History ──

export const getAlerts = (params?: { severity?: string; status?: string; page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<AlertHistoryResponse[]>>('/auth/iot/alerts', { params })

export const acknowledgeAlert = (id: number) =>
  axiosIns.put<unknown, BaseResponse<AlertHistoryResponse>>(`/auth/iot/alerts/${id}/acknowledge`)

export const resolveAlert = (id: number) =>
  axiosIns.put<unknown, BaseResponse<AlertHistoryResponse>>(`/auth/iot/alerts/${id}/resolve`)

export const exportAlerts = (params?: { severity?: string; status?: string; from?: string; to?: string }) =>
  axiosIns.get('/auth/iot/alerts/export', { params, responseType: 'blob' })

// ── Dimming ──

export const sendInstantDimming = (payload: { deviceId: number; brightness: number }) =>
  axiosIns.post<unknown, BaseResponse<DimmingLogResponse>>('/auth/iot/dimming/instant', payload)

export const sendGroupDimming = (payload: { groupId: number; brightness: number }) =>
  axiosIns.post<unknown, BaseResponse<DimmingLogResponse[]>>('/auth/iot/dimming/group', payload)

export const getDimmingGroups = () =>
  axiosIns.get<unknown, BaseResponse<DimmingGroupResponse[]>>('/auth/iot/dimming/groups')

export const createDimmingGroup = (payload: DimmingGroupRequest) =>
  axiosIns.post<unknown, BaseResponse<DimmingGroupResponse>>('/auth/iot/dimming/groups', payload)

export const updateDimmingGroup = (id: number, payload: DimmingGroupRequest) =>
  axiosIns.put<unknown, BaseResponse<DimmingGroupResponse>>(`/auth/iot/dimming/groups/${id}`, payload)

export const deleteDimmingGroup = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/iot/dimming/groups/${id}`)

export const getDimmingSchedules = () =>
  axiosIns.get<unknown, BaseResponse<DimmingScheduleResponse[]>>('/auth/iot/dimming/schedules')

export const createDimmingSchedule = (payload: DimmingScheduleRequest) =>
  axiosIns.post<unknown, BaseResponse<DimmingScheduleResponse>>('/auth/iot/dimming/schedules', payload)

export const updateDimmingSchedule = (id: number, payload: DimmingScheduleRequest) =>
  axiosIns.put<unknown, BaseResponse<DimmingScheduleResponse>>(`/auth/iot/dimming/schedules/${id}`, payload)

export const deleteDimmingSchedule = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/iot/dimming/schedules/${id}`)

export const syncDimmingCommand = (deviceId: number) =>
  axiosIns.post<unknown, BaseResponse<DimmingLogResponse>>(`/auth/iot/dimming/sync/${deviceId}`)

export const getDimmingLogs = (params?: { deviceId?: number; result?: string; page?: number; size?: number }) =>
  axiosIns.get<unknown, BaseResponse<DimmingLogResponse[]>>('/auth/iot/dimming/logs', { params })

// ── Map ──

export const getIoTMapStatus = () =>
  axiosIns.get<unknown, BaseResponse<IoTMapGeoJson>>('/auth/iot/map/status')

// ── Meter ──

export const getMeterStatus = () =>
  axiosIns.get<unknown, BaseResponse<MeterStatusResponse>>('/auth/iot/meters/status')
