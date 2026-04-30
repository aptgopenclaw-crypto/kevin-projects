<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Send, CheckCircle, Repeat } from 'lucide-vue-next'
import {
  getRepairTicketById,
  acceptRepairTicket,
  transferRepairTicket,
  getDispatches,
  getAttachments,
} from '@/api/repair'
import type { RepairTicketResponse, RepairTicketStatus, RepairTicketSource, RepairTicketPriority, DispatchResponse, AttachmentResponse } from '@/types/repair'
import type { WorkflowStepLogResponse } from '@/types/workflow'
import WorkflowStepper from '@/components/WorkflowStepper.vue'
import AttachmentGallery from '@/components/AttachmentGallery.vue'
import AttachmentUploader from '@/components/AttachmentUploader.vue'
import RepairDispatchDialog from './RepairDispatchDialog.vue'
import CompletionReportDialog from './CompletionReportDialog.vue'
import { formatDateTime, formatDate } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const loading = ref(false)
const ticket = ref<RepairTicketResponse | null>(null)
const dispatches = ref<DispatchResponse[]>([])
const attachments = ref<AttachmentResponse[]>([])
const stepLogs = ref<WorkflowStepLogResponse[]>([])
const activeTab = ref('info')

const dispatchDialogRef = ref<InstanceType<typeof RepairDispatchDialog>>()
const completionDialogRef = ref<InstanceType<typeof CompletionReportDialog>>()

const ticketId = Number(route.params.id)

// ──────────── Helpers ────────────
const statusLabel = (s: RepairTicketStatus) => {
  const map: Record<RepairTicketStatus, string> = {
    PENDING: t('repair.statusPending'),
    ACCEPTED: t('repair.statusAccepted'),
    DISPATCHED: t('repair.statusDispatched'),
    IN_PROGRESS: t('repair.statusInProgress'),
    COMPLETION_REPORTED: t('repair.statusCompletionReported'),
    PENDING_REVIEW: t('repair.statusPendingReview'),
    RETURNED: t('repair.statusReturned'),
    TRANSFERRED: t('repair.statusTransferred'),
    TRACKING: t('repair.statusTracking'),
    CLOSED: t('repair.statusClosed'),
  }
  return map[s] ?? s
}

const statusClass = (s: RepairTicketStatus) => {
  const map: Record<string, string> = {
    PENDING: 'status-warning',
    ACCEPTED: 'status-info',
    DISPATCHED: 'status-info',
    IN_PROGRESS: 'status-primary',
    COMPLETION_REPORTED: 'status-success',
    PENDING_REVIEW: 'status-warning',
    RETURNED: 'status-danger',
    TRANSFERRED: 'status-info',
    TRACKING: 'status-warning',
    CLOSED: 'status-success',
  }
  return map[s] ?? ''
}

const sourceLabel = (s: RepairTicketSource) => {
  const map: Record<RepairTicketSource, string> = {
    FAULT_TICKET: t('repair.sourceFault'),
    CITIZEN_WEB: t('repair.sourceCitizen'),
    EXTERNAL_1999: t('repair.source1999'),
    PATROL: t('repair.sourcePatrol'),
    PHONE: t('repair.sourcePhone'),
  }
  return map[s] ?? s
}

const priorityLabel = (p: RepairTicketPriority) => {
  const map: Record<RepairTicketPriority, string> = {
    LOW: t('repair.priorityLow'),
    NORMAL: t('repair.priorityNormal'),
    HIGH: t('repair.priorityHigh'),
    URGENT: t('repair.priorityUrgent'),
  }
  return map[p] ?? p
}

// ──────────── Data ────────────
async function loadDetail() {
  loading.value = true
  try {
    const [ticketRes, dispatchRes, attachRes] = await Promise.all([
      getRepairTicketById(ticketId),
      getDispatches(ticketId),
      getAttachments(ticketId),
    ])
    ticket.value = ticketRes.body
    dispatches.value = dispatchRes.body
    attachments.value = attachRes.body

    // Load workflow logs if available
    // WorkflowInstance is linked by ticketType + ticketId; we use the ticket's currentStep
    // stepLogs are loaded separately
  } catch {
    ElMessage.error(t('repair.loadFailed'))
  } finally {
    loading.value = false
  }
}

