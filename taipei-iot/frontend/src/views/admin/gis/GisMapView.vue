<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import Cluster from 'ol/source/Cluster'
import WebGLPointsLayer from 'ol/layer/WebGLPoints'
import XYZ from 'ol/source/XYZ'
import OSM from 'ol/source/OSM'
import GeoJSON from 'ol/format/GeoJSON'
import Feature from 'ol/Feature'
import Point from 'ol/geom/Point'
import { Style, Circle as CircleStyle, Fill, Stroke, Text } from 'ol/style'
import { fromLonLat, toLonLat, transformExtent } from 'ol/proj'
import Overlay from 'ol/Overlay'
import { DragBox } from 'ol/interaction'
import { platformModifierKeyOnly } from 'ol/events/condition'
import { getGisDevices, getGisDevicesBounds, getGisDevicesNearby, getGisZones, getGisZoneDevices, exportGisGml, exportGisOpenData, importGisGmlPreview, importGisGmlConfirm } from '@/api/gis'
import type { GeoJsonResponse, GmlImportDiff } from '@/types/gis'
import { ElMessage } from 'element-plus'
import CircleGeom from 'ol/geom/Circle'

const { t } = useI18n()

const mapContainer = ref<HTMLDivElement>()
const popupEl = ref<HTMLDivElement>()
let map: Map | null = null
let overlay: Overlay | null = null
const loading = ref(false)
const deviceCount = ref(0)
const selectedDevice = ref<Record<string, unknown> | null>(null)
const deviceTypeFilter = ref('')

// ── Box select state ──
const boxSelectResults = ref<Record<string, unknown>[]>([])
const showBottomPanel = ref(false)
const bottomPanelTitle = ref('')

// ── Zone layer state ──
const showZoneLayer = ref(false)
const zoneTypeFilter = ref('ADMIN_DISTRICT')

// ── Nearby search state ──
const nearbyMode = ref(false)
const nearbyRadius = ref(500)
let nearbyCircleLayer: VectorLayer<VectorSource> | null = null

// ── Export / Import state ──
const showImportDialog = ref(false)
const importDiff = ref<GmlImportDiff | null>(null)
const importLoading = ref(false)

// NLSC base map
function createNlscTileSource(): XYZ {
  return new XYZ({
    url: 'https://wmts.nlsc.gov.tw/wmts/EMAP/default/GoogleMapsCompatible/{z}/{y}/{x}',
    attributions: '© <a href="https://maps.nlsc.gov.tw/" target="_blank">NLSC</a>',
    crossOrigin: 'anonymous',
    maxZoom: 18,
  })
}

function createBaseLayer(): TileLayer<XYZ | OSM> {
  const nlsc = createNlscTileSource()
  const layer = new TileLayer({ source: nlsc })
  nlsc.on('tileloaderror', () => {
    console.warn('NLSC tile load failed, falling back to OSM')
    layer.setSource(new OSM())
  })
  return layer
}

// Device point color mapping
const DEVICE_COLORS: Record<string, string> = {
  STREETLIGHT: '#f5a623',
  POLE: '#4a90d9',
  CONTROLLER: '#7ed321',
  LUMINAIRE: '#bd10e0',
  default: '#9b9b9b',
}

const CLUSTER_THRESHOLD_ZOOM = 14

// ── Data sources ──
let rawSource: VectorSource       // holds all individual features
let clusterSource: Cluster        // wraps rawSource for low zoom
let clusterLayer: VectorLayer<Cluster>
let webglLayer: WebGLPointsLayer<VectorSource>

// ── Zone layer ──
let zoneSource: VectorSource
let zoneLayer: VectorLayer<VectorSource>

const ZONE_COLORS: Record<string, string> = {
  ADMIN_DISTRICT: 'rgba(64, 158, 255, 0.15)',
  SQUAD: 'rgba(103, 194, 58, 0.15)',
  TAIPOWER: 'rgba(230, 162, 60, 0.15)',
  VENDOR: 'rgba(245, 108, 108, 0.15)',
}

