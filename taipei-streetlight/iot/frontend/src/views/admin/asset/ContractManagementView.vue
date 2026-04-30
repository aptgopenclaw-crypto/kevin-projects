<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { FileText, Plus, Search, Pencil, Trash2 } from 'lucide-vue-next'
import {
  getContracts,
  getContractById,
  createContract,
  updateContract,
  deleteContract,
} from '@/api/contract'
import type { ContractResponse, ContractRequest, ContractStatus } from '@/types/contract'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const filterStatus = ref<ContractStatus | ''>('')
const pageSize = ref(15)
const items = ref<ContractResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const dialogLoading = ref(false)
const dialogFormRef = ref<FormInstance>()
const dialogEditId = ref<number | null>(null)
const dialogForm = ref<ContractRequest>({
  contractCode: '',
  contractName: '',
  budgetYear: undefined,
  procurementNumber: '',
  contractorName: '',
  contractorContact: '',
  assetCategory: '',
  quantity: undefined,
  startDate: '',
  endDate: '',
  acceptanceDate: '',
  warrantyYears: undefined,
  warrantyExpiry: '',
  status: 'ACTIVE',
})

const dialogRules = computed<FormRules>(() => ({
  contractCode: [{ required: true, message: t('contract.errors.codeRequired'), trigger: 'blur' }],
  contractName: [{ required: true, message: t('contract.errors.nameRequired'), trigger: 'blur' }],
}))

const statusLabel = (s: ContractStatus) => {
  const map: Record<ContractStatus, string> = {
    ACTIVE: t('contract.statusActive'),
    EXPIRED: t('contract.statusExpired'),
    TERMINATED: t('contract.statusTerminated'),
  }
  return map[s] ?? s
}

const statusClass = (s: ContractStatus) => {
  const map: Record<ContractStatus, string> = {
    ACTIVE: 'status-success',
    EXPIRED: 'status-warning',
    TERMINATED: 'status-danger',
  }
  return map[s] ?? ''
}

