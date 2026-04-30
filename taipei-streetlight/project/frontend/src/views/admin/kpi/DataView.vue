<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Database, Upload, Search } from 'lucide-vue-next'
import { getRawData, importRawData, getIndicators } from '@/api/kpi'
import type { KpiRawDataResponse, KpiIndicatorResponse } from '@/types/kpi'
import { formatDateTime } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const filterYear = ref<number>(new Date().getFullYear())
const filterMonth = ref<number>(new Date().getMonth() + 1)
const filterIndicatorId = ref<number | ''>('')
const pageSize = ref(20)
const items = ref<KpiRawDataResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// Indicator options for filter + import
const indicatorOptions = ref<KpiIndicatorResponse[]>([])

// Import dialog
const importVisible = ref(false)
const importLoading = ref(false)
const importIndicatorId = ref<number | null>(null)
const importYear = ref(new Date().getFullYear())
const importMonth = ref(new Date().getMonth() + 1)
const importFile = ref<File | null>(null)

const sourceLabel = (s: string) => (s === 'AUTO' ? '系統自動' : '手動匯入')

async function fetchIndicators() {
  try {
    const res = await getIndicators({ size: 200 })
    indicatorOptions.value = res.body.content
  } catch {
    // ignore
  }
}

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getRawData({
      periodYear: filterYear.value || undefined,
      periodMonth: filterMonth.value || undefined,
      indicatorId: filterIndicatorId.value || undefined,
      page,
      size: pageSize.value,
    })
    items.value = res.body.content
    pagination.value = {
      page: res.body.page,
      totalElements: res.body.totalElements,
      totalPages: res.body.totalPages,
    }
  } catch {
    ElMessage.error(t('kpi.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) {
  fetchData(p - 1)
}

function openImport() {
  importFile.value = null
  importVisible.value = true
}

function handleFileChange(file: any) {
  importFile.value = file.raw
}

async function handleImport() {
  if (!importIndicatorId.value || !importFile.value) {
    ElMessage.warning(t('kpi.importFieldsRequired'))
    return
  }
  importLoading.value = true
  try {
    const res = await importRawData(importIndicatorId.value, importYear.value, importMonth.value, importFile.value)
    ElMessage.success(t('kpi.importSuccess', { count: res.body }))
    importVisible.value = false
    fetchData()
  } catch {
    ElMessage.error(t('kpi.importFailed'))
  } finally {
    importLoading.value = false
  }
}

onMounted(() => {
  fetchIndicators()
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <Database :size="22" />
        <h2>{{ t('kpi.dataTitle') }}</h2>
      </div>
      <el-button type="primary" @click="openImport">
        <Upload :size="16" />
        {{ t('kpi.importData') }}
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input-number v-model="filterYear" :min="2020" :max="2099" controls-position="right" style="width: 120px" />
      <el-select v-model="filterMonth" :placeholder="t('kpi.month')" style="width: 100px">
        <el-option v-for="m in 12" :key="m" :label="`${m} 月`" :value="m" />
      </el-select>
      <el-select v-model="filterIndicatorId" :placeholder="t('kpi.indicator')" clearable style="width: 200px">
        <el-option v-for="ind in indicatorOptions" :key="ind.id" :label="ind.indicatorName" :value="ind.id" />
      </el-select>
      <el-button @click="fetchData()">
        <Search :size="16" />
        {{ t('common.query') }}
      </el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="indicatorCode" :label="t('kpi.indicatorCode')" width="140" />
      <el-table-column prop="indicatorName" :label="t('kpi.indicatorName')" min-width="180" />
      <el-table-column :label="t('kpi.period')" width="110">
        <template #default="{ row }">{{ row.periodYear }}/{{ String(row.periodMonth).padStart(2, '0') }}</template>
      </el-table-column>
      <el-table-column :label="t('kpi.contractId')" width="100" align="right">
        <template #default="{ row }">{{ row.contractId ?? t('kpi.cityLevel') }}</template>
      </el-table-column>
      <el-table-column prop="rawValue" :label="t('kpi.rawValue')" width="120" align="right" />
      <el-table-column :label="t('kpi.source')" width="100">
        <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
      </el-table-column>
      <el-table-column :label="t('kpi.collectedAt')" width="170">
        <template #default="{ row }">{{ formatDateTime(row.collectedAt) }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        :current-page="pagination.page + 1"
        :page-size="pageSize"
        :total="pagination.totalElements"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <!-- Import Dialog -->
    <el-dialog v-model="importVisible" :title="t('kpi.importData')" width="500px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item :label="t('kpi.indicator')">
          <el-select v-model="importIndicatorId" :placeholder="t('kpi.selectIndicator')" style="width: 100%">
            <el-option v-for="ind in indicatorOptions" :key="ind.id" :label="ind.indicatorName" :value="ind.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('kpi.year')">
          <el-input-number v-model="importYear" :min="2020" :max="2099" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('kpi.month')">
          <el-select v-model="importMonth" style="width: 100%">
            <el-option v-for="m in 12" :key="m" :label="`${m} 月`" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('kpi.file')">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls,.csv"
            :on-change="handleFileChange"
          >
            <el-button>{{ t('kpi.selectFile') }}</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImport">{{ t('kpi.doImport') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 8px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
