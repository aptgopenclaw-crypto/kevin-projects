<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getMyApplications, getPendingTasks } from '@/api/assetTransfer'
import type { AssetTransferApplicationDto, AssetTransferStatus } from '@/types/assetTransfer'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

/** When meta.mode === 'my' we fetch the user's own applications, otherwise pending tasks */
const isMyMode = computed(() => route.meta?.mode === 'my')

const items = ref<AssetTransferApplicationDto[]>([])
const loading = ref(false)

const pageTitle = computed(() =>
  isMyMode.value ? t('assetTransfer.myTitle') : t('assetTransfer.pendingTitle'),
)
const pageSubtitle = computed(() =>
  isMyMode.value ? t('assetTransfer.mySubtitle') : t('assetTransfer.pendingSubtitle'),
)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = isMyMode.value ? await getMyApplications() : await getPendingTasks()
    items.value = res.body
  } catch {
    ElMessage.error(t('assetTransfer.loadFailed'))
  } finally {
    loading.value = false
  }
}

function goCreate() {
  router.push('/asset-transfer/create')
}

function goDetail(row: AssetTransferApplicationDto) {
  router.push(`/asset-transfer/${row.id}`)
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`
}

const STATUS_TAG_TYPE: Record<AssetTransferStatus, 'info' | 'warning' | 'success' | 'danger'> = {
  DRAFT: 'info',
  PROCESSING: 'warning',
  COMPLETED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'info',
}

function getStatusType(status: AssetTransferStatus) {
  return STATUS_TAG_TYPE[status] ?? 'info'
}

function getStatusLabel(status: AssetTransferStatus) {
  const map: Record<AssetTransferStatus, string> = {
    DRAFT: t('assetTransfer.statusDraft'),
    PROCESSING: t('assetTransfer.statusPending'),
    COMPLETED: t('assetTransfer.statusApproved'),
    REJECTED: t('assetTransfer.statusRejected'),
    CANCELLED: t('assetTransfer.statusCancelled'),
  }
  return map[status] ?? status
}

function getTransferTypeLabel(type: string) {
  const map: Record<string, string> = {
    INTERNAL: t('assetTransfer.transferTypeInternal'),
    EXTERNAL: t('assetTransfer.transferTypeExternal'),
    DISPOSAL: t('assetTransfer.transferTypeDisposal'),
    RETURN: t('assetTransfer.transferTypeReturn'),
  }
  return map[type] ?? type
}
</script>

<template>
  <div class="page-container">
    <div class="page-content">
      <div class="page-header">
        <div>
          <h1 class="page-title">{{ pageTitle }}</h1>
          <p class="page-subtitle">{{ pageSubtitle }}</p>
        </div>
        <el-button v-if="isMyMode" class="create-btn" @click="goCreate">
          + {{ t('assetTransfer.createTitle') }}
        </el-button>
      </div>

      <div class="table-card" v-loading="loading">
        <el-table :data="items" style="width: 100%" row-class-name="table-row">
          <el-table-column prop="applicationNo" :label="t('assetTransfer.colAppNo')" min-width="160" />
          <el-table-column prop="assetCode" :label="t('assetTransfer.colAssetCode')" min-width="120" />
          <el-table-column prop="assetName" :label="t('assetTransfer.colAssetName')" min-width="160" />
          <el-table-column :label="t('assetTransfer.colTransferType')" min-width="120">
            <template #default="{ row }">
              {{ getTransferTypeLabel(row.transferType) }}
            </template>
          </el-table-column>
          <el-table-column prop="departmentName" :label="t('assetTransfer.colDept')" min-width="120">
            <template #default="{ row }">
              {{ row.departmentName || '-' }}
            </template>
          </el-table-column>
          <el-table-column :label="t('assetTransfer.colStatus')" width="110">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applicantName" :label="t('assetTransfer.colApplicant')" min-width="100">
            <template #default="{ row }">
              {{ row.applicantName || row.applicantId }}
            </template>
          </el-table-column>
          <el-table-column :label="t('assetTransfer.colCreatedAt')" min-width="110">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('assetTransfer.colActions')" width="90" fixed="right">
            <template #default="{ row }">
              <el-button size="small" class="action-btn" @click="goDetail(row)">
                {{ t('assetTransfer.btnViewDetail') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!loading && items.length === 0" class="empty-hint">
          {{ t('assetTransfer.noData') }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  padding: 32px 24px;
  min-height: 100vh;
  background-color: var(--bg-base);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-heading);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.create-btn {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none;
  border-radius: 86px;
  padding: 8px 24px;
  font-size: 14px;
  font-weight: 600;
}

.create-btn:hover {
  background: var(--btn-primary-hover);
  color: var(--btn-primary-text);
}

.table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  overflow: hidden;
}

.action-btn {
  background: transparent;
  color: var(--text-primary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
}

.empty-hint {
  text-align: center;
  padding: 48px;
  color: var(--text-secondary);
  font-size: 14px;
}
</style>
