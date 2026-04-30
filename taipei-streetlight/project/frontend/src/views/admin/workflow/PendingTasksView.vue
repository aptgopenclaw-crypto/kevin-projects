<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ClipboardList, Eye, Play, XCircle } from 'lucide-vue-next'
import { getPendingTasks, getStepLogs, transitionWorkflow, cancelWorkflow } from '@/api/workflow'
import type {
  WorkflowInstanceResponse,
  WorkflowStepLogResponse,
  WorkflowTransitionRequest,
  WorkflowStatus,
  WorkflowType,
  TicketType,
} from '@/types/workflow'
import WorkflowStepper from '@/components/WorkflowStepper.vue'
import WorkflowActionBar from '@/components/WorkflowActionBar.vue'

const { t } = useI18n()

const loading = ref(false)
const pageSize = ref(15)
const items = ref<WorkflowInstanceResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// Logs drawer
const logsVisible = ref(false)
const logsLoading = ref(false)
const logsData = ref<WorkflowStepLogResponse[]>([])
const logsInstanceId = ref<number | null>(null)

// Action bar ref
const actionBarRef = ref<InstanceType<typeof WorkflowActionBar>>()
const actionInstanceId = ref<number | null>(null)

const statusLabel = (s: WorkflowStatus) => {
  const map: Record<WorkflowStatus, string> = {
    ACTIVE: t('workflow.statusActive'),
    COMPLETED: t('workflow.statusCompleted'),
    CANCELLED: t('workflow.statusCancelled'),
  }
  return map[s] ?? s
}

const statusClass = (s: WorkflowStatus) => {
  const map: Record<WorkflowStatus, string> = {
    ACTIVE: 'status-info',
    COMPLETED: 'status-success',
    CANCELLED: 'status-danger',
  }
  return map[s] ?? ''
}

const wfTypeLabel = (wt: WorkflowType) => {
  const map: Record<WorkflowType, string> = {
    FAULT_REVIEW: t('workflow.typeFaultReview'),
    REPAIR_DISPATCH: t('workflow.typeRepairDispatch'),
    REPAIR_CLOSE: t('workflow.typeRepairClose'),
    REPLACEMENT_REVIEW: t('workflow.typeReplacementReview'),
    ASSET_CHANGE: t('workflow.typeAssetChange'),
  }
  return map[wt] ?? wt
}

const ticketTypeLabel = (tt: TicketType) => {
  const map: Record<TicketType, string> = {
    FAULT_TICKET: t('workflow.ticketFault'),
    REPAIR_TICKET: t('workflow.ticketRepair'),
    REPLACEMENT_ORDER: t('workflow.ticketReplacement'),
    ASSET_CHANGE: t('workflow.ticketAssetChange'),
  }
  return map[tt] ?? tt
}

