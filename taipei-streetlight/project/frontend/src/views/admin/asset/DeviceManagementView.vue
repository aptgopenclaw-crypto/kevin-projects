<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useDeptStore } from '@/stores/deptStore'
import {
  getDevices,
  createDevice,
  updateDevice,
  decommissionDevice,
  getComponents,
  replaceComponent,
  exportDevices,
  getDeviceSchema,
} from '@/api/device'
import type { DeviceSchema } from '@/api/device'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type {
  DeviceResponse,
  DeviceRequest,
  DeviceType,
  DeviceStatus,
  ComponentReplaceRequest,
} from '@/types/device'
import {
  Search, Plus, Pencil, Ban, Lightbulb, Cpu, UtilityPole,
  RefreshCw, Eye, ArrowRightLeft, Download,
} from 'lucide-vue-next'
import DeptTreeSelector from '@/components/DeptTreeSelector.vue'
import DynamicFieldEditor from '@/components/DynamicFieldEditor.vue'
import {
  wgs84ToTwd97, wgs84ToTwd67,
  twd97ToWgs84, twd97ToTwd67,
  twd67ToWgs84, twd67ToTwd97,
} from '@/utils/coordinateUtils'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const deptStore = useDeptStore()

// ── Filter state ──
const filterType = ref<DeviceType | ''>('POLE')
const filterStatus = ref<DeviceStatus | ''>('')
const keyword = ref('')
const pageSize = ref(20)
const loading = ref(false)

// ── Table data ──
const devices = ref<DeviceResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// ── Expand row: lazy-loaded children ──
const expandedComponents = ref<Record<number, DeviceResponse[]>>({})
const expandLoading = ref<Record<number, boolean>>({})

// ── Detail drawer ──
const drawerVisible = ref(false)
const drawerDevice = ref<DeviceResponse | null>(null)

// ── Create/Edit dialog ──
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const dialogLoading = ref(false)
const dialogFormRef = ref<FormInstance>()
const dialogForm = ref<DeviceRequest>({
  deviceType: 'POLE',
  deviceCode: '',
  deviceName: '',
  deptId: null,
})
const dialogEditId = ref<number | null>(null)

// ── Dynamic schema for dialog ──
const dialogSchema = ref<DeviceSchema | null>(null)

watch(() => dialogForm.value.deviceType, async (type) => {
  if (!type) { dialogSchema.value = null; return }
  try {
    const res = await getDeviceSchema(type)
    dialogSchema.value = res.data ?? null
  } catch {
    dialogSchema.value = null
  }
}, { immediate: true })

const dialogRules = computed<FormRules>(() => ({
  deviceType: [{ required: true, message: t('device.errors.typeRequired'), trigger: 'change' }],
  deviceCode: [{ required: true, message: t('device.errors.codeRequired'), trigger: 'blur' }],
}))

// ── Replace dialog ──
const replaceVisible = ref(false)
const replaceLoading = ref(false)
const replaceFormRef = ref<FormInstance>()
const replacePoleId = ref<number | null>(null)
const replaceOldDevice = ref<DeviceResponse | null>(null)
const replaceForm = ref({
  deviceType: 'LUMINAIRE' as DeviceType,
  deviceCode: '',
  deviceName: '',
  reason: '',
  attributes: {} as Record<string, unknown>,
})

const replaceRules = computed<FormRules>(() => ({
  deviceCode: [{ required: true, message: t('device.errors.codeRequired'), trigger: 'blur' }],
}))

// ── Coordinate auto-conversion ──
const coordSource = ref<'wgs84' | 'twd97' | 'twd67'>('wgs84')

function onWgs84Change() {
  if (coordSource.value !== 'wgs84') return
  const { lng, lat } = dialogForm.value
  if (lng == null || lat == null) return
  try {
    const twd97 = wgs84ToTwd97(lng, lat)
    dialogForm.value.twd97X = twd97.x
    dialogForm.value.twd97Y = twd97.y
    const twd67 = wgs84ToTwd67(lng, lat)
    dialogForm.value.twd67X = twd67.x
    dialogForm.value.twd67Y = twd67.y
  } catch { /* ignore conversion errors */ }
}

function onTwd97Change() {
  if (coordSource.value !== 'twd97') return
  const { twd97X, twd97Y } = dialogForm.value
  if (twd97X == null || twd97Y == null) return
  try {
    const wgs84 = twd97ToWgs84(twd97X, twd97Y)
    dialogForm.value.lng = wgs84.lng
    dialogForm.value.lat = wgs84.lat
    const twd67 = twd97ToTwd67(twd97X, twd97Y)
    dialogForm.value.twd67X = twd67.x
    dialogForm.value.twd67Y = twd67.y
  } catch { /* ignore */ }
}

