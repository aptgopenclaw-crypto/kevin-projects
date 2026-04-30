// ── Enums ──
export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL'
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED'
export type DimmingResult = 'PENDING' | 'SUCCESS' | 'FAIL' | 'TIMEOUT'
export type FieldType = 'NUMBER' | 'STRING' | 'BOOLEAN'
export type ComparisonOperator = 'GT' | 'GTE' | 'LT' | 'LTE' | 'EQ' | 'NEQ'
export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'LINE'

// ── IoT Device ──
export interface IoTDeviceResponse {
  id: number
  deviceId: number
  deviceCode: string
  deviceToken: string
  registeredAt: string
}

// ── Telemetry Format ──
export interface TelemetryFormatFieldResponse {
  id: number
  fieldKey: string
  fieldName: string
  fieldType: FieldType
  unit: string | null
  sortOrder: number
}

export interface TelemetryFormatResponse {
  id: number
  formatName: string
  description: string | null
  fields: TelemetryFormatFieldResponse[]
  createdAt: string
  updatedAt: string
}

export interface TelemetryFormatRequest {
  formatName: string
  description?: string
  fields: {
    fieldKey: string
    fieldName: string
    fieldType: FieldType
    unit?: string
    sortOrder?: number
  }[]
}

// ── Telemetry Data ──
export interface TelemetryLatestResponse {
  deviceId: number
  receivedAt: string
  payload: Record<string, unknown>
}

export interface TelemetryHistoryResponse {
  id: number
  deviceId: number
  receivedAt: string
  payload: Record<string, unknown>
}

// ── Event Rule ──
export interface EventRuleResponse {
  id: number
  ruleName: string
  description: string | null
  severity: AlertSeverity
  enabled: boolean
  cooldownMinutes: number
  createdAt: string
  updatedAt: string
}

export interface EventRuleRequest {
  ruleName: string
  description?: string
  severity: AlertSeverity
  enabled?: boolean
  cooldownMinutes?: number
}

export interface EventRuleConditionResponse {
  id: number
  fieldKey: string
  operator: ComparisonOperator
  threshold: number
  sortOrder: number
}

export interface EventRuleConditionRequest {
  fieldKey: string
  operator: ComparisonOperator
  threshold: number
  sortOrder?: number
}

export interface EventRuleRecipientResponse {
  id: number
  userId: string
}

export interface EventRuleChannelResponse {
  id: number
  channel: NotificationChannel
}

// ── Alert History ──
export interface AlertHistoryResponse {
  id: number
  ruleId: number | null
  ruleName: string | null
  deviceId: number
  deviceCode: string | null
  severity: AlertSeverity
  status: AlertStatus
  message: string
  triggeredAt: string
  acknowledgedAt: string | null
  acknowledgedBy: string | null
  resolvedAt: string | null
  resolvedBy: string | null
}

// ── Dimming ──
export interface DimmingLogResponse {
  id: number
  deviceId: number
  deviceCode: string | null
  brightness: number
  result: DimmingResult
  sentAt: string
  ackedAt: string | null
  failReason: string | null
}

export interface DimmingGroupResponse {
  id: number
  groupName: string
  description: string | null
  deviceIds: number[]
  createdAt: string
  updatedAt: string
}

export interface DimmingGroupRequest {
  groupName: string
  description?: string
  deviceIds: number[]
}

export interface DimmingScheduleResponse {
  id: number
  scheduleName: string
  groupId: number
  groupName: string | null
  brightness: number
  cron: string | null
  oneTimeAt: string | null
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface DimmingScheduleRequest {
  scheduleName: string
  groupId: number
  brightness: number
  cron?: string
  oneTimeAt?: string
  enabled?: boolean
}

// ── Map Status ──
export interface IoTMapFeature {
  type: 'Feature'
  geometry: { type: 'Point'; coordinates: [number, number] }
  properties: Record<string, unknown>
}

export interface IoTMapGeoJson {
  type: 'FeatureCollection'
  features: IoTMapFeature[]
}

// ── Meter ──
export interface MeterStatusResponse {
  totalMeters: number
  onlineMeters: number
  offlineMeters: number
  anomalyMeters: number
}
