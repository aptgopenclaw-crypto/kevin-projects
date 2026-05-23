<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Link2 } from 'lucide-vue-next'
import { getFaultCorrelations } from '@/api/fault'
import type { FaultCorrelationResponse, RootCauseType } from '@/types/fault'

const { t } = useI18n()

const loading = ref(false)
const pageSize = ref(15)
const items = ref<FaultCorrelationResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

const rootCauseLabel = (rc: RootCauseType) => {
  const map: Record<RootCauseType, string> = {
    CIRCUIT: t('faultCorrelation.rootCauseCircuit'),
    GATEWAY: t('faultCorrelation.rootCauseGateway'),
    AREA: t('faultCorrelation.rootCauseArea'),
  }
  return map[rc] ?? rc
}

const corrStatusLabel = (s: string) => {
  const map: Record<string, string> = {
    DETECTED: t('faultCorrelation.statusDetected'),
    CONFIRMED: t('faultCorrelation.statusConfirmed'),
    RESOLVED: t('faultCorrelation.statusResolved'),
  }
  return map[s] ?? s
}

const corrStatusClass = (s: string) => {
  const map: Record<string, string> = {
    DETECTED: 'status-warning',
    CONFIRMED: 'status-info',
    RESOLVED: 'status-success',
  }
  return map[s] ?? ''
}

async function loadData(page = 0) {
  loading.value = true
  try {
    const res = await getFaultCorrelations({ page, size: pageSize.value })
    items.value = res.body.content
    pagination.value = { page: res.body.page, totalElements: res.body.totalElements, totalPages: res.body.totalPages }
  } catch {
    ElMessage.error(t('faultCorrelation.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) { loadData(p - 1) }
function handleSizeChange(s: number) { pageSize.value = s; loadData(0) }

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <div class="page-content">
      <div class="page-header">
        <div class="header-left">
          <div class="header-icon"><Link2 :size="20" /></div>
          <div>
            <h2 class="header-title">{{ t('faultCorrelation.title') }}</h2>
            <p class="header-subtitle">{{ t('faultCorrelation.subtitle') }}</p>
          </div>
        </div>
      </div>

      <div class="table-card" v-loading="loading">
        <el-table :data="items" stripe>
          <el-table-column prop="id" :label="t('faultCorrelation.colId')" width="120" />
          <el-table-column :label="t('faultCorrelation.colRootCauseType')" width="120">
            <template #default="{ row }">{{ rootCauseLabel(row.rootCauseType) }}</template>
          </el-table-column>
          <el-table-column prop="rootCauseId" :label="t('faultCorrelation.colRootCauseId')" width="120" />
          <el-table-column prop="affectedCount" :label="t('faultCorrelation.colAffectedCount')" width="120" />
          <el-table-column :label="t('faultCorrelation.colStatus')" width="110">
            <template #default="{ row }">
              <span class="status-badge" :class="corrStatusClass(row.status)">{{ corrStatusLabel(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="detectedAt" :label="t('faultCorrelation.colDetectedAt')" width="170" />
          <el-table-column prop="confirmedAt" :label="t('faultCorrelation.colConfirmedAt')" width="170" />
          <el-table-column prop="resolvedAt" :label="t('faultCorrelation.colResolvedAt')" width="170" />
        </el-table>
      </div>

      <div class="pagination-row">
        <el-pagination
          :current-page="pagination.page + 1"
          :page-size="pageSize"
          :page-sizes="[15, 30, 50]"
          :total="pagination.totalElements"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }
.page-content {}
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon {
  width: 40px; height: 40px;
  background: rgba(85, 179, 255, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #55b3ff;
}
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); font-family: 'Inter', sans-serif; margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); font-family: 'Inter', sans-serif; margin: 2px 0 0; }
.table-card { background: var(--bg-surface); border: 1px solid var(--bg-active); border-radius: 12px; overflow: hidden; }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-warning { background: rgba(255, 188, 51, 0.15); color: #ffbc33; }
.status-info { background: rgba(85, 179, 255, 0.15); color: #55b3ff; }
.pagination-row { display: flex; justify-content: flex-end; padding: 16px 20px; border-top: 1px solid var(--bg-active); }

:deep(.el-table) {
  --el-table-bg-color: var(--bg-surface); --el-table-tr-bg-color: var(--bg-surface);
  --el-table-header-bg-color: var(--bg-surface); --el-table-row-hover-bg-color: var(--bg-hover-subtle);
  --el-table-text-color: var(--text-primary); --el-table-header-text-color: var(--text-secondary);
  --el-table-border-color: var(--bg-active); font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500;
}
:deep(.el-table th.el-table__cell) { font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px; }
:deep(.el-pagination) { --el-pagination-bg-color: transparent; --el-pagination-text-color: var(--text-secondary); --el-pagination-button-bg-color: transparent; --el-pagination-hover-color: #55b3ff; }
</style>