function onTwd67Change() {
  if (coordSource.value !== 'twd67') return
  const { twd67X, twd67Y } = dialogForm.value
  if (twd67X == null || twd67Y == null) return
  try {
    const wgs84 = twd67ToWgs84(twd67X, twd67Y)
    dialogForm.value.lng = wgs84.lng
    dialogForm.value.lat = wgs84.lat
    const twd97 = twd67ToTwd97(twd67X, twd67Y)
    dialogForm.value.twd97X = twd97.x
    dialogForm.value.twd97Y = twd97.y
  } catch { /* ignore */ }
}

const deviceTypeOptions: { value: DeviceType; label: string }[] = [
  { value: 'POLE', label: '燈桿' },
  { value: 'LUMINAIRE', label: '燈具' },
  { value: 'CONTROLLER', label: '控制器' },
  { value: 'PANEL_BOX', label: '分電箱' },
  { value: 'POWER_EQUIPMENT', label: '電力設備' },
  { value: 'ATTACHMENT', label: '附掛物' },
]

const statusOptions: { value: DeviceStatus; label: string }[] = [
  { value: 'ACTIVE', label: '啟用' },
  { value: 'REPORTED', label: '已報修' },
  { value: 'UNDER_REPAIR', label: '維修中' },
  { value: 'INACTIVE', label: '停用' },
  { value: 'DECOMMISSIONED', label: '已除役' },
]

function getDeviceTypeLabel(type: DeviceType) {
  return deviceTypeOptions.find(o => o.value === type)?.label ?? type
}

function getStatusLabel(status: DeviceStatus) {
  return statusOptions.find(o => o.value === status)?.label ?? status
}

function getStatusType(status: DeviceStatus) {
  const map: Record<DeviceStatus, string> = {
    ACTIVE: 'success',
    REPORTED: 'warning',
    UNDER_REPAIR: 'warning',
    INACTIVE: 'danger',
    DECOMMISSIONED: 'info',
  }
  return map[status] ?? ''
}

function getDeviceTypeIcon(type: DeviceType) {
  const map: Record<string, typeof UtilityPole> = {
    POLE: UtilityPole,
    LUMINAIRE: Lightbulb,
    CONTROLLER: Cpu,
  }
  return map[type] ?? UtilityPole
}

// ── Data loading ──
async function loadDevices(page = 0) {
  loading.value = true
  try {
    const res = await getDevices({
      deviceType: filterType.value || undefined,
      status: filterStatus.value || undefined,
      keyword: keyword.value || undefined,
      page,
      size: pageSize.value,
    })
    devices.value = res.body.content
    pagination.value = {
      page: res.body.page,
      totalElements: res.body.totalElements,
      totalPages: res.body.totalPages,
    }
  } catch {
    ElMessage.error(t('device.loadFailed'))
  } finally {
    loading.value = false
    expandedComponents.value = {}
  }
}

function handleSearch() { loadDevices(0) }
function handlePageChange(page: number) { loadDevices(page - 1) }
function handleSizeChange(size: number) {
  pageSize.value = size
  loadDevices(0)
}

// ── Expand row: load children ──
async function handleExpandChange(row: DeviceResponse, expanded: boolean) {
  if (!expanded) return
  if (expandedComponents.value[row.id]) return // already loaded
  if (row.deviceType !== 'POLE' && row.deviceType !== 'PANEL_BOX') return
  expandLoading.value[row.id] = true
  try {
    const res = await getComponents(row.id)
    expandedComponents.value[row.id] = res.body
  } catch {
    expandedComponents.value[row.id] = []
  } finally {
    expandLoading.value[row.id] = false
  }
}

// ── Export ──
const exporting = ref(false)

async function handleExport(format: 'csv' | 'xlsx' | 'ods') {
  exporting.value = true
  try {
    const res = await exportDevices({
      format,
      deviceType: filterType.value || undefined,
      status: filterStatus.value || undefined,
      keyword: keyword.value || undefined,
    })
    const blob = new Blob([res as unknown as BlobPart])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `devices.${format}`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success(t('device.exportSuccess'))
  } catch {
    ElMessage.error(t('device.exportFailed'))
  } finally {
    exporting.value = false
  }
}

// ── Detail drawer ──
function openDrawer(row: DeviceResponse) {
  drawerDevice.value = row
  drawerVisible.value = true
}

// ── Create/Edit ──
function openCreateDialog(parentId?: number) {
  dialogMode.value = 'create'
  dialogEditId.value = null
  dialogForm.value = {
    deviceType: 'POLE',
    deviceCode: '',
    deviceName: '',
    parentDeviceId: parentId,
  }
  dialogVisible.value = true
}

