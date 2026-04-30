export interface GeoJsonResponse {
  type: 'FeatureCollection'
  features: GeoJsonFeature[]
}

export interface GeoJsonFeature {
  type: 'Feature'
  geometry: GeoJsonGeometry
  properties: DeviceGeoProperties
}

export interface GeoJsonGeometry {
  type: 'Point' | 'Polygon'
  coordinates: [number, number] | number[][][] // Point or Polygon
}

export interface DeviceGeoProperties {
  id: number
  deviceCode: string
  deviceName: string
  deviceType: string
  status: string
  deptId: number | null
}

// ── GML Import ──

export interface GmlImportRow {
  existingId: number | null
  deviceCode: string
  deviceName: string | null
  deviceType: string | null
  status: string | null
  twd97X: number | null
  twd97Y: number | null
  lng: number | null
  lat: number | null
  diffFields: string | null
}

export interface GmlImportDiff {
  toAdd: GmlImportRow[]
  toUpdate: GmlImportRow[]
  toDelete: GmlImportRow[]
  totalParsed: number
}