async function loadData(page = 0) {
  loading.value = true
  try {
    const res = await getContracts({
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
    ElMessage.error(t('contract.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleSearch() { loadData(0) }
function handlePageChange(p: number) { loadData(p - 1) }
function handleSizeChange(s: number) { pageSize.value = s; loadData(0) }

function resetForm() {
  dialogForm.value = {
    contractCode: '', contractName: '', budgetYear: undefined,
    procurementNumber: '', contractorName: '', contractorContact: '',
    assetCategory: '', quantity: undefined, startDate: '', endDate: '',
    acceptanceDate: '', warrantyYears: undefined, warrantyExpiry: '', status: 'ACTIVE',
  }
}

function openCreateDialog() {
  dialogMode.value = 'create'
  dialogEditId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEditDialog(row: ContractResponse) {
  dialogMode.value = 'edit'
  dialogEditId.value = row.id
  dialogLoading.value = true
  dialogVisible.value = true
  try {
    const res = await getContractById(row.id)
    const d = res.body
    dialogForm.value = {
      contractCode: d.contractCode,
      contractName: d.contractName,
      budgetYear: d.budgetYear ?? undefined,
      procurementNumber: d.procurementNumber ?? '',
      contractorName: d.contractorName ?? '',
      contractorContact: d.contractorContact ?? '',
      assetCategory: d.assetCategory ?? '',
      quantity: d.quantity ?? undefined,
      startDate: d.startDate ?? '',
      endDate: d.endDate ?? '',
      acceptanceDate: d.acceptanceDate ?? '',
      warrantyYears: d.warrantyYears ?? undefined,
      warrantyExpiry: d.warrantyExpiry ?? '',
      status: d.status,
    }
  } catch {
    ElMessage.error(t('contract.loadFailed'))
  } finally {
    dialogLoading.value = false
  }
}

async function handleDialogSubmit() {
  const form = dialogFormRef.value
  if (!form) return
  const valid = await form.validate().catch(() => false)
  if (!valid) return
  dialogLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createContract(dialogForm.value)
      ElMessage.success(t('contract.createdSuccess'))
    } else {
      await updateContract(dialogEditId.value!, dialogForm.value)
      ElMessage.success(t('contract.updatedSuccess'))
    }
    dialogVisible.value = false
    loadData(pagination.value.page)
  } catch {
    ElMessage.error(t('contract.loadFailed'))
  } finally {
    dialogLoading.value = false
  }
}

async function handleDelete(row: ContractResponse) {
  try {
    await ElMessageBox.confirm(
      t('contract.deleteConfirm', { code: row.contractCode }),
      { type: 'warning' },
    )
    await deleteContract(row.id)
    ElMessage.success(t('contract.deletedSuccess'))
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
          <div class="header-icon"><FileText :size="20" /></div>
          <div>
            <h2 class="header-title">{{ t('contract.title') }}</h2>
            <p class="header-subtitle">{{ t('contract.subtitle') }}</p>
          </div>
        </div>
        <el-button class="create-btn" @click="openCreateDialog">
          <Plus :size="16" style="margin-right: 6px" /> {{ t('contract.addBtn') }}
        </el-button>
      </div>

      <div class="filter-bar">
        <el-select v-model="filterStatus" :placeholder="t('contract.filterStatus')" clearable style="width: 160px" @change="handleSearch">
          <el-option value="ACTIVE" :label="t('contract.statusActive')" />
          <el-option value="EXPIRED" :label="t('contract.statusExpired')" />
          <el-option value="TERMINATED" :label="t('contract.statusTerminated')" />
        </el-select>
        <el-input v-model="keyword" :placeholder="t('contract.searchPlaceholder')" clearable style="width: 280px" @keyup.enter="handleSearch" />
        <el-button class="search-btn" @click="handleSearch"><Search :size="16" /></el-button>
      </div>

      <div class="table-card" v-loading="loading">
        <el-table :data="items" stripe>
          <el-table-column prop="contractCode" :label="t('contract.colCode')" width="140">
            <template #default="{ row }"><span class="code-text">{{ row.contractCode }}</span></template>
          </el-table-column>
          <el-table-column prop="contractName" :label="t('contract.colName')" min-width="180" />
          <el-table-column prop="contractorName" :label="t('contract.colContractor')" width="140" />
          <el-table-column prop="budgetYear" :label="t('contract.colBudgetYear')" width="100" />
          <el-table-column prop="startDate" :label="t('contract.colStartDate')" width="120" />
          <el-table-column prop="endDate" :label="t('contract.colEndDate')" width="120" />
          <el-table-column prop="warrantyExpiry" :label="t('contract.colWarrantyExpiry')" width="120" />
          <el-table-column :label="t('contract.colStatus')" width="100">
            <template #default="{ row }">
              <span class="status-badge" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.actions')" width="120" fixed="right">
            <template #default="{ row }">
              <el-button class="action-btn action-edit" size="small" @click="openEditDialog(row)"><Pencil :size="14" /></el-button>
              <el-button class="action-btn action-delete" size="small" @click="handleDelete(row)"><Trash2 :size="14" /></el-button>
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

    <!-- Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? t('contract.dialogCreateTitle') : t('contract.dialogEditTitle')"
      width="640px"
      class="dark-dialog"
    >
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-position="top" v-loading="dialogLoading">
        <el-form-item :label="t('contract.colCode')" prop="contractCode">
          <el-input v-model="dialogForm.contractCode" :placeholder="t('contract.codePlaceholder')" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item :label="t('contract.colName')" prop="contractName">
          <el-input v-model="dialogForm.contractName" :placeholder="t('contract.namePlaceholder')" />
        </el-form-item>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px">
          <el-form-item :label="t('contract.colContractor')">
            <el-input v-model="dialogForm.contractorName" />
          </el-form-item>
          <el-form-item :label="t('contract.colContractorContact')">
            <el-input v-model="dialogForm.contractorContact" />
          </el-form-item>
          <el-form-item :label="t('contract.colBudgetYear')">
            <el-input v-model.number="dialogForm.budgetYear" type="number" />
          </el-form-item>
          <el-form-item :label="t('contract.colProcurementNo')">
            <el-input v-model="dialogForm.procurementNumber" />
          </el-form-item>
          <el-form-item :label="t('contract.colStartDate')">
            <el-date-picker v-model="dialogForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('contract.colEndDate')">
            <el-date-picker v-model="dialogForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('contract.colAcceptanceDate')">
            <el-date-picker v-model="dialogForm.acceptanceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('contract.colWarrantyYears')">
            <el-input v-model.number="dialogForm.warrantyYears" type="number" />
          </el-form-item>
          <el-form-item :label="t('contract.colWarrantyExpiry')">
            <el-date-picker v-model="dialogForm.warrantyExpiry" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('contract.colAssetCategory')">
            <el-input v-model="dialogForm.assetCategory" />
          </el-form-item>
          <el-form-item :label="t('contract.colQuantity')">
            <el-input v-model.number="dialogForm.quantity" type="number" />
          </el-form-item>
          <el-form-item :label="t('contract.colStatus')">
            <el-select v-model="dialogForm.status" style="width: 100%">
              <el-option value="ACTIVE" :label="t('contract.statusActive')" />
              <el-option value="EXPIRED" :label="t('contract.statusExpired')" />
              <el-option value="TERMINATED" :label="t('contract.statusTerminated')" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button class="submit-btn" @click="handleDialogSubmit" :loading="dialogLoading">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }

.page-content {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left { display: flex; align-items: center; gap: 12px; }

.header-icon {
  width: 40px; height: 40px;
  background: rgba(255, 188, 51, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #ffbc33;
}

.header-title {
  font-size: 20px; font-weight: 700;
  color: var(--text-heading);
  font-family: 'Inter', sans-serif;
  margin: 0;
}

.header-subtitle {
  font-size: 13px; color: var(--text-secondary);
  font-family: 'Inter', sans-serif;
  margin: 2px 0 0;
}

.create-btn {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none; border-radius: 86px;
  padding: 10px 20px;
  font-family: 'Inter', sans-serif;
  font-size: 14px; font-weight: 600;
}
.create-btn:hover { background: var(--btn-primary-hover); color: var(--btn-primary-text); }

.filter-bar {
  display: flex; gap: 10px; align-items: center;
  margin-bottom: 16px;
}

.search-btn {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none; border-radius: 8px;
  padding: 8px 14px;
}
.search-btn:hover { background: var(--btn-primary-hover); color: var(--btn-primary-text); }

.table-card {
  background: var(--bg-surface);
  border: 1px solid var(--bg-active);
  border-radius: 12px;
  overflow: hidden;
}

.code-text { color: #55b3ff; font-weight: 600; font-family: 'JetBrains Mono', monospace; font-size: 13px; }

.status-badge {
  display: inline-block; padding: 2px 10px;
  border-radius: 6px; font-size: 12px; font-weight: 600;
}
.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-warning { background: rgba(255, 188, 51, 0.15); color: #ffbc33; }
.status-danger { background: rgba(255, 99, 99, 0.15); color: #FF6363; }

.action-btn {
  padding: 4px 8px; min-width: auto;
  background: transparent; border-radius: 6px;
}
.action-edit { color: #ffbc33; border: 1px solid rgba(255, 188, 51, 0.2); }
.action-edit:hover { background: rgba(255, 188, 51, 0.15); color: #ffbc33; }
.action-delete { color: #FF6363; border: 1px solid rgba(255, 99, 99, 0.2); }
.action-delete:hover { background: rgba(255, 99, 99, 0.15); color: #FF6363; }

.pagination-row {
  display: flex; justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--bg-active);
}

.cancel-btn {
  background: transparent; color: var(--text-secondary);
  border: none; border-radius: 6px; padding: 8px 16px;
  font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 600;
}
.cancel-btn:hover { opacity: 0.6; color: var(--text-primary); }

.submit-btn {
  background: var(--btn-primary-bg); color: var(--btn-primary-text);
  border: none; border-radius: 86px; padding: 8px 24px;
  font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 600;
}
.submit-btn:hover { background: var(--btn-primary-hover); color: var(--btn-primary-text); }

:deep(.el-table) {
  --el-table-bg-color: var(--bg-surface);
  --el-table-tr-bg-color: var(--bg-surface);
  --el-table-header-bg-color: var(--bg-surface);
  --el-table-row-hover-bg-color: var(--bg-hover-subtle);
  --el-table-text-color: var(--text-primary);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-border-color: var(--bg-active);
  font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500;
}
:deep(.el-table th.el-table__cell) {
  font-size: 12px; font-weight: 600;
  text-transform: uppercase; letter-spacing: 0.3px;
}
:deep(.el-input__wrapper) {
  background-color: var(--bg-base);
  border: 1px solid var(--border-medium);
  border-radius: 8px; box-shadow: none;
}
:deep(.el-input__wrapper:hover) { border-color: var(--border-strong); }
:deep(.el-input__wrapper.is-focus) {
  border-color: rgba(85, 179, 255, 0.5);
  box-shadow: 0 0 0 3px rgba(85, 179, 255, 0.15);
}
:deep(.el-input__inner) {
  color: var(--text-primary);
  font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500;
}
:deep(.el-input__inner::placeholder) { color: var(--text-muted); }
:deep(.el-form-item__label) {
  color: var(--text-label);
  font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500;
}
:deep(.el-select .el-input__wrapper) {
  background-color: var(--bg-base);
  border: 1px solid var(--border-medium);
  border-radius: 8px; box-shadow: none;
}
:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: var(--text-secondary);
  --el-pagination-button-bg-color: transparent;
  --el-pagination-hover-color: #55b3ff;
}
</style>
