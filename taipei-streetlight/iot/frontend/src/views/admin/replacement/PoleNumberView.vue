<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Hash, Plus, Search, QrCode, FileDown } from 'lucide-vue-next'
import { getPoleNumbers, generatePoleNumber, getPoleNumberQrCode, batchExportQrCodePdf } from '@/api/replacement'
import type { PoleNumberResponse, PoleNumberRequest, PoleNumberStatus } from '@/types/replacement'
import { formatDateTime } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const pageSize = ref(15)
const items = ref<PoleNumberResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// ──────────── Selection ────────────
const selectedIds = ref<number[]>([])

const handleSelectionChange = (rows: PoleNumberResponse[]) => {
  selectedIds.value = rows.map(r => r.id)
}

// ──────────── Create ────────────
const createVisible = ref(false)
const createLoading = ref(false)
const createForm = ref<PoleNumberRequest>({ poleNumber: '' })

// ──────────── QR Preview ────────────
const qrPreviewVisible = ref(false)
const qrPreviewLoading = ref(false)
const qrPreviewUrl = ref('')
const qrPreviewLabel = ref('')

// ──────────── Batch Export ────────────
const batchExporting = ref(false)
const hasSelection = computed(() => selectedIds.value.length > 0)

const statusLabel = (s: PoleNumberStatus) => {
  const map: Record<PoleNumberStatus, string> = {
    ACTIVE: t('replacement.poleStatusActive'),
    DECOMMISSIONED: t('replacement.poleStatusDecommissioned'),
    LOST: t('replacement.poleStatusLost'),
  }
  return map[s] ?? s
}

const statusClass = (s: PoleNumberStatus) => {
  const map: Record<string, string> = {
    ACTIVE: 'status-success',
    DECOMMISSIONED: 'status-info',
    LOST: 'status-danger',
  }
  return map[s] ?? ''
}

