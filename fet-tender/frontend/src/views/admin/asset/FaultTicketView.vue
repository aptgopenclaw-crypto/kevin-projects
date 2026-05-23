<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { AlertTriangle, Plus, Search, CheckCircle } from 'lucide-vue-next'
import { getFaultTickets, createFaultTicket, resolveFaultTicket } from '@/api/fault'
import type { FaultTicketResponse, FaultTicketRequest, FaultTicketStatus, FaultTicketSource } from '@/types/fault'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const filterStatus = ref<FaultTicketStatus | ''>('')
const pageSize = ref(15)
const items = ref<FaultTicketResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// Create dialog
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = ref<FaultTicketRequest>({ source: 'CITIZEN_REPORT' })

const createRules = computed<FormRules>(() => ({
  source: [{ required: true, message: t('fault.errors.sourceRequired'), trigger: 'change' }],
}))

// Resolve dialog
const resolveVisible = ref(false)
const resolveLoading = ref(false)
const resolveId = ref<number | null>(null)
const resolutionNote = ref('')

const statusLabel = (s: FaultTicketStatus) => {
  const map: Record<FaultTicketStatus, string> = {
    OPEN: t('fault.statusOpen'),
    IN_PROGRESS: t('fault.statusInProgress'),
    RESOLVED: t('fault.statusResolved'),
    MERGED: t('fault.statusMerged'),
  }
  return map[s] ?? s
}

const statusClass = (s: FaultTicketStatus) => {
  const map: Record<FaultTicketStatus, string> = {
    OPEN: 'status-danger',
    IN_PROGRESS: 'status-warning',
    RESOLVED: 'status-success',
    MERGED: 'status-info',
  }
  return map[s] ?? ''
}

const sourceLabel = (s: FaultTicketSource) => {
  const map: Record<FaultTicketSource, string> = {
    CITIZEN_REPORT: t('fault.sourceCitizen'),
    PATROL: t('fault.sourcePatrol'),
    AUTO_ALERT: t('fault.sourceAutoAlert'),
  }
  return map[s] ?? s
}