async function loadData(page = 0) {
  loading.value = true
  try {
    const res = await getPendingTasks({ page, size: pageSize.value })
    items.value = res.body.content
    pagination.value = {
      page: res.body.page,
      totalElements: res.body.totalElements,
      totalPages: res.body.totalPages,
    }
  } catch {
    ElMessage.error(t('workflow.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) { loadData(p - 1) }
function handleSizeChange(s: number) { pageSize.value = s; loadData(0) }

async function openLogs(row: WorkflowInstanceResponse) {
  logsInstanceId.value = row.id
  logsVisible.value = true
  logsLoading.value = true
  try {
    const res = await getStepLogs(row.id)
    logsData.value = res.body
  } catch {
    ElMessage.error(t('workflow.loadFailed'))
  } finally {
    logsLoading.value = false
  }
}

function openAction(row: WorkflowInstanceResponse) {
  actionInstanceId.value = row.id
  actionBarRef.value?.open()
}

async function handleTransition(payload: WorkflowTransitionRequest) {
  if (!actionInstanceId.value) return
  try {
    await transitionWorkflow(actionInstanceId.value, payload)
    ElMessage.success(t('workflow.transitionSuccess'))
    loadData(pagination.value.page)
  } catch {
    ElMessage.error(t('workflow.loadFailed'))
  }
}

async function handleCancel(row: WorkflowInstanceResponse) {
  try {
    await ElMessageBox.confirm(t('workflow.cancelConfirm'), { type: 'warning' })
    await cancelWorkflow(row.id)
    ElMessage.success(t('workflow.cancelledSuccess'))
    loadData(pagination.value.page)
  } catch { /* cancelled */ }
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <div class="page-content">
      <div class="page-header">
        <div class="header-left">
          <div class="header-icon"><ClipboardList :size="20" /></div>
          <div>
            <h2 class="header-title">{{ t('workflow.title') }}</h2>
            <p class="header-subtitle">{{ t('workflow.subtitle') }}</p>
          </div>
        </div>
      </div>

      <div class="table-card" v-loading="loading">
        <el-table :data="items" stripe>
          <el-table-column prop="id" :label="t('workflow.colId')" width="90" />
          <el-table-column :label="t('workflow.colWorkflowType')" width="130">
            <template #default="{ row }">{{ wfTypeLabel(row.workflowType) }}</template>
          </el-table-column>
          <el-table-column :label="t('workflow.colTicketType')" width="120">
            <template #default="{ row }">{{ ticketTypeLabel(row.ticketType) }}</template>
          </el-table-column>
          <el-table-column prop="ticketId" :label="t('workflow.colTicketId')" width="90" />
          <el-table-column prop="currentStep" :label="t('workflow.colCurrentStep')" width="120">
            <template #default="{ row }"><span class="code-text">{{ row.currentStep }}</span></template>
          </el-table-column>
          <el-table-column :label="t('workflow.colStatus')" width="100">
            <template #default="{ row }">
              <span class="status-badge" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('workflow.colAssignedTo')" width="120">
            <template #default="{ row }">
              {{ row.assignedToName || row.assignedTo }}
              <span v-if="row.delegatedFrom" class="delegated-tag">{{ t('workflow.colDelegatedFrom') }}: {{ row.delegatedFrom }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="startedAt" :label="t('workflow.colStartedAt')" width="160" />
          <el-table-column :label="t('common.actions')" width="140" fixed="right">
            <template #default="{ row }">
              <el-button class="action-btn action-view" size="small" @click="openLogs(row)"><Eye :size="14" /></el-button>
              <el-button v-if="row.status === 'ACTIVE'" class="action-btn action-transition" size="small" @click="openAction(row)"><Play :size="14" /></el-button>
              <el-button v-if="row.status === 'ACTIVE'" class="action-btn action-cancel" size="small" @click="handleCancel(row)"><XCircle :size="14" /></el-button>
            </template>
          </el-table-column>
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

    <!-- Logs Drawer -->
    <el-drawer v-model="logsVisible" :title="t('workflow.logsTitle')" size="480px" direction="rtl">
      <div v-loading="logsLoading" style="padding: 8px 0">
        <WorkflowStepper :logs="logsData" />
      </div>
    </el-drawer>

    <!-- Action Dialog -->
    <WorkflowActionBar
      ref="actionBarRef"
      :instance-id="actionInstanceId ?? 0"
      @submit="handleTransition"
    />
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }
.page-content { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon {
  width: 40px; height: 40px;
  background: rgba(167, 139, 250, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #a78bfa;
}
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); font-family: 'Inter', sans-serif; margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); font-family: 'Inter', sans-serif; margin: 2px 0 0; }
.table-card { background: var(--bg-surface); border: 1px solid var(--bg-active); border-radius: 12px; overflow: hidden; }

.code-text { color: #55b3ff; font-weight: 600; font-family: 'JetBrains Mono', monospace; font-size: 13px; }

.status-badge { display: inline-block; padding: 2px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-info { background: rgba(85, 179, 255, 0.15); color: #55b3ff; }
.status-danger { background: rgba(255, 99, 99, 0.15); color: #FF6363; }

.delegated-tag { display: block; font-size: 11px; color: #a78bfa; margin-top: 2px; }

.action-btn { padding: 4px 8px; min-width: auto; background: transparent; border-radius: 6px; }
.action-view { color: #55b3ff; border: 1px solid rgba(85, 179, 255, 0.2); }
.action-view:hover { background: rgba(85, 179, 255, 0.15); color: #55b3ff; }
.action-transition { color: #5fc992; border: 1px solid rgba(95, 201, 146, 0.2); }
.action-transition:hover { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.action-cancel { color: #FF6363; border: 1px solid rgba(255, 99, 99, 0.2); }
.action-cancel:hover { background: rgba(255, 99, 99, 0.15); color: #FF6363; }

.pagination-row { display: flex; justify-content: flex-end; padding: 16px 20px; border-top: 1px solid var(--bg-active); }

:deep(.el-table) {
  --el-table-bg-color: var(--bg-surface); --el-table-tr-bg-color: var(--bg-surface);
  --el-table-header-bg-color: var(--bg-surface); --el-table-row-hover-bg-color: var(--bg-hover-subtle);
  --el-table-text-color: var(--text-primary); --el-table-header-text-color: var(--text-secondary);
  --el-table-border-color: var(--bg-active); font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500;
}
:deep(.el-table th.el-table__cell) { font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px; }
:deep(.el-pagination) { --el-pagination-bg-color: transparent; --el-pagination-text-color: var(--text-secondary); --el-pagination-button-bg-color: transparent; --el-pagination-hover-color: #55b3ff; }
:deep(.el-drawer) { background: var(--bg-surface); }
:deep(.el-drawer__header) { color: var(--text-heading); }
</style>