const ZONE_STROKE_COLORS: Record<string, string> = {
  ADMIN_DISTRICT: '#409eff',
  SQUAD: '#67c23a',
  TAIPOWER: '#e6a23c',
  VENDOR: '#f56c6c',
}

function getDeviceColorNum(type: string): number {
  const map: Record<string, number> = {
    STREETLIGHT: 1, POLE: 2, CONTROLLER: 3, LUMINAIRE: 4,
    PANEL_BOX: 5, POWER_EQUIPMENT: 6, ATTACHMENT: 7,
  }
  return map[type] || 0
}

function clusterStyle(feature: Feature): Style {
  const features = feature.get('features') as Feature[]
  const size = features?.length ?? 1

  if (size === 1) {
    const f = features[0]
    const type = (f.get('deviceType') as string) || 'default'
    const status = f.get('status') as string
    const color = DEVICE_COLORS[type] || DEVICE_COLORS.default
    const opacity = status === 'ACTIVE' ? 1 : 0.5
    return new Style({
      image: new CircleStyle({
        radius: 6,
        fill: new Fill({ color: hexToRgba(color, opacity) }),
        stroke: new Stroke({ color: '#fff', width: 1.5 }),
      }),
    })
  }

  // Cluster circle
  const radius = Math.min(8 + Math.log2(size) * 4, 30)
  return new Style({
    image: new CircleStyle({
      radius,
      fill: new Fill({ color: 'rgba(74, 144, 217, 0.7)' }),
      stroke: new Stroke({ color: '#fff', width: 2 }),
    }),
    text: new Text({
      text: size > 999 ? `${Math.round(size / 1000)}k` : String(size),
      fill: new Fill({ color: '#fff' }),
      font: 'bold 12px Inter, sans-serif',
    }),
  })
}