const fetchData = async (page = 0) => {
  loading.value = true
  try {
    const res = await getPoleNumbers({ keyword: keyword.value || undefined, page, size: pageSize.value })
    items.value = res.body.content
    pagination.value = { page: res.body.page, totalElements: res.body.totalElements, totalPages: res.body.totalPages }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => fetchData(0)
const handlePageChange = (p: number) => fetchData(p - 1)
const handleSizeChange = (s: number) => { pageSize.value = s; fetchData(0) }

const openCreateDialog = () => {
  createForm.value = { poleNumber: '' }
  createVisible.value = true
}

const handleCreate = async () => {
  if (!createForm.value.poleNumber) {
    ElMessage.warning(t('replacement.validation.poleNumberRequired'))
    return
  }
  createLoading.value = true
  try {
    await generatePoleNumber(createForm.value)
    ElMessage.success(t('replacement.generatedSuccess'))
    createVisible.value = false
    fetchData(0)
  } finally {
    createLoading.value = false
  }
}

const openQrPreview = async (row: PoleNumberResponse) => {
  qrPreviewLabel.value = row.poleNumber
  qrPreviewLoading.value = true
  qrPreviewVisible.value = true
  try {
    const res = await getPoleNumberQrCode(row.id)
    const blob = new Blob([res as unknown as BlobPart], { type: 'image/png' })
    qrPreviewUrl.value = URL.createObjectURL(blob)
  } catch {
    ElMessage.error(t('replacement.qrCodeFailed'))
    qrPreviewVisible.value = false
  } finally {
    qrPreviewLoading.value = false
  }
}

const closeQrPreview = () => {
  if (qrPreviewUrl.value) {
    URL.revokeObjectURL(qrPreviewUrl.value)
    qrPreviewUrl.value = ''
  }
}

const handleBatchExport = async () => {
  if (!hasSelection.value) return
  batchExporting.value = true
  try {
    const res = await batchExportQrCodePdf(selectedIds.value)
    const blob = new Blob([res as unknown as BlobPart], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'pole-numbers-qrcode.pdf'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success(t('replacement.batchExportSuccess'))
  } catch {
    ElMessage.error(t('replacement.batchExportFailed'))
  } finally {
    batchExporting.value = false
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><Hash :size="20" /></div>
        <div>
          <h2 class="header-title">{{ t('replacement.poleNumberTitle') }}</h2>
          <p class="header-subtitle">{{ t('replacement.poleNumberSubtitle') }}</p>
        </div>
      </div>
      <el-button class="create-btn" @click="openCreateDialog">
        <Plus :size="16" style="margin-right: 6px" /> {{ t('replacement.generatePoleNumber') }}
      </el-button>
    </div>

    <!-- Filter -->
    <div class="filter-bar">
      <el-input v-model="keyword" clearable :placeholder="t('replacement.searchPoleNumber')" style="width: 260px" @keyup.enter="handleSearch" />
      <el-button class="search-btn" @click="handleSearch"><Search :size="16" /></el-button>
      <el-button class="export-btn" :loading="batchExporting" :disabled="!hasSelection" @click="handleBatchExport">
        <FileDown :size="16" style="margin-right: 6px" /> {{ t('replacement.batchExportQrPdf') }}
        <span v-if="hasSelection" style="margin-left: 4px">({{ selectedIds.length }})</span>
      </el-button>
    </div>

    <!-- Table -->
    <div class="table-card" v-loading="loading">
      <el-table :data="items" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="poleNumber" :label="t('replacement.poleNumber')" width="200" />
        <el-table-column prop="deviceId" :label="t('replacement.poleDevice')" width="150" />
        <el-table-column :label="t('replacement.poleIssuedAt')" width="150">
          <template #default="{ row }">{{ row.issuedAt }}</template>
        </el-table-column>
        <el-table-column :label="t('replacement.filterStatus')" width="120">
          <template #default="{ row }">
            <span class="status-badge" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('replacement.createdAt')" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('replacement.qrCodeAction')" width="120" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openQrPreview(row)">
              <QrCode :size="16" style="margin-right: 4px" /> QR Code
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Pagination -->
    <div class="pagination-row">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :current-page="pagination.page + 1"
        :page-size="pageSize"
        :page-sizes="[10, 15, 30, 50]"
        :total="pagination.totalElements"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="createVisible" :title="t('replacement.dialogGenerateTitle')" width="460px" class="dark-dialog">
      <el-form :model="createForm" label-position="top">
        <el-form-item :label="t('replacement.poleNumber')">
          <el-input v-model="createForm.poleNumber" />
        </el-form-item>
        <el-form-item :label="t('replacement.poleDevice')">
          <el-input-number v-model="createForm.deviceId" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button class="submit-btn" :loading="createLoading" @click="handleCreate">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- QR Code Preview Dialog -->
    <el-dialog v-model="qrPreviewVisible" :title="t('replacement.qrCodePreview')" width="380px" class="dark-dialog" @close="closeQrPreview">
      <div class="qr-preview-content" v-loading="qrPreviewLoading">
        <img v-if="qrPreviewUrl" :src="qrPreviewUrl" :alt="qrPreviewLabel" class="qr-preview-img" />
        <p class="qr-preview-label">{{ qrPreviewLabel }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 14px; }
.header-icon {
  width: 40px; height: 40px;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #409eff;
}
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); margin: 4px 0 0; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.table-card { border-radius: 12px; overflow: hidden; border: 1px solid var(--bg-active); }
.pagination-row { display: flex; justify-content: flex-end; margin-top: 16px; }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-info { background: rgba(64, 158, 255, 0.15); color: #409eff; }
.status-danger { background: rgba(255, 99, 99, 0.15); color: #ff6363; }
:deep(.el-table) { --el-table-bg-color: var(--bg-surface); --el-table-tr-bg-color: var(--bg-surface); }
:deep(.el-table__row--striped td.el-table__cell) { background: var(--bg-base) !important; }
.qr-preview-content { display: flex; flex-direction: column; align-items: center; padding: 16px 0; }
.qr-preview-img { width: 260px; height: 260px; border-radius: 8px; background: #fff; padding: 8px; }
.qr-preview-label { margin-top: 12px; font-size: 16px; font-weight: 600; color: var(--text-heading); }
</style>