function openEditDialog(device: DeviceResponse) {
  dialogMode.value = 'edit'
  dialogEditId.value = device.id
  dialogForm.value = {
    deviceType: device.deviceType,
    deviceCode: device.deviceCode,
    deviceName: device.deviceName ?? '',
    twd97X: device.twd97X ?? undefined,
    twd97Y: device.twd97Y ?? undefined,
    lng: device.lng ?? undefined,
    lat: device.lat ?? undefined,
    elevation: device.elevation ?? undefined,
    twd67X: device.twd67X ?? undefined,
    twd67Y: device.twd67Y ?? undefined,
    taipowerCoord: device.taipowerCoord ?? undefined,
    deptId: device.deptId ?? null,
    contractId: device.contractId ?? undefined,
    propertyOwner: device.propertyOwner ?? undefined,
    installedAt: device.installedAt ?? undefined,
    parentDeviceId: device.parentDeviceId ?? undefined,
    mountPosition: device.mountPosition ?? undefined,
    connectivityType: device.connectivityType ?? undefined,
    circuitId: device.circuitId ?? undefined,
    attributes: device.attributes ?? undefined,
  }
  dialogVisible.value = true
}

async function handleDialogSubmit() {
  const valid = await dialogFormRef.value?.validate().catch(() => false)
  if (!valid) return
  dialogLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createDevice(dialogForm.value)
      ElMessage.success(t('device.createdSuccess'))
    } else {
      await updateDevice(dialogEditId.value!, dialogForm.value)
      ElMessage.success(t('device.updatedSuccess'))
    }
    dialogVisible.value = false
    await loadDevices(pagination.value.page)
  } catch (err: unknown) {
    const error = err as { response?: { data?: { errorCode?: string } } }
    const code = error?.response?.data?.errorCode
    const msg = code ? (t(`device.errors.${code}`, code) ) : t('common.operationFailed')
    ElMessage.error(msg)
  } finally {
    dialogLoading.value = false
  }
}

