export type DeviceType = 'POLE' | 'LUMINAIRE' | 'PANEL_BOX' | 'CONTROLLER' | 'POWER_EQUIPMENT' | 'ATTACHMENT'
export type DeviceStatus = 'ACTIVE' | 'REPORTED' | 'UNDER_REPAIR' | 'INACTIVE' | 'DECOMMISSIONED'
export type ConnectivityType = 'NONE' | 'DIRECT' | 'GATEWAY'

export interface DeviceResponse {
  id: number
  deviceType: DeviceType
  deviceCode: string
  deviceName: string | null
  twd97X: number | null
  twd97Y: number | null
  lng: number | null
  lat: number | null
  elevation: number | null
  twd67X: number | null
  twd67Y: number | null
  taipowerCoord: string | null
  deptId: number | null
  deptName: string | null
  contractId: number | null
  propertyOwner: string | null
  status: DeviceStatus
  installedAt: string | null
  decommissionedAt: string | null
  parentDeviceId: number | null
  parentDeviceCode: string | null
  mountPosition: string | null
  connectivityType: ConnectivityType | null
  networkConfig: Record<string, unknown> | null
  lastHeartbeatAt: string | null
  circuitId: number | null
  circuitNumber: string | null
  attributes: Record<string, unknown> | null
  childrenCount: number
  children: DeviceResponse[] | null
  createdBy: string | null
  createdAt: string
  updatedAt: string
}

export interface DeviceRequest {
  deviceType: DeviceType
  deviceCode: string
  deviceName?: string
  twd97X?: number
  twd97Y?: number
  lng?: number
  lat?: number
  elevation?: number
  twd67X?: number
  twd67Y?: number
  taipowerCoord?: string
  deptId?: number | null
  contractId?: number
  propertyOwner?: string
  installedAt?: string
  parentDeviceId?: number
  mountPosition?: string
  connectivityType?: ConnectivityType
  networkConfig?: Record<string, unknown>
  circuitId?: number
  attributes?: Record<string, unknown>
}

export interface ComponentReplaceRequest {
  oldDeviceId: number
  newDevice: DeviceRequest
  reason?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
