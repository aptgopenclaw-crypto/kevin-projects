<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { ArrowLeft, Plus } from 'lucide-vue-next'
import { getInspectionRecords, getInspectionTaskById, createInspectionRecord } from '@/api/inspection'
import type { InspectionTaskResponse, InspectionRecordResponse, InspectionRecordRequest, InspectionResult } from '@/types/repair'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const taskId = Number(route.params.taskId)

// ──────────── State ────────────
const loading = ref(false)
const task = ref<InspectionTaskResponse | null>(null)
const records = ref<InspectionRecordResponse[]>([])
const pageSize = ref(15)
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// ──────────── Create Record Dialog ────────────
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const formRef = ref<FormInstance>()
const form = ref<InspectionRecordRequest>({ taskId, result: 'NORMAL' })

// ──────────── Helpers ────────────
const resultLabel = (r: InspectionResult) => {
  const map: Record<InspectionResult, string> = {
    NORMAL: t('inspection.resultNormal'),
    ABNORMAL: t('inspection.resultAbnormal'),
    NEED_REPAIR: t('inspection.resultNeedRepair'),
  }
  return map[r] ?? r
}

const resultClass = (r: InspectionResult) => {
  const map: Record<string, string> = {
    NORMAL: 'status-success',
    ABNORMAL: 'status-warning',
    NEED_REPAIR: 'status-danger',
  }
  return map[r] ?? ''
}

// ──────────── Data ────────────
async function loadTask() {
  try {
    const res = await getInspectionTaskById(taskId)
    task.value = res.body
  } catch {
    // ignore
  }
}

async function loadRecords(page = 0) {
  loading.value = true
  try {
    const res = await getInspectionRecords(taskId, { page, size: pageSize.value })
    records.value = res.body.content
    pagination.value = {
      page: res.body.page,
      totalElements: res.body.totalElements,
      totalPages: res.body.totalPages,
    }
  } catch {
    ElMessage.error(t('inspection.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) { loadRecords(p - 1) }
function handleSizeChange(s: number) { pageSize.value = s; loadRecords(0) }

function openCreate() {
  form.value = { taskId, result: 'NORMAL' }
  dialogVisible.value = true
}

async function handleSubmit() {
  dialogLoading.value = true
  try {
    await createInspectionRecord(form.value)
    ElMessage.success(t('inspection.recordCreatedSuccess'))
    dialogVisible.value = false
    loadRecords(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    dialogLoading.value = false
  }
}

onMounted(() => {
  loadTask()
  loadRecords()
})
</script>

<template>
  <div class="page-container">
    <!-- Back -->
    <div class="detail-header">
      <el-button text @click="router.push('/admin/repair/inspection')">
        <ArrowLeft :size="16" style="margin-right: 4px" /> {{ t('common.back') }}
      </el-button>
    </div>

    <!-- Task Info -->
    <div v-if="task" class="task-header">
      <h2 class="header-title">{{ task.taskName }}</h2>
      <p class="header-subtitle">{{ t('inspection.colTaskType') }}: {{ task.taskType === 'ONE_TIME' ? t('inspection.typeOneTime') : t('inspection.typeRecurring') }}</p>
    </div>

    <!-- Add Record Button -->
    <div style="margin-bottom: 16px">
      <el-button class="create-btn" @click="openCreate">
        <Plus :size="16" style="margin-right: 6px" /> {{ t('inspection.addRecord') }}
      </el-button>
    </div>

    <!-- Records Table -->
    <div class="table-card" v-loading="loading">
      <el-table :data="records" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="deviceId" :label="t('inspection.colDeviceId')" width="100" />
        <el-table-column :label="t('inspection.colResult')" width="120">
          <template #default="{ row }">
            <span class="status-badge" :class="resultClass(row.result)">{{ resultLabel(row.result) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="notes" :label="t('inspection.colNotes')" show-overflow-tooltip />
        <el-table-column :label="t('inspection.colFaultTicketId')" width="120">
          <template #default="{ row }">
            <span v-if="row.faultTicketId">{{ row.faultTicketId }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.createTime')" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
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

    <!-- Create Record Dialog -->
    <el-dialog v-model="dialogVisible" :title="t('inspection.dialogRecordTitle')" width="500px" class="dark-dialog">
      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item :label="t('inspection.colDeviceId')">
          <el-input-number v-model="form.deviceId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('inspection.colResult')">
          <el-select v-model="form.result" style="width: 100%">
            <el-option value="NORMAL" :label="t('inspection.resultNormal')" />
            <el-option value="ABNORMAL" :label="t('inspection.resultAbnormal')" />
            <el-option value="NEED_REPAIR" :label="t('inspection.resultNeedRepair')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inspection.colNotes')">
          <el-input v-model="form.notes" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button class="submit-btn" @click="handleSubmit" :loading="dialogLoading">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }
.detail-header { margin-bottom: 8px; }
.task-header { margin-bottom: 16px; }
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); margin: 4px 0 0; }
.table-card { border-radius: 12px; overflow: hidden; border: 1px solid var(--bg-active); }
.pagination-row { display: flex; justify-content: flex-end; margin-top: 16px; }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-warning { background: rgba(230, 162, 60, 0.15); color: #e6a23c; }
.status-danger { background: rgba(255, 99, 99, 0.15); color: #ff6363; }
:deep(.el-table) { --el-table-bg-color: var(--bg-surface); --el-table-tr-bg-color: var(--bg-surface); }
:deep(.el-table__row--striped td.el-table__cell) { background: var(--bg-base) !important; }
</style>