// ── Delete / Decommission ──
async function handleDecommission(row: DeviceResponse) {
  try {
    await ElMessageBox.confirm(
      t('device.decommissionConfirm', { code: row.deviceCode }),
      t('device.decommissionTitle'),
      { confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'), type: 'warning' },
    )
  } catch { return }

  try {
    await decommissionDevice(row.id)
    ElMessage.success(t('device.decommissionedSuccess'))
    await loadDevices(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  }
}

// ── Replace component ──
function openReplaceDialog(poleId: number, oldDevice: DeviceResponse) {
  replacePoleId.value = poleId
  replaceOldDevice.value = oldDevice
  replaceForm.value = {
    deviceType: oldDevice.deviceType,
    deviceCode: '',
    deviceName: '',
    reason: '',
    attributes: {},
  }
  replaceVisible.value = true
}

async function handleReplaceSubmit() {
  const valid = await replaceFormRef.value?.validate().catch(() => false)
  if (!valid) return
  replaceLoading.value = true
  try {
    const payload: ComponentReplaceRequest = {
      oldDeviceId: replaceOldDevice.value!.id,
      newDevice: {
        deviceType: replaceForm.value.deviceType,
        deviceCode: replaceForm.value.deviceCode,
        deviceName: replaceForm.value.deviceName || undefined,
      },
      reason: replaceForm.value.reason || undefined,
    }
    await replaceComponent(replacePoleId.value!, payload)
    ElMessage.success(t('device.replaceSuccess'))
    replaceVisible.value = false
    // Refresh expand cache for this pole
    if (replacePoleId.value) {
      delete expandedComponents.value[replacePoleId.value]
      try {
        const res = await getComponents(replacePoleId.value)
        expandedComponents.value[replacePoleId.value] = res.body
      } catch { /* silent */ }
    }
    await loadDevices(pagination.value.page)
  } catch (err: unknown) {
    const error = err as { response?: { data?: { errorCode?: string } } }
    const code = error?.response?.data?.errorCode
    const msg = code ? t(`device.errors.${code}`, code) : t('common.operationFailed')
    ElMessage.error(msg)
  } finally {
    replaceLoading.value = false
  }
}

onMounted(async () => {
  await loadDevices()
  if (!deptStore.initialized) {
    await deptStore.fetchDeptOptions()
  }
})
</script>

<template>
  <div class="page-container">
    <div class="page-content">
      <!-- Header -->
      <div class="page-header">
        <div>
          <h1 class="page-title">{{ t('device.title') }}</h1>
          <p class="page-subtitle">{{ t('device.subtitle') }}</p>
        </div>
        <div class="header-actions">
          <el-dropdown @command="handleExport" :disabled="exporting">
            <el-button class="export-btn" :loading="exporting">
              <Download :size="16" style="margin-right: 6px" />
              {{ t('device.exportBtn') }}
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="csv">CSV</el-dropdown-item>
                <el-dropdown-item command="xlsx">XLSX (Excel)</el-dropdown-item>
                <el-dropdown-item command="ods">ODS (LibreOffice)</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button class="create-btn" @click="openCreateDialog()">
            <Plus :size="16" style="margin-right: 6px" />
            {{ t('device.addBtn') }}
          </el-button>
        </div>
      </div>

      <!-- Filter bar -->
      <div class="filter-bar">
        <el-select v-model="filterType" :placeholder="t('device.filterType')" clearable style="width: 140px" @change="handleSearch">
          <el-option v-for="opt in deviceTypeOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
        </el-select>
        <el-select v-model="filterStatus" :placeholder="t('device.filterStatus')" clearable style="width: 120px" @change="handleSearch">
          <el-option v-for="opt in statusOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
        </el-select>
        <el-input v-model="keyword" :placeholder="t('device.searchPlaceholder')" clearable style="width: 240px"
                  @keyup.enter="handleSearch" @clear="handleSearch">
          <template #prefix><Search :size="16" class="input-icon" /></template>
        </el-input>
        <el-button class="search-btn" @click="handleSearch">{{ t('common.search') }}</el-button>
      </div>

      <!-- Table -->
      <div class="table-card" v-loading="loading">
        <el-table :data="devices" style="width: 100%" row-key="id" @expand-change="handleExpandChange">
          <!-- Expand column: show mounted components -->
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="expand-content" v-if="row.deviceType === 'POLE' || row.deviceType === 'PANEL_BOX'">
                <div v-if="expandLoading[row.id]" class="expand-loading">
                  <el-icon class="is-loading"><RefreshCw :size="14" /></el-icon>
                  {{ t('common.loading') }}
                </div>
                <div v-else-if="!expandedComponents[row.id] || expandedComponents[row.id].length === 0" class="expand-empty">
                  {{ t('device.noComponents') }}
                </div>
                <nav v-else class="tree-nav">
                  <div v-for="(child, idx) in expandedComponents[row.id]" :key="child.id" class="tree-item" :class="{ 'tree-item--last': idx === expandedComponents[row.id].length - 1 }">
                    <div class="tree-branch">
                      <span class="tree-vline"></span>
                      <span class="tree-hline"></span>
                    </div>
                    <div class="tree-node" :class="'tree-node--' + child.deviceType.toLowerCase()">
                      <div class="tree-node-left">
                        <component :is="getDeviceTypeIcon(child.deviceType)" :size="16" class="tree-node-icon" />
                        <span class="tree-node-code">{{ child.deviceCode }}</span>
                        <span class="type-badge small" :class="'type-' + child.deviceType">{{ getDeviceTypeLabel(child.deviceType) }}</span>
                        <span v-if="child.mountPosition" class="mount-tag">{{ child.mountPosition }}</span>
                        <span class="status-badge" :class="'status-' + getStatusType(child.status)">{{ getStatusLabel(child.status) }}</span>
                      </div>
                      <div class="tree-node-actions">
                        <el-button class="action-btn" size="small" @click="openDrawer(child)" :title="t('device.viewDetail')">
                          <Eye :size="14" />
                        </el-button>
                        <el-button class="action-btn" size="small" @click="openEditDialog(child)">
                          <Pencil :size="14" />
                        </el-button>
                        <el-button
                          v-if="child.status !== 'DECOMMISSIONED'"
                          class="action-btn action-btn-warn" size="small"
                          @click="handleDecommission(child)">
                          <Ban :size="14" />
                        </el-button>
                        <el-button
                          v-if="child.status !== 'DECOMMISSIONED'"
                          class="replace-btn" size="small"
                          @click="openReplaceDialog(row.id, child)"
                          :title="t('device.replaceBtn')">
                          <ArrowRightLeft :size="13" />
                        </el-button>
                      </div>
                    </div>
                  </div>
                </nav>
              </div>
              <div v-else class="expand-empty">{{ t('device.notComposite') }}</div>
            </template>
          </el-table-column>

          <el-table-column prop="deviceCode" :label="t('device.colCode')" min-width="160">
            <template #default="{ row }">
              <div class="code-cell" @click="openDrawer(row)">
                <component :is="getDeviceTypeIcon(row.deviceType)" :size="16" class="type-icon" />
                <span class="code-link">{{ row.deviceCode }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="deviceName" :label="t('device.colName')" min-width="160" />
          <el-table-column prop="deviceType" :label="t('device.colType')" width="100" align="center">
            <template #default="{ row }">
              <span class="type-badge" :class="'type-' + row.deviceType">
                {{ getDeviceTypeLabel(row.deviceType) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column :label="t('device.colDept')" min-width="140">
            <template #default="{ row }">
              {{ deptStore.getDeptName(row.deptId) || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="t('device.colStatus')" width="100" align="center">
            <template #default="{ row }">
              <span class="status-badge" :class="'status-' + getStatusType(row.status)">
                {{ getStatusLabel(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column :label="t('device.colChildren')" width="80" align="center">
            <template #default="{ row }">
              <span v-if="row.childrenCount > 0" class="children-count">{{ row.childrenCount }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.actions')" width="130" fixed="right">
            <template #default="{ row }">
              <div class="action-group">
                <el-button class="action-btn" size="small" @click="openDrawer(row)" :title="t('device.viewDetail')">
                  <Eye :size="14" />
                </el-button>
                <el-button class="action-btn" size="small" @click="openEditDialog(row)">
                  <Pencil :size="14" />
                </el-button>
                <el-button
                  v-if="row.status !== 'DECOMMISSIONED'"
                  class="action-btn action-btn-warn" size="small" @click="handleDecommission(row)">
                  <Ban :size="14" />
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row" v-if="pagination.totalElements > 0">
          <el-pagination
            background layout="total, sizes, prev, pager, next"
            :total="pagination.totalElements"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :current-page="pagination.page + 1"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>

    <!-- ── Detail Drawer ── -->
    <el-drawer v-model="drawerVisible" :title="drawerDevice?.deviceCode ?? ''" size="520px" class="device-drawer">
      <template v-if="drawerDevice">
        <div class="drawer-body">
          <!-- Basic info -->
          <div class="info-section">
            <h3 class="section-title">{{ t('device.basicInfo') }}</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">{{ t('device.colType') }}</span>
                <span class="type-badge" :class="'type-' + drawerDevice.deviceType">
                  {{ getDeviceTypeLabel(drawerDevice.deviceType) }}
                </span>
              </div>
              <div class="info-item">
                <span class="info-label">{{ t('device.colStatus') }}</span>
                <span class="status-badge" :class="'status-' + getStatusType(drawerDevice.status)">
                  {{ getStatusLabel(drawerDevice.status) }}
                </span>
              </div>
              <div class="info-item">
                <span class="info-label">{{ t('device.colName') }}</span>
                <span>{{ drawerDevice.deviceName || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">{{ t('device.colDept') }}</span>
                <span>{{ deptStore.getDeptName(drawerDevice.deptId) || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">{{ t('device.installedAt') }}</span>
                <span>{{ drawerDevice.installedAt || '-' }}</span>
              </div>
              <div class="info-item" v-if="drawerDevice.lng && drawerDevice.lat">
                <span class="info-label">WGS84</span>
                <span class="mono-text">{{ drawerDevice.lng }}, {{ drawerDevice.lat }}</span>
              </div>
              <div class="info-item" v-if="drawerDevice.twd97X && drawerDevice.twd97Y">
                <span class="info-label">TWD97</span>
                <span class="mono-text">{{ drawerDevice.twd97X }}, {{ drawerDevice.twd97Y }}</span>
              </div>
              <div class="info-item" v-if="drawerDevice.twd67X && drawerDevice.twd67Y">
                <span class="info-label">TWD67</span>
                <span class="mono-text">{{ drawerDevice.twd67X }}, {{ drawerDevice.twd67Y }}</span>
              </div>
              <div class="info-item" v-if="drawerDevice.lng && drawerDevice.lat">
                <span class="info-label">{{ t('gis.streetView') }}</span>
                <a :href="`https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=${drawerDevice.lat},${drawerDevice.lng}`"
                   target="_blank" rel="noopener noreferrer" class="street-view-link">
                  🛣️ {{ t('gis.openStreetView') }}
                </a>
              </div>
            </div>
          </div>

          <!-- JSONB attributes -->
          <div class="info-section" v-if="drawerDevice.attributes && Object.keys(drawerDevice.attributes).length">
            <h3 class="section-title">{{ t('device.attributes') }}</h3>
            <div class="attr-grid">
              <div v-for="(val, key) in drawerDevice.attributes" :key="String(key)" class="attr-item">
                <span class="attr-key">{{ key }}</span>
                <span class="attr-val">{{ val }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- ── Create/Edit Dialog ── -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? t('device.dialogCreateTitle') : t('device.dialogEditTitle')"
      width="560px" :close-on-click-modal="false" class="dark-dialog"
    >
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-position="top"
               @submit.prevent="handleDialogSubmit">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px;">
          <el-form-item :label="t('device.colType')" prop="deviceType">
            <el-select v-model="dialogForm.deviceType" style="width: 100%">
              <el-option v-for="opt in deviceTypeOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('device.colCode')" prop="deviceCode">
            <el-input v-model="dialogForm.deviceCode" placeholder="SL-N-001" />
          </el-form-item>
          <el-form-item :label="t('device.colName')" style="grid-column: 1 / -1">
            <el-input v-model="dialogForm.deviceName" :placeholder="t('device.namePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('device.colDept')">
            <DeptTreeSelector :model-value="dialogForm.deptId ?? null" @update:model-value="v => dialogForm.deptId = v" :placeholder="t('device.deptPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('device.mountPosition')">
            <el-input v-model="dialogForm.mountPosition" placeholder="ARM_1" />
          </el-form-item>
        </div>

        <!-- 坐標資訊 -->
        <div class="coord-section">
          <div class="coord-section-title">{{ t('device.coordSection') }}</div>
          <div class="coord-grid">
            <div class="coord-group">
              <div class="coord-group-label">WGS84 (GPS)</div>
              <div style="display: flex; gap: 8px;">
                <el-form-item :label="t('device.lng')" style="flex: 1; margin-bottom: 8px;">
                  <el-input-number v-model="dialogForm.lng" :precision="7" :controls="false" style="width: 100%"
                    @focus="coordSource = 'wgs84'" @change="onWgs84Change" />
                </el-form-item>
                <el-form-item :label="t('device.lat')" style="flex: 1; margin-bottom: 8px;">
                  <el-input-number v-model="dialogForm.lat" :precision="7" :controls="false" style="width: 100%"
                    @focus="coordSource = 'wgs84'" @change="onWgs84Change" />
                </el-form-item>
              </div>
            </div>
            <div class="coord-group">
              <div class="coord-group-label">TWD97 {{ t('device.twd97Label') }}</div>
              <div style="display: flex; gap: 8px;">
                <el-form-item label="X" style="flex: 1; margin-bottom: 8px;">
                  <el-input-number v-model="dialogForm.twd97X" :precision="3" :controls="false" style="width: 100%"
                    @focus="coordSource = 'twd97'" @change="onTwd97Change" />
                </el-form-item>
                <el-form-item label="Y" style="flex: 1; margin-bottom: 8px;">
                  <el-input-number v-model="dialogForm.twd97Y" :precision="3" :controls="false" style="width: 100%"
                    @focus="coordSource = 'twd97'" @change="onTwd97Change" />
                </el-form-item>
              </div>
            </div>
            <div class="coord-group">
              <div class="coord-group-label">TWD67 {{ t('device.twd67Label') }}</div>
              <div style="display: flex; gap: 8px;">
                <el-form-item label="X" style="flex: 1; margin-bottom: 8px;">
                  <el-input-number v-model="dialogForm.twd67X" :precision="3" :controls="false" style="width: 100%"
                    @focus="coordSource = 'twd67'" @change="onTwd67Change" />
                </el-form-item>
                <el-form-item label="Y" style="flex: 1; margin-bottom: 8px;">
                  <el-input-number v-model="dialogForm.twd67Y" :precision="3" :controls="false" style="width: 100%"
                    @focus="coordSource = 'twd67'" @change="onTwd67Change" />
                </el-form-item>
              </div>
            </div>
            <div style="display: flex; gap: 8px;">
              <el-form-item :label="t('device.taipowerCoord')" style="flex: 1; margin-bottom: 8px;">
                <el-input v-model="dialogForm.taipowerCoord" :placeholder="t('device.taipowerCoordPlaceholder')" />
              </el-form-item>
              <el-form-item :label="t('device.elevation')" style="flex: 1; margin-bottom: 8px;">
                <el-input-number v-model="dialogForm.elevation" :precision="3" :controls="false" style="width: 100%" />
              </el-form-item>
            </div>
          </div>
        </div>

        <!-- Dynamic attributes from schema -->
        <DynamicFieldEditor
          v-if="dialogSchema"
          :schema="dialogSchema"
          :model-value="dialogForm.attributes ?? {}"
          @update:model-value="v => dialogForm.attributes = v"
        />
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button class="submit-btn" :loading="dialogLoading" @click="handleDialogSubmit">
          {{ dialogMode === 'create' ? t('common.add') : t('common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ── Replace Component Dialog ── -->
    <el-dialog v-model="replaceVisible" :title="t('device.replaceTitle')" width="500px" :close-on-click-modal="false" class="dark-dialog">
      <div v-if="replaceOldDevice" class="replace-old-info">
        <span class="replace-label">{{ t('device.replaceOld') }}</span>
        <div class="replace-old-card">
          <component :is="getDeviceTypeIcon(replaceOldDevice.deviceType)" :size="18" />
          <span>{{ replaceOldDevice.deviceCode }}</span>
          <span class="type-badge" :class="'type-' + replaceOldDevice.deviceType">
            {{ getDeviceTypeLabel(replaceOldDevice.deviceType) }}
          </span>
        </div>
        <RefreshCw :size="20" class="replace-arrow" />
      </div>

      <el-form ref="replaceFormRef" :model="replaceForm" :rules="replaceRules" label-position="top">
        <el-form-item :label="t('device.replaceNewCode')" prop="deviceCode">
          <el-input v-model="replaceForm.deviceCode" placeholder="LM-N-021" />
        </el-form-item>
        <el-form-item :label="t('device.colName')">
          <el-input v-model="replaceForm.deviceName" :placeholder="t('device.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('device.replaceReason')">
          <el-input v-model="replaceForm.reason" type="textarea" :rows="2" :placeholder="t('device.replaceReasonPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="replaceVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button class="submit-btn" :loading="replaceLoading" @click="handleReplaceSubmit">
          {{ t('device.replaceConfirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 32px 24px;
  min-height: 100vh;
  background-color: var(--bg-base);
}

.page-content {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-title {
  font-family: 'Inter', sans-serif;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.15;
  color: var(--text-heading);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.export-btn {
  background: transparent;
  color: var(--text-primary);
  border: 1px solid var(--border-light);
  border-radius: 86px;
  padding: 8px 20px;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.export-btn:hover {
  opacity: 0.6;
  color: var(--text-primary);
}

.create-btn {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none;
  border-radius: 86px;
  padding: 8px 24px;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.create-btn:hover {
  background: var(--btn-primary-hover);
  color: var(--btn-primary-text);
}

/* Filter bar */
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-btn {
  background: transparent;
  color: var(--text-primary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  padding: 8px 16px;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
}

.search-btn:hover { opacity: 0.6; }
.input-icon { color: var(--text-muted); }

/* Table */
.table-card {
  background-color: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.code-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.code-link {
  color: #55b3ff;
  font-weight: 600;
  font-size: 13px;
}

.code-link:hover { text-decoration: underline; }

.type-icon { color: var(--text-secondary); }

/* Type badge */
.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.type-POLE { background: rgba(85, 179, 255, 0.15); color: #55b3ff; }
.type-LUMINAIRE { background: rgba(255, 188, 51, 0.15); color: #ffbc33; }
.type-CONTROLLER { background: rgba(168, 131, 255, 0.15); color: #a883ff; }
.type-PANEL_BOX { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.type-POWER_EQUIPMENT { background: rgba(255, 150, 100, 0.15); color: #ff9664; }
.type-ATTACHMENT { background: rgba(150, 150, 150, 0.15); color: #999; }

/* Status badge */
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-warning { background: rgba(255, 188, 51, 0.15); color: #ffbc33; }
.status-danger { background: rgba(255, 99, 99, 0.15); color: #FF6363; }
.status-info { background: rgba(150, 150, 150, 0.15); color: #999; }

.children-count {
  font-weight: 600;
  color: #55b3ff;
}

.text-muted { color: var(--text-muted); }

/* ── Expand row: tree nav (menu-aligned) ── */
.expand-content {
  padding: 4px 16px 8px 56px;
}

.expand-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 13px;
  padding: 8px 0;
}

.expand-empty {
  color: var(--text-muted);
  font-size: 13px;
  padding: 8px 16px;
}

.tree-nav {
  display: flex;
  flex-direction: column;
  position: relative;
}

.tree-item {
  display: flex;
  align-items: stretch;
  min-height: 40px;
}

/* Branch connector lines */
.tree-branch {
  position: relative;
  width: 28px;
  flex-shrink: 0;
}

.tree-vline {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 1px;
  background: var(--border-medium, #333);
}

.tree-item--last .tree-vline {
  bottom: 50%;
}

.tree-hline {
  position: absolute;
  left: 0;
  top: 50%;
  width: 20px;
  height: 1px;
  background: var(--border-medium, #333);
}

/* Tree node (menu-item aligned) */
.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  border-radius: 6px;
  margin: 2px 0;
  border-left: 2px solid transparent;
  transition: background 150ms ease;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
}

.tree-node:hover {
  background: var(--bg-hover);
}

.tree-node--luminaire { border-left-color: #ffbc33; }
.tree-node--controller { border-left-color: #a883ff; }
.tree-node--panel_box { border-left-color: #5fc992; }
.tree-node--pole { border-left-color: #55b3ff; }

.tree-node-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.tree-node-icon {
  color: var(--text-secondary);
  flex-shrink: 0;
}

.tree-node-code {
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.2px;
  white-space: nowrap;
}

.type-badge.small {
  font-size: 10px;
  padding: 1px 6px;
}

.tree-node-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 150ms ease;
}

.tree-node:hover .tree-node-actions {
  opacity: 1;
}

/* Actions */
.action-group { display: flex; gap: 4px; }

.action-btn {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  padding: 4px 8px;
  min-width: auto;
}

.action-btn:hover { opacity: 0.6; color: var(--text-primary); }
.action-btn-warn { color: #ffbc33; border-color: rgba(255, 188, 51, 0.2); }
.action-btn-warn:hover { background: rgba(255, 188, 51, 0.15); color: #ffbc33; }
.action-btn-danger { color: #FF6363; border-color: rgba(255, 99, 99, 0.2); }
.action-btn-danger:hover { background: rgba(255, 99, 99, 0.15); color: #FF6363; }

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--bg-active);
}

/* ── Drawer ── */
.drawer-body { padding: 0 4px; }

.info-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-subtle);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.mono-text { font-family: 'JetBrains Mono', monospace; font-size: 13px; }

.street-view-link {
  color: #67c23a;
  font-size: 13px;
  text-decoration: none;
}
.street-view-link:hover {
  color: #529b2e;
  text-decoration: underline;
}

/* Attributes */
.attr-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.attr-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 10px;
  background: var(--bg-base);
  border-radius: 6px;
  font-size: 13px;
}

.attr-key { color: var(--text-secondary); font-weight: 500; }
.attr-val { color: var(--text-primary); font-weight: 600; }

.mount-tag {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(85, 179, 255, 0.12);
  color: #55b3ff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.replace-btn {
  background: transparent;
  color: #a883ff;
  border: 1px solid rgba(168, 131, 255, 0.25);
  border-radius: 6px;
  padding: 4px 8px;
  min-width: auto;
  margin-left: 4px;
}

.replace-btn:hover {
  background: rgba(168, 131, 255, 0.15);
  color: #a883ff;
}

/* ── Replace dialog ── */
.replace-old-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 16px;
  background: var(--bg-base);
  border-radius: 10px;
}

.replace-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
}

.replace-old-card {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.replace-arrow {
  color: var(--text-muted);
  margin: 4px 0;
}

/* Dialog buttons */
.cancel-btn {
  background: transparent;
  color: var(--text-secondary);
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
}

.cancel-btn:hover { opacity: 0.6; color: var(--text-primary); }

.submit-btn {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none;
  border-radius: 86px;
  padding: 8px 24px;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
}

.submit-btn:hover {
  background: var(--btn-primary-hover);
  color: var(--btn-primary-text);
}

/* Element Plus dark overrides */
:deep(.el-table) {
  --el-table-bg-color: var(--bg-surface);
  --el-table-tr-bg-color: var(--bg-surface);
  --el-table-header-bg-color: var(--bg-surface);
  --el-table-row-hover-bg-color: var(--bg-hover-subtle);
  --el-table-text-color: var(--text-primary);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-border-color: var(--bg-active);
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
}

:deep(.el-table th.el-table__cell) {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

:deep(.el-input__wrapper) {
  background-color: var(--bg-base);
  border: 1px solid var(--border-medium);
  border-radius: 8px;
  box-shadow: none;
}

:deep(.el-input__wrapper:hover) { border-color: var(--border-strong); }

:deep(.el-input__wrapper.is-focus) {
  border-color: rgba(85, 179, 255, 0.5);
  box-shadow: 0 0 0 3px rgba(85, 179, 255, 0.15);
}

:deep(.el-input__inner) {
  color: var(--text-primary);
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
}

:deep(.el-input__inner::placeholder) { color: var(--text-muted); }

:deep(.el-form-item__label) {
  color: var(--text-label);
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
}

:deep(.el-select .el-input__wrapper) {
  background-color: var(--bg-base);
  border: 1px solid var(--border-medium);
  border-radius: 8px;
  box-shadow: none;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: var(--text-secondary);
  --el-pagination-button-bg-color: transparent;
  --el-pagination-hover-color: #55b3ff;
}

:deep(.el-drawer__header) {
  color: var(--text-heading);
  font-weight: 600;
  margin-bottom: 0;
}

:deep(.el-drawer__body) {
  padding: 16px 20px;
}
</style>
