<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PackageMinus, Plus, Check, X } from 'lucide-vue-next'
import {
  getIssueRequests,
  createIssueRequest,
  approveIssueRequest,
  rejectIssueRequest,
  issueFromRequest,
} from '@/api/material'
import type {
  IssueRequestResponse,
  IssueRequestRequest,
  IssueRequestStatus,
  IssueRecordRequest,
} from '@/types/material'
import { formatDateTime } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const filterStatus = ref<IssueRequestStatus | ''>('')
const pageSize = ref(15)
const items = ref<IssueRequestResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// Create dialog
const createVisible = ref(false)
const createLoading = ref(false)
const createForm = ref<IssueRequestRequest>({})

// Issue dialog
const issueVisible = ref(false)
const issueLoading = ref(false)
const issueRequestId = ref<number | null>(null)
const issueRecords = ref<IssueRecordRequest[]>([{ inventoryId: 0, materialSpecId: 0, quantity: 1 }])

const statusOptions: { value: IssueRequestStatus; label: string }[] = [
  { value: 'PENDING', label: '待審核' },
  { value: 'APPROVED', label: '已核准' },
  { value: 'ISSUED', label: '已出料' },
  { value: 'REJECTED', label: '已駁回' },
]

const statusLabel = (s: IssueRequestStatus) =>
  statusOptions.find(o => o.value === s)?.label ?? s

const statusClass = (s: IssueRequestStatus) => {
  const map: Record<string, string> = {
    PENDING: 'status-warning',
    APPROVED: 'status-primary',
    ISSUED: 'status-success',
    REJECTED: 'status-danger',
  }
  return map[s] ?? ''
}

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getIssueRequests({
      status: filterStatus.value || undefined,
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
    ElMessage.error(t('material.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) {
  fetchData(p - 1)
}

function openCreate() {
  createForm.value = {}
  createVisible.value = true
}

async function handleCreate() {
  createLoading.value = true
  try {
    await createIssueRequest(createForm.value)
    ElMessage.success(t('material.createdSuccess'))
    createVisible.value = false
    fetchData(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    createLoading.value = false
  }
}

async function handleApprove(row: IssueRequestResponse) {
  try {
    await approveIssueRequest(row.id)
    ElMessage.success(t('material.approvedSuccess'))
    fetchData(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  }
}

async function handleReject(row: IssueRequestResponse) {
  try {
    await ElMessageBox.confirm(t('material.rejectConfirm'), t('common.confirm'), { type: 'warning' })
    await rejectIssueRequest(row.id)
    ElMessage.success(t('material.rejectedSuccess'))
    fetchData(pagination.value.page)
  } catch {
    // cancelled
  }
}

function openIssue(row: IssueRequestResponse) {
  issueRequestId.value = row.id
  issueRecords.value = [{ inventoryId: 0, materialSpecId: 0, quantity: 1 }]
  issueVisible.value = true
}

function addIssueRecord() {
  issueRecords.value.push({ inventoryId: 0, materialSpecId: 0, quantity: 1 })
}

function removeIssueRecord(index: number) {
  issueRecords.value.splice(index, 1)
}

async function handleIssue() {
  if (!issueRequestId.value) return
  issueLoading.value = true
  try {
    await issueFromRequest(issueRequestId.value, issueRecords.value)
    ElMessage.success(t('material.issuedSuccess'))
    issueVisible.value = false
    fetchData(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    issueLoading.value = false
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <PackageMinus :size="22" />
        <h2>{{ t('material.issueTitle') }}</h2>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="16" />
        {{ t('material.newIssueRequest') }}
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="filterStatus" :placeholder="t('common.status')" clearable @change="() => fetchData()">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button @click="fetchData()">{{ t('common.query') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="requestNumber" :label="t('material.requestNumber')" width="180" />
      <el-table-column prop="requestedBy" :label="t('material.requestedBy')" width="120" />
      <el-table-column prop="repairTicketId" :label="t('material.repairTicketId')" width="120" />
      <el-table-column :label="t('common.status')" width="100">
        <template #default="{ row }">
          <span :class="['status-tag', statusClass(row.status)]">{{ statusLabel(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.createTime')" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="220" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button link type="success" @click="handleApprove(row)">
              <Check :size="14" /> {{ t('material.approve') }}
            </el-button>
            <el-button link type="danger" @click="handleReject(row)">
              <X :size="14" /> {{ t('material.reject') }}
            </el-button>
          </template>
          <el-button v-if="row.status === 'APPROVED'" link type="primary" @click="openIssue(row)">
            {{ t('material.issueAction') }}
          </el-button>
        </template>
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

    <!-- Create Dialog -->
    <el-dialog v-model="createVisible" :title="t('material.newIssueRequest')" width="460px" destroy-on-close>
      <el-form label-width="120px">
        <el-form-item :label="t('material.repairTicketId')">
          <el-input-number v-model="createForm.repairTicketId" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- Issue Dialog -->
    <el-dialog v-model="issueVisible" :title="t('material.issueAction')" width="600px" destroy-on-close>
      <div v-for="(rec, idx) in issueRecords" :key="idx" class="item-row">
        <el-input-number v-model="rec.inventoryId" :min="1" :placeholder="t('material.inventoryId')" style="width: 120px" />
        <el-input-number v-model="rec.materialSpecId" :min="1" :placeholder="t('material.specId')" style="width: 120px" />
        <el-input-number v-model="rec.quantity" :min="1" :placeholder="t('material.quantity')" style="width: 100px" />
        <el-button link type="danger" @click="removeIssueRecord(idx)">{{ t('common.delete') }}</el-button>
      </div>
      <el-button style="width: 100%; margin-top: 8px" @click="addIssueRecord">
        <Plus :size="16" /> {{ t('material.addItem') }}
      </el-button>
      <template #footer>
        <el-button @click="issueVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="issueLoading" @click="handleIssue">{{ t('material.confirmIssue') }}</el-button>
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
.item-row { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.status-tag { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-success { background: #f0f9ff; color: #059669; }
.status-warning { background: #fffbeb; color: #d97706; }
.status-primary { background: #eff6ff; color: #2563eb; }
.status-danger { background: #fef2f2; color: #dc2626; }
</style>