async function loadData(page = 0) {
  loading.value = true
  try {
    const res = await getFaultTickets({
      page,
      size: pageSize.value,
      status: filterStatus.value || undefined,
      keyword: keyword.value || undefined,
    })
    items.value = res.body.content
    pagination.value = {
      page: res.body.page,
      totalElements: res.body.totalElements,
      totalPages: res.body.totalPages,
    }
  } catch {
    ElMessage.error(t('fault.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleSearch() { loadData(0) }
function handlePageChange(p: number) { loadData(p - 1) }
function handleSizeChange(s: number) { pageSize.value = s; loadData(0) }

function openCreateDialog() {
  createForm.value = { source: 'CITIZEN_REPORT' }
  createVisible.value = true
}

async function handleCreate() {
  const form = createFormRef.value
  if (!form) return
  const valid = await form.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    await createFaultTicket(createForm.value)
    ElMessage.success(t('fault.createdSuccess'))
    createVisible.value = false
    loadData(pagination.value.page)
  } catch {
    ElMessage.error(t('fault.loadFailed'))
  } finally {
    createLoading.value = false
  }
}

function openResolveDialog(row: FaultTicketResponse) {
  resolveId.value = row.id
  resolutionNote.value = ''
  resolveVisible.value = true
}

async function handleResolve() {
  if (!resolveId.value) return
  resolveLoading.value = true
  try {
    await resolveFaultTicket(resolveId.value, resolutionNote.value || undefined)
    ElMessage.success(t('fault.resolvedSuccess'))
    resolveVisible.value = false
    loadData(pagination.value.page)
  } catch {
    ElMessage.error(t('fault.loadFailed'))
  } finally {
    resolveLoading.value = false
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <div class="page-content">
      <div class="page-header">
        <div class="header-left">
          <div class="header-icon"><AlertTriangle :size="20" /></div>
          <div>
            <h2 class="header-title">{{ t('fault.title') }}</h2>
            <p class="header-subtitle">{{ t('fault.subtitle') }}</p>
          </div>
        </div>
        <el-button class="create-btn" @click="openCreateDialog">
          <Plus :size="16" style="margin-right: 6px" /> {{ t('fault.addBtn') }}
        </el-button>
      </div>

      <div class="filter-bar">
        <el-select v-model="filterStatus" :placeholder="t('fault.filterStatus')" clearable style="width: 160px" @change="handleSearch">
          <el-option value="OPEN" :label="t('fault.statusOpen')" />
          <el-option value="IN_PROGRESS" :label="t('fault.statusInProgress')" />
          <el-option value="RESOLVED" :label="t('fault.statusResolved')" />
          <el-option value="MERGED" :label="t('fault.statusMerged')" />
        </el-select>
        <el-input v-model="keyword" :placeholder="t('fault.searchPlaceholder')" clearable style="width: 280px" @keyup.enter="handleSearch" />
        <el-button class="search-btn" @click="handleSearch"><Search :size="16" /></el-button>
      </div>

      <div class="table-card" v-loading="loading">
        <el-table :data="items" stripe>
          <el-table-column prop="id" :label="t('fault.colId')" width="100" />
          <el-table-column :label="t('fault.colSource')" width="120">
            <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
          </el-table-column>
          <el-table-column :label="t('fault.colStatus')" width="100">
            <template #default="{ row }">
              <span class="status-badge" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="priority" :label="t('fault.colPriority')" width="90" />
          <el-table-column prop="description" :label="t('fault.colDescription')" min-width="200" show-overflow-tooltip />
          <el-table-column prop="reportedBy" :label="t('fault.colReportedBy')" width="120" />
          <el-table-column prop="reportedAt" :label="t('fault.colReportedAt')" width="160" />
          <el-table-column prop="resolvedAt" :label="t('fault.colResolvedAt')" width="160" />
          <el-table-column :label="t('common.actions')" width="90" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'OPEN' || row.status === 'IN_PROGRESS'"
                class="action-btn action-resolve"
                size="small"
                @click="openResolveDialog(row)"
              >
                <CheckCircle :size="14" />
              </el-button>
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

    <!-- Create Dialog -->
    <el-dialog v-model="createVisible" :title="t('fault.dialogCreateTitle')" width="520px" class="dark-dialog">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top" v-loading="createLoading">
        <el-form-item :label="t('fault.colSource')" prop="source">
          <el-select v-model="createForm.source" style="width: 100%">
            <el-option value="CITIZEN_REPORT" :label="t('fault.sourceCitizen')" />
            <el-option value="PATROL" :label="t('fault.sourcePatrol')" />
            <el-option value="AUTO_ALERT" :label="t('fault.sourceAutoAlert')" />
          </el-select>
        </el-form-item>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px">
          <el-form-item :label="t('fault.colDevice')">
            <el-input v-model.number="createForm.deviceId" type="number" />
          </el-form-item>
          <el-form-item :label="t('fault.colCircuit')">
            <el-input v-model.number="createForm.circuitId" type="number" />
          </el-form-item>
        </div>
        <el-form-item :label="t('fault.colPriority')">
          <el-select v-model="createForm.priority" style="width: 100%">
            <el-option value="HIGH" :label="t('fault.priorityHigh')" />
            <el-option value="NORMAL" :label="t('fault.priorityNormal')" />
            <el-option value="LOW" :label="t('fault.priorityLow')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('fault.colDescription')">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button class="submit-btn" @click="handleCreate" :loading="createLoading">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- Resolve Dialog -->
    <el-dialog v-model="resolveVisible" :title="t('fault.resolveTitle')" width="480px" class="dark-dialog">
      <p style="color: var(--text-secondary); margin-bottom: 16px">{{ t('fault.resolveConfirm') }}</p>
      <el-input v-model="resolutionNote" type="textarea" :rows="3" :placeholder="t('fault.resolutionNotePlaceholder')" />
      <template #footer>
        <el-button class="cancel-btn" @click="resolveVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button class="submit-btn" @click="handleResolve" :loading="resolveLoading">{{ t('fault.resolveBtn') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }
.page-content {}
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon {
  width: 40px; height: 40px;
  background: rgba(255, 99, 99, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #FF6363;
}
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); font-family: 'Inter', sans-serif; margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); font-family: 'Inter', sans-serif; margin: 2px 0 0; }
.create-btn { background: var(--btn-primary-bg); color: var(--btn-primary-text); border: none; border-radius: 86px; padding: 10px 20px; font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 600; }
.create-btn:hover { background: var(--btn-primary-hover); color: var(--btn-primary-text); }
.filter-bar { display: flex; gap: 10px; align-items: center; margin-bottom: 16px; }
.search-btn { background: var(--btn-primary-bg); color: var(--btn-primary-text); border: none; border-radius: 8px; padding: 8px 14px; }
.search-btn:hover { background: var(--btn-primary-hover); color: var(--btn-primary-text); }
.table-card { background: var(--bg-surface); border: 1px solid var(--bg-active); border-radius: 12px; overflow: hidden; }

.status-badge { display: inline-block; padding: 2px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-warning { background: rgba(255, 188, 51, 0.15); color: #ffbc33; }
.status-danger { background: rgba(255, 99, 99, 0.15); color: #FF6363; }
.status-info { background: rgba(85, 179, 255, 0.15); color: #55b3ff; }

.action-btn { padding: 4px 8px; min-width: auto; background: transparent; border-radius: 6px; }
.action-resolve { color: #5fc992; border: 1px solid rgba(95, 201, 146, 0.2); }
.action-resolve:hover { background: rgba(95, 201, 146, 0.15); color: #5fc992; }

.pagination-row { display: flex; justify-content: flex-end; padding: 16px 20px; border-top: 1px solid var(--bg-active); }
.cancel-btn { background: transparent; color: var(--text-secondary); border: none; border-radius: 6px; padding: 8px 16px; font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 600; }
.cancel-btn:hover { opacity: 0.6; color: var(--text-primary); }
.submit-btn { background: var(--btn-primary-bg); color: var(--btn-primary-text); border: none; border-radius: 86px; padding: 8px 24px; font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 600; }
.submit-btn:hover { background: var(--btn-primary-hover); color: var(--btn-primary-text); }

:deep(.el-table) {
  --el-table-bg-color: var(--bg-surface); --el-table-tr-bg-color: var(--bg-surface);
  --el-table-header-bg-color: var(--bg-surface); --el-table-row-hover-bg-color: var(--bg-hover-subtle);
  --el-table-text-color: var(--text-primary); --el-table-header-text-color: var(--text-secondary);
  --el-table-border-color: var(--bg-active); font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500;
}
:deep(.el-table th.el-table__cell) { font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px; }
:deep(.el-input__wrapper) { background-color: var(--bg-base); border: 1px solid var(--border-medium); border-radius: 8px; box-shadow: none; }
:deep(.el-input__wrapper:hover) { border-color: var(--border-strong); }
:deep(.el-input__wrapper.is-focus) { border-color: rgba(85, 179, 255, 0.5); box-shadow: 0 0 0 3px rgba(85, 179, 255, 0.15); }
:deep(.el-input__inner) { color: var(--text-primary); font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500; }
:deep(.el-input__inner::placeholder) { color: var(--text-muted); }
:deep(.el-form-item__label) { color: var(--text-label); font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500; }
:deep(.el-select .el-input__wrapper) { background-color: var(--bg-base); border: 1px solid var(--border-medium); border-radius: 8px; box-shadow: none; }
:deep(.el-pagination) { --el-pagination-bg-color: transparent; --el-pagination-text-color: var(--text-secondary); --el-pagination-button-bg-color: transparent; --el-pagination-hover-color: #55b3ff; }
</style>