// ──────────── Actions ────────────
async function handleAccept() {
  try {
    await acceptRepairTicket(ticketId)
    ElMessage.success(t('repair.acceptSuccess'))
    loadDetail()
  } catch {
    ElMessage.error(t('common.operationFailed'))
  }
}

function handleDispatch() {
  dispatchDialogRef.value?.open(ticketId)
}

function handleComplete() {
  completionDialogRef.value?.open(ticketId)
}

async function handleTransfer() {
  try {
    await transferRepairTicket(ticketId)
    ElMessage.success(t('repair.transferSuccess'))
    loadDetail()
  } catch {
    ElMessage.error(t('common.operationFailed'))
  }
}

function onDispatched() { loadDetail() }
function onCompleted() { loadDetail() }
function onAttachmentUploaded() { getAttachments(ticketId).then(res => attachments.value = res.body) }

onMounted(loadDetail)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- Back + Header -->
    <div class="detail-header">
      <el-button text @click="router.push('/admin/repair/tickets')">
        <ArrowLeft :size="16" style="margin-right: 4px" /> {{ t('common.back') }}
      </el-button>
    </div>

    <template v-if="ticket">
      <!-- Ticket Header -->
      <div class="ticket-header">
        <div class="ticket-title-row">
          <h2 class="header-title">{{ ticket.ticketNumber }}</h2>
          <span class="status-badge" :class="statusClass(ticket.status)">{{ statusLabel(ticket.status) }}</span>
          <span class="status-badge" :class="ticket.priority ? `priority-${ticket.priority.toLowerCase()}` : ''">{{ priorityLabel(ticket.priority) }}</span>
        </div>
        <p class="header-subtitle">{{ sourceLabel(ticket.source) }} · {{ formatDateTime(ticket.reportedAt) }}</p>
      </div>

      <!-- Action Bar -->
      <div class="action-bar">
        <el-button v-if="ticket.status === 'PENDING'" class="submit-btn" @click="handleAccept">
          <CheckCircle :size="16" style="margin-right: 4px" /> {{ t('repair.actionAccept') }}
        </el-button>
        <el-button v-if="ticket.status === 'ACCEPTED' || ticket.status === 'TRANSFERRED'" class="submit-btn" @click="handleDispatch">
          <Send :size="16" style="margin-right: 4px" /> {{ t('repair.actionDispatch') }}
        </el-button>
        <el-button v-if="ticket.status === 'IN_PROGRESS'" class="submit-btn" @click="handleComplete">
          <CheckCircle :size="16" style="margin-right: 4px" /> {{ t('repair.actionComplete') }}
        </el-button>
        <el-button v-if="['ACCEPTED','DISPATCHED','IN_PROGRESS'].includes(ticket.status)" @click="handleTransfer">
          <Repeat :size="16" style="margin-right: 4px" /> {{ t('repair.actionTransfer') }}
        </el-button>
      </div>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="detail-tabs">
        <!-- Info Tab -->
        <el-tab-pane :label="t('repair.tabInfo')" name="info">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">{{ t('repair.colReporter') }}</span>
              <span class="info-value">{{ ticket.reporterName || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">{{ t('repair.reporterPhone') }}</span>
              <span class="info-value">{{ ticket.reporterPhone || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">{{ t('repair.reportAddress') }}</span>
              <span class="info-value">{{ ticket.reportAddress || '-' }}</span>
            </div>
            <div class="info-item full">
              <span class="info-label">{{ t('repair.colDescription') }}</span>
              <span class="info-value">{{ ticket.reportDescription || '-' }}</span>
            </div>
            <div v-if="ticket.repairDescription" class="info-item full">
              <span class="info-label">{{ t('repair.repairDescription') }}</span>
              <span class="info-value">{{ ticket.repairDescription }}</span>
            </div>
            <div v-if="ticket.faultCause" class="info-item">
              <span class="info-label">{{ t('repair.faultCause') }}</span>
              <span class="info-value">{{ ticket.faultCause }}</span>
            </div>
            <div v-if="ticket.completedAt" class="info-item">
              <span class="info-label">{{ t('repair.completedAt') }}</span>
              <span class="info-value">{{ formatDateTime(ticket.completedAt) }}</span>
            </div>
          </div>
        </el-tab-pane>

        <!-- Dispatch Tab -->
        <el-tab-pane :label="t('repair.tabDispatches')" name="dispatches">
          <div v-if="dispatches.length === 0" class="empty-text">{{ t('common.noData') }}</div>
          <div v-for="(d, idx) in dispatches" :key="d.id" class="dispatch-card">
            <div class="dispatch-header">
              <span class="dispatch-index">#{{ idx + 1 }}</span>
              <span>{{ formatDateTime(d.dispatchedAt) }}</span>
            </div>
            <div class="dispatch-body">
              <p v-if="d.assignedOrg">{{ t('repair.dispatch.assignedOrg') }}: {{ d.assignedOrg }}</p>
              <p v-if="d.dueDate">{{ t('repair.dispatch.dueDate') }}: {{ formatDate(d.dueDate) }}</p>
              <p v-if="d.dispatchNote">{{ d.dispatchNote }}</p>
            </div>
          </div>
        </el-tab-pane>

        <!-- Attachment Tab -->
        <el-tab-pane :label="t('repair.tabAttachments')" name="attachments">
          <AttachmentGallery :attachments="attachments" />
          <el-divider />
          <AttachmentUploader :ticket-id="ticketId" @uploaded="onAttachmentUploaded" />
        </el-tab-pane>

        <!-- Workflow Log Tab -->
        <el-tab-pane :label="t('repair.tabWorkflow')" name="workflow">
          <WorkflowStepper :logs="stepLogs" />
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- Dialogs -->
    <RepairDispatchDialog ref="dispatchDialogRef" @dispatched="onDispatched" />
    <CompletionReportDialog ref="completionDialogRef" @completed="onCompleted" />
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }
.detail-header { margin-bottom: 8px; }
.ticket-header { margin-bottom: 16px; }
.ticket-title-row { display: flex; align-items: center; gap: 12px; }
.header-title { font-size: 22px; font-weight: 700; color: var(--text-heading); margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); margin: 4px 0 0; }
.action-bar { display: flex; gap: 8px; margin-bottom: 20px; }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-warning { background: rgba(230, 162, 60, 0.15); color: #e6a23c; }
.status-danger { background: rgba(255, 99, 99, 0.15); color: #ff6363; }
.status-info { background: rgba(64, 158, 255, 0.15); color: #409eff; }
.status-primary { background: rgba(128, 100, 255, 0.15); color: #8064ff; }
.priority-urgent { background: rgba(255, 99, 99, 0.15); color: #ff6363; }
.priority-high { background: rgba(230, 162, 60, 0.15); color: #e6a23c; }
.priority-normal { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.priority-low { background: rgba(144, 147, 153, 0.15); color: #909399; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-item.full { grid-column: 1 / -1; }
.info-label { font-size: 12px; color: var(--text-secondary); font-weight: 600; text-transform: uppercase; }
.info-value { font-size: 14px; color: var(--text-primary); }
.dispatch-card { border: 1px solid var(--bg-active); border-radius: 8px; padding: 12px; margin-bottom: 12px; }
.dispatch-header { display: flex; justify-content: space-between; font-size: 13px; color: var(--text-secondary); margin-bottom: 8px; }
.dispatch-index { font-weight: 700; color: var(--text-heading); }
.dispatch-body { font-size: 14px; }
.dispatch-body p { margin: 4px 0; }
.empty-text { text-align: center; color: var(--text-secondary); padding: 32px 0; }
:deep(.el-tabs__item) { color: var(--text-secondary); }
:deep(.el-tabs__item.is-active) { color: var(--text-heading); }
</style>
