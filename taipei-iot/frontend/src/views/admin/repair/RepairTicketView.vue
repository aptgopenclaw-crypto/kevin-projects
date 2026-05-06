<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Wrench, Plus, Search } from 'lucide-vue-next'
import { getRepairTickets, createRepairTicket } from '@/api/repair'
import type {
  RepairTicketResponse,
  RepairTicketRequest,
  RepairTicketStatus,
  RepairTicketSource,
  RepairTicketPriority,
} from '@/types/repair'
import { formatDateTime } from '@/utils/datetime'

const { t } = useI18n()
const router = useRouter()

// ──────────── List State ────────────
const loading = ref(false)
const keyword = ref('')
const filterStatus = ref<RepairTicketStatus | ''>('')
const filterSource = ref<RepairTicketSource | ''>('')
const filterPriority = ref<RepairTicketPriority | ''>('')
const pageSize = ref(15)
const items = ref<RepairTicketResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// ──────────── Create Dialog ────────────
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = ref<RepairTicketRequest>({ source: 'PHONE' })

const createRules = computed<FormRules>(() => ({
  source: [{ required: true, message: t('repair.errors.sourceRequired'), trigger: 'change' }],
}))

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

const priorityClass = (p: RepairTicketPriority) => {
  const map: Record<string, string> = {
    LOW: 'status-info',
    NORMAL: 'status-success',
    HIGH: 'status-warning',
    URGENT: 'status-danger',
  }
  return map[p] ?? ''
}

// ──────────── Data Loading ────────────
async function loadData(page = 0) {
  loading.value = true
  try {
    const res = await getRepairTickets({
      page,
      size: pageSize.value,
      status: filterStatus.value || undefined,
      source: filterSource.value || undefined,
      priority: filterPriority.value || undefined,
      keyword: keyword.value || undefined,
    })
    items.value = res.body.content
    pagination.value = {
      page: res.body.page,
      totalElements: res.body.totalElements,
      totalPages: res.body.totalPages,
    }
  } catch {
    ElMessage.error(t('repair.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleSearch() { loadData(0) }
function handlePageChange(p: number) { loadData(p - 1) }
function handleSizeChange(s: number) { pageSize.value = s; loadData(0) }

function goDetail(row: RepairTicketResponse) {
  router.push(`/admin/repair/tickets/${row.id}`)
}

// ──────────── Create ────────────
function openCreateDialog() {
  createForm.value = { source: 'PHONE' }
  createVisible.value = true
}

async function handleCreate() {
  const form = createFormRef.value
  if (!form) return
  const valid = await form.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    await createRepairTicket(createForm.value)
    ElMessage.success(t('repair.createdSuccess'))
    createVisible.value = false
    loadData(pagination.value.page)
  } catch {
    ElMessage.error(t('repair.loadFailed'))
  } finally {
    createLoading.value = false
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><Wrench :size="20" /></div>
        <div>
          <h2 class="header-title">{{ t('repair.title') }}</h2>
          <p class="header-subtitle">{{ t('repair.subtitle') }}</p>
        </div>
      </div>
      <el-button class="create-btn" @click="openCreateDialog">
        <Plus :size="16" style="margin-right: 6px" /> {{ t('repair.addBtn') }}
      </el-button>
    </div>

    <!-- Filter -->
    <div class="filter-bar">
      <el-select v-model="filterStatus" clearable :placeholder="t('repair.filterStatus')" style="width: 150px" @change="handleSearch">
        <el-option v-for="s in (['PENDING','ACCEPTED','DISPATCHED','IN_PROGRESS','COMPLETION_REPORTED','PENDING_REVIEW','RETURNED','TRANSFERRED','CLOSED'] as RepairTicketStatus[])" :key="s" :value="s" :label="statusLabel(s)" />
      </el-select>
      <el-select v-model="filterSource" clearable :placeholder="t('repair.filterSource')" style="width: 140px" @change="handleSearch">
        <el-option v-for="s in (['FAULT_TICKET','CITIZEN_WEB','EXTERNAL_1999','PATROL','PHONE'] as RepairTicketSource[])" :key="s" :value="s" :label="sourceLabel(s)" />
      </el-select>
      <el-select v-model="filterPriority" clearable :placeholder="t('repair.filterPriority')" style="width: 120px" @change="handleSearch">
        <el-option v-for="p in (['LOW','NORMAL','HIGH','URGENT'] as RepairTicketPriority[])" :key="p" :value="p" :label="priorityLabel(p)" />
      </el-select>
      <el-input v-model="keyword" clearable :placeholder="t('repair.searchPlaceholder')" style="width: 220px" @keyup.enter="handleSearch" />
      <el-button class="search-btn" @click="handleSearch"><Search :size="16" /></el-button>
    </div>

    <!-- Table -->
    <div class="table-card" v-loading="loading">
      <el-table :data="items" stripe @row-click="goDetail" style="cursor: pointer">
        <el-table-column prop="ticketNumber" :label="t('repair.colTicketNumber')" width="170" />
        <el-table-column :label="t('repair.colStatus')" width="120">
          <template #default="{ row }">
            <span class="status-badge" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('repair.colSource')" width="110">
          <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
        </el-table-column>
        <el-table-column :label="t('repair.colPriority')" width="90">
          <template #default="{ row }">
            <span class="status-badge" :class="priorityClass(row.priority)">{{ priorityLabel(row.priority) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" :label="t('repair.colReporter')" width="120" />
        <el-table-column :label="t('repair.colReportedAt')" width="170">
          <template #default="{ row }">{{ formatDateTime(row.reportedAt) }}</template>
        </el-table-column>
        <el-table-column prop="reportDescription" :label="t('repair.colDescription')" show-overflow-tooltip />
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
    <el-dialog v-model="createVisible" :title="t('repair.dialogCreateTitle')" width="560px" class="dark-dialog">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item :label="t('repair.colSource')" prop="source">
          <el-select v-model="createForm.source" style="width: 100%">
            <el-option v-for="s in (['CITIZEN_WEB','EXTERNAL_1999','PATROL','PHONE'] as RepairTicketSource[])" :key="s" :value="s" :label="sourceLabel(s)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('repair.colReporter')">
          <el-input v-model="createForm.reporterName" />
        </el-form-item>
        <el-form-item :label="t('repair.reporterPhone')">
          <el-input v-model="createForm.reporterPhone" />
        </el-form-item>
        <el-form-item :label="t('repair.reportAddress')">
          <el-input v-model="createForm.reportAddress" />
        </el-form-item>
        <el-form-item :label="t('repair.colDescription')">
          <el-input v-model="createForm.reportDescription" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="t('repair.colPriority')">
          <el-select v-model="createForm.priority" style="width: 100%">
            <el-option v-for="p in (['LOW','NORMAL','HIGH','URGENT'] as RepairTicketPriority[])" :key="p" :value="p" :label="priorityLabel(p)" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button class="submit-btn" @click="handleCreate" :loading="createLoading">{{ t('common.confirm') }}</el-button>
      </template>
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
.status-warning { background: rgba(230, 162, 60, 0.15); color: #e6a23c; }
.status-danger { background: rgba(255, 99, 99, 0.15); color: #ff6363; }
.status-info { background: rgba(64, 158, 255, 0.15); color: #409eff; }
.status-primary { background: rgba(128, 100, 255, 0.15); color: #8064ff; }
:deep(.el-table) { --el-table-bg-color: var(--bg-surface); --el-table-tr-bg-color: var(--bg-surface); }
:deep(.el-table__row--striped td.el-table__cell) { background: var(--bg-base) !important; }
</style>