function hexToRgba(hex: string, alpha: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r},${g},${b},${alpha})`
}

// ── WebGL flat style for high zoom (individual points) ──
const webglFlatStyle = {
  'circle-radius': 6,
  'circle-fill-color': [
    'match', ['get', 'deviceColorNum'],
    1, '#f5a623',  // STREETLIGHT
    2, '#4a90d9',  // POLE
    3, '#7ed321',  // CONTROLLER
    4, '#bd10e0',  // LUMINAIRE
    5, '#5fc992',  // PANEL_BOX
    6, '#ff9664',  // POWER_EQUIPMENT
    7, '#999999',  // ATTACHMENT
    '#9b9b9b',     // default
  ],
  'circle-stroke-color': '#ffffff',
  'circle-stroke-width': 1.5,
  'circle-opacity': [
    'match', ['get', 'status'],
    'ACTIVE', 1.0,
    0.5,
  ],
}

// ── Layer visibility by zoom ──
function updateLayerVisibility() {
  if (!map) return
  const zoom = map.getView().getZoom() ?? 14
  const useCluster = zoom < CLUSTER_THRESHOLD_ZOOM

  clusterLayer.setVisible(useCluster)
  webglLayer.setVisible(!useCluster)
}

// ── Load devices with zoom-aware payload ──
let moveendTimer: ReturnType<typeof setTimeout> | null = null

async function loadDevicesByBounds() {
  if (!map) return
  const extent = map.getView().calculateExtent(map.getSize())
  const [minLng, minLat, maxLng, maxLat] = transformExtent(extent, 'EPSG:3857', 'EPSG:4326')
  const zoom = Math.round(map.getView().getZoom() ?? 14)

  loading.value = true
  try {
    const params = {
      minLng, minLat, maxLng, maxLat, zoom,
      ...(deviceTypeFilter.value ? { deviceType: deviceTypeFilter.value } : {}),
    }
    const res = await getGisDevicesBounds(params)
    if (res.errorCode === '00000') {
      renderDevices(res.body)
      deviceCount.value = res.body.features.length
    }
  } catch {
    // silently fail on auto-reload
  } finally {
    loading.value = false
  }
}

function onMoveEnd() {
  if (moveendTimer) clearTimeout(moveendTimer)
  moveendTimer = setTimeout(() => {
    loadDevicesByBounds()
    updateLayerVisibility()
  }, 300)
}

async function loadAllDevices() {
  loading.value = true
  try {
    const params = deviceTypeFilter.value ? { deviceType: deviceTypeFilter.value } : undefined
    const res = await getGisDevices(params)
    if (res.errorCode === '00000') {
      renderDevices(res.body)
      deviceCount.value = res.body.features.length
    } else {
      ElMessage.error(res.errorMsg || t('gis.loadFailed'))
    }
  } catch {
    ElMessage.error(t('gis.loadFailed'))
  } finally {
    loading.value = false
  }
}

function renderDevices(geojson: GeoJsonResponse) {
  const format = new GeoJSON()
  const features = format.readFeatures(geojson, {
    dataProjection: 'EPSG:4326',
    featureProjection: 'EPSG:3857',
  })

  // Add deviceColorNum for WebGL style expression
  for (const f of features) {
    const type = (f.get('deviceType') as string) || ''
    f.set('deviceColorNum', getDeviceColorNum(type))
  }

  rawSource.clear()
  rawSource.addFeatures(features)
}

// ── Zone layer functions ──
function zoneStyle(feature: Feature): Style {
  const zoneType = (feature.get('zoneType') as string) || 'ADMIN_DISTRICT'
  return new Style({
    fill: new Fill({ color: ZONE_COLORS[zoneType] || ZONE_COLORS.ADMIN_DISTRICT }),
    stroke: new Stroke({
      color: ZONE_STROKE_COLORS[zoneType] || ZONE_STROKE_COLORS.ADMIN_DISTRICT,
      width: 2,
    }),
    text: new Text({
      text: feature.get('zoneName') as string || '',
      font: 'bold 13px Inter, sans-serif',
      fill: new Fill({ color: '#303133' }),
      stroke: new Stroke({ color: '#fff', width: 3 }),
      overflow: true,
    }),
  })
}

async function loadZones() {
  if (!showZoneLayer.value) {
    zoneSource?.clear()
    return
  }
  try {
    const res = await getGisZones({ type: zoneTypeFilter.value })
    if (res.errorCode === '00000') {
      const format = new GeoJSON()
      const features = format.readFeatures(res.body, {
        dataProjection: 'EPSG:4326',
        featureProjection: 'EPSG:3857',
      })
      zoneSource.clear()
      zoneSource.addFeatures(features)
    }
  } catch {
    ElMessage.error(t('gis.loadZonesFailed'))
  }
}

async function onZoneClick(feature: Feature) {
  const zoneId = feature.get('id') as number
  const zoneName = feature.get('zoneName') as string
  if (!zoneId) return

  loading.value = true
  try {
    const res = await getGisZoneDevices(zoneId)
    if (res.errorCode === '00000') {
      const devices: Record<string, unknown>[] = res.body.features.map((f) => {
        const coords = f.geometry.coordinates as [number, number]
        return {
          id: f.properties.id,
          deviceCode: f.properties.deviceCode,
          deviceName: f.properties.deviceName,
          deviceType: f.properties.deviceType,
          status: f.properties.status,
          lng: coords[0],
          lat: coords[1],
        }
      })
      boxSelectResults.value = devices
      bottomPanelTitle.value = t('gis.zoneDevices', { zone: zoneName, count: devices.length })
      showBottomPanel.value = devices.length > 0
    }
  } catch {
    ElMessage.error(t('gis.loadFailed'))
  } finally {
    loading.value = false
  }
}

// ── Nearby search ──
function toggleNearbyMode() {
  nearbyMode.value = !nearbyMode.value
  if (!nearbyMode.value) {
    clearNearbyCircle()
  }
}

function clearNearbyCircle() {
  if (nearbyCircleLayer) {
    nearbyCircleLayer.getSource()?.clear()
  }
}

async function onNearbyClick(coordinate: number[]) {
  if (!nearbyMode.value || !map) return

  const [lng, lat] = toLonLat(coordinate)
  clearNearbyCircle()

  // Draw radius circle
  const circleSource = nearbyCircleLayer!.getSource()!
  const circle = new Feature({
    geometry: new CircleGeom(coordinate, nearbyRadius.value),
  })
  circleSource.clear()
  circleSource.addFeatures([circle])

  loading.value = true
  try {
    const res = await getGisDevicesNearby({
      lng: Math.round(lng * 1e7) / 1e7,
      lat: Math.round(lat * 1e7) / 1e7,
      radius: nearbyRadius.value,
    })
    if (res.errorCode === '00000') {
      const devices: Record<string, unknown>[] = res.body.features.map((f) => {
        const coords = f.geometry.coordinates as [number, number]
        return {
          id: f.properties.id,
          deviceCode: f.properties.deviceCode,
          deviceName: f.properties.deviceName,
          deviceType: f.properties.deviceType,
          status: f.properties.status,
          lng: coords[0],
          lat: coords[1],
        }
      })
      boxSelectResults.value = devices
      bottomPanelTitle.value = t('gis.nearbyResult', { count: devices.length, radius: nearbyRadius.value })
      showBottomPanel.value = devices.length > 0
    }
  } catch {
    ElMessage.error(t('gis.loadFailed'))
  } finally {
    loading.value = false
  }
}

// ── DragBox interaction ──
let dragBox: DragBox

function initDragBox() {
  dragBox = new DragBox({ condition: platformModifierKeyOnly })

  dragBox.on('boxend', () => {
    const boxExtent = dragBox.getGeometry().getExtent()
    const results: Record<string, unknown>[] = []

    // Check from whichever source is currently visible
    const zoom = map?.getView().getZoom() ?? 14
    const source = zoom < CLUSTER_THRESHOLD_ZOOM ? clusterSource : rawSource
    const features = source.getFeaturesInExtent(boxExtent)

    for (const f of features) {
      // Unwrap cluster features
      const innerFeatures = f.get('features') as Feature[] | undefined
      const list = innerFeatures || [f]
      for (const inner of list) {
        const geom = inner.getGeometry()
        if (geom && boxExtent && geom.intersectsExtent(boxExtent)) {
          const props = inner.getProperties()
          let lng: number | undefined, lat: number | undefined
          if (geom instanceof Point) {
            const [pLng, pLat] = toLonLat(geom.getCoordinates())
            lng = Math.round(pLng * 1e7) / 1e7
            lat = Math.round(pLat * 1e7) / 1e7
          }
          results.push({
            id: props.id,
            deviceCode: props.deviceCode || '-',
            deviceName: props.deviceName || '-',
            deviceType: props.deviceType,
            status: props.status || '-',
            lng, lat,
          })
        }
      }
    }

    boxSelectResults.value = results
    bottomPanelTitle.value = t('gis.boxSelectResult', { count: results.length })
    showBottomPanel.value = results.length > 0
  })

  map!.addInteraction(dragBox)
}

// ── Map initialization ──
function initMap() {
  if (!mapContainer.value) return

  rawSource = new VectorSource()
  clusterSource = new Cluster({ source: rawSource, distance: 40 })

  clusterLayer = new VectorLayer({
    source: clusterSource,
    style: clusterStyle as never,
    visible: true,
  })

  webglLayer = new WebGLPointsLayer({
    source: rawSource,
    style: webglFlatStyle as never,
    visible: false,
  })

  // Zone polygon layer
  zoneSource = new VectorSource()
  zoneLayer = new VectorLayer({
    source: zoneSource,
    style: zoneStyle as never,
    visible: false,
  })

  // Nearby radius circle layer
  const nearbyCircleSource = new VectorSource()
  nearbyCircleLayer = new VectorLayer({
    source: nearbyCircleSource,
    style: new Style({
      fill: new Fill({ color: 'rgba(64, 158, 255, 0.1)' }),
      stroke: new Stroke({ color: '#409eff', width: 2, lineDash: [6, 4] }),
    }),
  })

  overlay = new Overlay({
    element: popupEl.value,
    autoPan: { animation: { duration: 250 } },
    offset: [0, -10],
  })

  map = new Map({
    target: mapContainer.value,
    layers: [createBaseLayer(), zoneLayer, clusterLayer, webglLayer, nearbyCircleLayer],
    overlays: [overlay],
    view: new View({
      center: fromLonLat([121.5654, 25.0330]),
      zoom: 14,
      minZoom: 7,
      maxZoom: 19,
    }),
  })

  // Click handler — show popup, zone click, nearby click
  map.on('click', (evt) => {
    // Nearby mode: click sets search point
    if (nearbyMode.value) {
      onNearbyClick(evt.coordinate)
      return
    }

    const feature = map!.forEachFeatureAtPixel(evt.pixel, (f) => f)
    if (feature) {
      // Check if it's a zone feature
      if (feature.get('zoneCode')) {
        onZoneClick(feature as Feature)
        return
      }

      // Unwrap cluster
      const inner = (feature.get('features') as Feature[] | undefined)?.[0] || feature
      const props = inner.getProperties()
      const geom = inner.getGeometry()
      let lng: number | undefined, lat: number | undefined
      if (geom && geom instanceof Point) {
        const [pLng, pLat] = toLonLat(geom.getCoordinates())
        lng = Math.round(pLng * 1e7) / 1e7
        lat = Math.round(pLat * 1e7) / 1e7
      }

      // If it's a multi-feature cluster, zoom in instead
      const clusterFeatures = feature.get('features') as Feature[] | undefined
      if (clusterFeatures && clusterFeatures.length > 1) {
        const extent = new VectorSource({ features: clusterFeatures }).getExtent()
        map!.getView().fit(extent, { padding: [60, 60, 60, 60], maxZoom: 17, duration: 500 })
        return
      }

      selectedDevice.value = {
        deviceCode: props.deviceCode,
        deviceName: props.deviceName,
        deviceType: props.deviceType,
        status: props.status,
        id: props.id,
        lng, lat,
      }
      overlay!.setPosition(evt.coordinate)
    } else {
      closePopup()
    }
  })

  map.on('pointermove', (evt) => {
    const hit = map!.hasFeatureAtPixel(evt.pixel)
    map!.getTargetElement().style.cursor = hit ? 'pointer' : ''
  })

  // moveend auto-reload
  map.on('moveend', onMoveEnd)

  initDragBox()
  updateLayerVisibility()
}

function closePopup() {
  selectedDevice.value = null
  overlay?.setPosition(undefined)
}

function zoomToAll() {
  if (!rawSource.getFeatures().length) return
  const extent = rawSource.getExtent()
  if (!extent || extent[0] === Infinity) return
  map?.getView().fit(extent, { padding: [50, 50, 50, 50], maxZoom: 17, duration: 500 })
}

function flyToDevice(device: Record<string, unknown>) {
  if (!map || !device.lng || !device.lat) return
  const coord = fromLonLat([device.lng as number, device.lat as number])
  map.getView().animate({ center: coord, zoom: 17, duration: 500 })

  // Open popup
  selectedDevice.value = device
  nextTick(() => overlay?.setPosition(coord))
}

watch(deviceTypeFilter, () => {
  loadDevicesByBounds()
})

watch(showZoneLayer, (val) => {
  zoneLayer?.setVisible(val)
  if (val) loadZones()
  else zoneSource?.clear()
})

watch(zoneTypeFilter, () => {
  if (showZoneLayer.value) loadZones()
})

// ── Export functions ──
function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function handleExportGml() {
  try {
    loading.value = true
    const params = deviceTypeFilter.value ? { deviceType: deviceTypeFilter.value } : undefined
    const res = await exportGisGml(params)
    downloadBlob(new Blob([res.data]), 'streetlight_export.gml')
    ElMessage.success(t('gis.exportSuccess'))
  } catch {
    ElMessage.error(t('gis.exportFailed'))
  } finally {
    loading.value = false
  }
}

async function handleExportCsv() {
  try {
    loading.value = true
    const params = deviceTypeFilter.value ? { deviceType: deviceTypeFilter.value } : undefined
    const res = await exportGisOpenData(params)
    downloadBlob(new Blob([res.data]), 'streetlight_opendata.csv')
    ElMessage.success(t('gis.exportSuccess'))
  } catch {
    ElMessage.error(t('gis.exportFailed'))
  } finally {
    loading.value = false
  }
}

// ── Import functions ──
async function handleImportFile(file: File) {
  importLoading.value = true
  try {
    const res = await importGisGmlPreview(file)
    if (res.errorCode === '00000') {
      importDiff.value = res.body
      showImportDialog.value = true
    } else {
      ElMessage.error(res.errorMsg || t('gis.importParseFailed'))
    }
  } catch {
    ElMessage.error(t('gis.importParseFailed'))
  } finally {
    importLoading.value = false
  }
}

async function handleImportConfirm() {
  if (!importDiff.value) return
  importLoading.value = true
  try {
    const res = await importGisGmlConfirm(importDiff.value)
    if (res.errorCode === '00000') {
      ElMessage.success(t('gis.importSuccess', { count: res.body }))
      showImportDialog.value = false
      importDiff.value = null
      loadDevicesByBounds()
    } else {
      ElMessage.error(res.errorMsg || t('gis.importFailed'))
    }
  } catch {
    ElMessage.error(t('gis.importFailed'))
  } finally {
    importLoading.value = false
  }
}

onMounted(() => {
  initMap()
  loadDevicesByBounds()
})

onUnmounted(() => {
  if (moveendTimer) clearTimeout(moveendTimer)
  map?.setTarget(undefined)
  map = null
})
</script>

<template>
  <div class="gis-map-page">
    <!-- Toolbar row 1 -->
    <div class="gis-toolbar">
      <el-select
        v-model="deviceTypeFilter"
        :placeholder="t('gis.allDeviceTypes')"
        clearable
        style="width: 180px"
      >
        <el-option value="STREETLIGHT" :label="t('gis.streetlight')" />
        <el-option value="POLE" :label="t('gis.pole')" />
        <el-option value="CONTROLLER" :label="t('gis.controller')" />
        <el-option value="LUMINAIRE" :label="t('gis.luminaire')" />
      </el-select>

      <el-divider direction="vertical" />

      <!-- Zone layer toggle -->
      <el-checkbox v-model="showZoneLayer">{{ t('gis.zoneLayer') }}</el-checkbox>
      <el-select
        v-if="showZoneLayer"
        v-model="zoneTypeFilter"
        style="width: 140px"
        size="small"
      >
        <el-option value="ADMIN_DISTRICT" :label="t('gis.adminDistrict')" />
        <el-option value="SQUAD" :label="t('gis.squad')" />
        <el-option value="TAIPOWER" :label="t('gis.taipower')" />
        <el-option value="VENDOR" :label="t('gis.vendor')" />
      </el-select>

      <el-divider direction="vertical" />

      <!-- Nearby search -->
      <el-button
        :type="nearbyMode ? 'primary' : 'default'"
        @click="toggleNearbyMode"
        size="default"
      >
        {{ t('gis.nearbySearch') }}
      </el-button>
      <el-input-number
        v-if="nearbyMode"
        v-model="nearbyRadius"
        :min="100"
        :max="5000"
        :step="100"
        size="small"
        style="width: 130px"
      />
      <span v-if="nearbyMode" class="toolbar-hint">{{ t('gis.nearbyHint') }}</span>

      <el-divider direction="vertical" />

      <el-button @click="loadDevicesByBounds" :loading="loading">
        {{ t('gis.refresh') }}
      </el-button>
      <el-button @click="zoomToAll" :disabled="deviceCount === 0">
        {{ t('gis.zoomToAll') }}
      </el-button>

      <el-divider direction="vertical" />

      <!-- Export -->
      <el-dropdown @command="(cmd: string) => cmd === 'gml' ? handleExportGml() : handleExportCsv()">
        <el-button>
          {{ t('gis.export') }} <el-icon class="el-icon--right"><span>▼</span></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="gml">{{ t('gis.exportGml') }}</el-dropdown-item>
            <el-dropdown-item command="csv">{{ t('gis.exportOpenData') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <!-- Import -->
      <el-upload
        :show-file-list="false"
        accept=".gml,.xml"
        :before-upload="(file: File) => { handleImportFile(file); return false }"
      >
        <el-button :loading="importLoading">{{ t('gis.importGml') }}</el-button>
      </el-upload>

      <el-tooltip :content="t('gis.boxSelectHint')" placement="bottom">
        <span class="toolbar-hint">⌘/Ctrl + {{ t('gis.dragToSelect') }}</span>
      </el-tooltip>

      <span class="device-count">
        {{ t('gis.deviceCount', { count: deviceCount }) }}
      </span>
    </div>

    <!-- Map container -->
    <div ref="mapContainer" class="map-container" />

    <!-- Popup overlay -->
    <div ref="popupEl" class="ol-popup" v-show="selectedDevice">
      <a class="ol-popup-closer" @click.prevent="closePopup">&times;</a>
      <div v-if="selectedDevice" class="popup-content">
        <h4>{{ selectedDevice.deviceName || selectedDevice.deviceCode }}</h4>
        <p><strong>{{ t('gis.code') }}:</strong> {{ selectedDevice.deviceCode }}</p>
        <p><strong>{{ t('gis.type') }}:</strong> {{ selectedDevice.deviceType }}</p>
        <p v-if="selectedDevice.status"><strong>{{ t('gis.status') }}:</strong> {{ selectedDevice.status }}</p>
        <p v-if="selectedDevice.lng" class="popup-coords">
          <strong>WGS84:</strong> {{ selectedDevice.lng }}, {{ selectedDevice.lat }}
        </p>
        <a v-if="selectedDevice.lat && selectedDevice.lng"
           :href="`https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=${selectedDevice.lat},${selectedDevice.lng}`"
           target="_blank" rel="noopener noreferrer" class="street-view-link">
          🛣️ {{ t('gis.streetView') }}
        </a>
      </div>
    </div>

    <!-- Legend -->
    <div class="map-legend">
      <div class="legend-title">{{ t('gis.legend') }}</div>
      <div class="legend-item" v-for="(color, type) in DEVICE_COLORS" :key="type">
        <span v-if="type !== 'default'" class="legend-dot" :style="{ backgroundColor: color }" />
        <span v-if="type !== 'default'">{{ t(`gis.${type.toLowerCase()}`) }}</span>
      </div>
    </div>

    <!-- Loading indicator -->
    <div v-if="loading" class="map-loading">
      <el-icon class="is-loading" :size="24"><span>⟳</span></el-icon>
    </div>

    <!-- Bottom panel: query results -->
    <Transition name="slide-up">
      <div v-if="showBottomPanel" class="bottom-panel">
        <div class="bottom-panel-header">
          <span>{{ bottomPanelTitle }}</span>
          <el-button size="small" text @click="showBottomPanel = false; boxSelectResults = []">✕</el-button>
        </div>
        <div class="bottom-panel-body">
          <el-table :data="boxSelectResults" max-height="220" size="small" highlight-current-row
                    @row-click="flyToDevice" style="width: 100%">
            <el-table-column prop="deviceCode" :label="t('gis.code')" width="140" />
            <el-table-column prop="deviceName" :label="t('gis.deviceName')" min-width="140" />
            <el-table-column prop="deviceType" :label="t('gis.type')" width="120" />
            <el-table-column prop="status" :label="t('gis.status')" width="100" />
          </el-table>
        </div>
      </div>
    </Transition>

    <!-- Import diff dialog -->
    <el-dialog
      v-model="showImportDialog"
      :title="t('gis.importDiff')"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="importDiff" class="import-diff">
        <p>{{ t('gis.importTotalParsed', { count: importDiff.totalParsed }) }}</p>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item :label="t('gis.addCount')">
            <el-tag type="success">{{ importDiff.toAdd.length }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('gis.updateCount')">
            <el-tag type="warning">{{ importDiff.toUpdate.length }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('gis.deleteCount')">
            <el-tag type="danger">{{ importDiff.toDelete.length }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-tabs v-if="importDiff.toAdd.length || importDiff.toUpdate.length || importDiff.toDelete.length" class="import-tabs">
          <el-tab-pane v-if="importDiff.toAdd.length" :label="`${t('gis.addCount')} (${importDiff.toAdd.length})`">
            <el-table :data="importDiff.toAdd" max-height="200" size="small">
              <el-table-column prop="deviceCode" :label="t('gis.code')" width="140" />
              <el-table-column prop="deviceName" :label="t('gis.deviceName')" min-width="140" />
              <el-table-column prop="deviceType" :label="t('gis.type')" width="120" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane v-if="importDiff.toUpdate.length" :label="`${t('gis.updateCount')} (${importDiff.toUpdate.length})`">
            <el-table :data="importDiff.toUpdate" max-height="200" size="small">
              <el-table-column prop="deviceCode" :label="t('gis.code')" width="140" />
              <el-table-column prop="deviceName" :label="t('gis.deviceName')" min-width="140" />
              <el-table-column prop="diffFields" :label="t('gis.changedFields')" width="180" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane v-if="importDiff.toDelete.length" :label="`${t('gis.deleteCount')} (${importDiff.toDelete.length})`">
            <el-table :data="importDiff.toDelete" max-height="200" size="small">
              <el-table-column prop="deviceCode" :label="t('gis.code')" width="140" />
              <el-table-column prop="deviceName" :label="t('gis.deviceName')" min-width="140" />
              <el-table-column prop="deviceType" :label="t('gis.type')" width="120" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="showImportDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImportConfirm"
                   :disabled="!importDiff || (!importDiff.toAdd.length && !importDiff.toUpdate.length)">
          {{ t('gis.importConfirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.gis-map-page {
  position: relative;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
}

.gis-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.toolbar-hint {
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.device-count {
  margin-left: auto;
  color: #909399;
  font-size: 14px;
}

.map-container {
  flex: 1;
  width: 100%;
}

.ol-popup {
  position: absolute;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: 16px;
  border-radius: 8px;
  min-width: 200px;
  max-width: 300px;
}

.ol-popup-closer {
  position: absolute;
  top: 4px;
  right: 8px;
  font-size: 18px;
  cursor: pointer;
  color: #909399;
  text-decoration: none;
}

.popup-content h4 {
  margin: 0 0 8px;
  font-size: 15px;
  color: #303133;
}

.popup-content p {
  margin: 4px 0;
  font-size: 13px;
  color: #606266;
}

.popup-coords {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
}

.street-view-link {
  display: inline-block;
  margin-top: 8px;
  padding: 4px 10px;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 4px;
  color: #67c23a;
  font-size: 13px;
  text-decoration: none;
  cursor: pointer;
}

.street-view-link:hover {
  background: #e1f3d8;
  color: #529b2e;
}

.map-legend {
  position: absolute;
  bottom: 24px;
  right: 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 6px;
  padding: 10px 14px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.legend-title {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 6px;
  color: #303133;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #606266;
  margin: 3px 0;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1.5px solid #fff;
  box-shadow: 0 0 2px rgba(0, 0, 0, 0.3);
}

.map-loading {
  position: absolute;
  top: 70px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.9);
  padding: 8px 16px;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 15;
  font-size: 14px;
}

/* Bottom panel */
.bottom-panel {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 2px solid #409eff;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.1);
  z-index: 20;
  max-height: 300px;
}

.bottom-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #e4e7ed;
}

.bottom-panel-body {
  overflow: auto;
  max-height: 240px;
}

.bottom-panel-body :deep(.el-table__row) {
  cursor: pointer;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.25s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}

.import-diff p {
  margin: 0 0 12px;
  color: #606266;
}

.import-tabs {
  margin-top: 16px;
}
</style>
