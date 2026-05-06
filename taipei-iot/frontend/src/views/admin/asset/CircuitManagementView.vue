<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getCircuits, createCircuit, updateCircuit, deleteCircuit } from '@/api/circuit'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { CircuitResponse, CircuitRequest } from '@/types/circuit'
import { Search, Plus, Pencil, Trash2, Zap } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const filterStatus = ref('')
const pageSize = ref(20)

const circuits = ref<CircuitResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// Dialog
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const dialogLoading = ref(false)
const dialogFormRef = ref<FormInstance>()
const dialogEditId = ref<number | null>(null)
const dialogForm = ref<CircuitRequest>({
  circuitNumber: '',
  circuitName: '',
  taipowerAccount: '',
  usageType: 'LIGHTING',
  status: 'ACTIVE',
})

const dialogRules = computed<FormRules>(() => ({
  circuitNumber: [{ required: true, message: t('circuit.errors.numberRequired'), trigger: 'blur' }],
}))

const statusOptions = [
  { value: 'ACTIVE', label: '啟用' },
  { value: 'INACTIVE', label: '停用' },
]

const usageTypeOptions = [
  { value: 'LIGHTING', label: '照明' },
  { value: 'TRAFFIC_SIGNAL', label: '交通號誌' },
  { value: 'OTHER', label: '其他' },
]

function getStatusLabel(status: string) {
  return statusOptions.find(o => o.value === status)?.label ?? status
}

function getStatusType(status: string) {
  return status === 'ACTIVE' ? 'success' : 'danger'
}

function getUsageLabel(type: string | null) {
  return usageTypeOptions.find(o => o.value === type)?.label ?? type ?? '-'
}

async function loadCircuits(page = 0) {
  loading.value = true
  try {
    const res = await getCircuits({
      keyword: keyword.value || undefined,
      status: filterStatus.value || undefined,
      page,
      size: pageSize.value,
    })
    circuits.value = res.body.content
    pagination.value = {
      page: res.body.page,
      totalElements: res.body.totalElements,
      totalPages: res.body.totalPages,
    }
  } catch {
    ElMessage.error(t('circuit.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleSearch() { loadCircuits(0) }
function handlePageChange(page: number) { loadCircuits(page - 1) }
function handleSizeChange(size: number) {
  pageSize.value = size
  loadCircuits(0)
}

function openCreateDialog() {
  dialogMode.value = 'create'
  dialogEditId.value = null
  dialogForm.value = {
    circuitNumber: '',
    circuitName: '',
    taipowerAccount: '',
    usageType: 'LIGHTING',
    status: 'ACTIVE',
  }
  dialogVisible.value = true
}

function openEditDialog(row: CircuitResponse) {
  dialogMode.value = 'edit'
  dialogEditId.value = row.id
  dialogForm.value = {
    circuitNumber: row.circuitNumber,
    circuitName: row.circuitName ?? '',
    taipowerAccount: row.taipowerAccount ?? '',
    usageType: row.usageType ?? 'LIGHTING',
    status: row.status,
    panelBoxDeviceId: row.panelBoxDeviceId ?? undefined,
  }
  dialogVisible.value = true
}

async function handleDialogSubmit() {
  const valid = await dialogFormRef.value?.validate().catch(() => false)
  if (!valid) return
  dialogLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createCircuit(dialogForm.value)
      ElMessage.success(t('circuit.createdSuccess'))
    } else {
      await updateCircuit(dialogEditId.value!, dialogForm.value)
      ElMessage.success(t('circuit.updatedSuccess'))
    }
    dialogVisible.value = false
    await loadCircuits(pagination.value.page)
  } catch (err: unknown) {
    const error = err as { response?: { data?: { errorCode?: string } } }
    const code = error?.response?.data?.errorCode
    const msg = code ? t(`circuit.errors.${code}`, code) : t('common.operationFailed')
    ElMessage.error(msg)
  } finally {
    dialogLoading.value = false
  }
}

async function handleDelete(row: CircuitResponse) {
  try {
    await ElMessageBox.confirm(
      t('circuit.deleteConfirm', { number: row.circuitNumber }),
      t('common.confirmDelete'),
      { confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'), type: 'warning' },
    )
  } catch { return }

  try {
    await deleteCircuit(row.id)
    ElMessage.success(t('circuit.deletedSuccess'))
    await loadCircuits(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  }
}

onMounted(() => { loadCircuits() })
</script>

<template>
  <div class="page-container">
    <div class="page-content">
      <!-- Header -->
      <div class="page-header">
        <div class="header-left">
          <Zap :size="24" class="header-icon" />
          <div>
            <h1 class="page-title">{{ t('circuit.title') }}</h1>
            <p class="page-subtitle">{{ t('circuit.subtitle') }}</p>
          </div>
        </div>
        <el-button class="create-btn" @click="openCreateDialog()">
          <Plus :size="16" style="margin-right: 6px" />
          {{ t('circuit.addBtn') }}
        </el-button>
      </div>

      <!-- Filter bar -->
      <div class="filter-bar">
        <el-select v-model="filterStatus" :placeholder="t('circuit.filterStatus')" clearable style="width: 120px" @change="handleSearch">
          <el-option v-for="opt in statusOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
        </el-select>
        <el-input v-model="keyword" :placeholder="t('circuit.searchPlaceholder')" clearable style="width: 280px"
                  @keyup.enter="handleSearch" @clear="handleSearch">
          <template #prefix><Search :size="16" class="input-icon" /></template>
        </el-input>
        <el-button class="search-btn" @click="handleSearch">{{ t('common.search') }}</el-button>
      </div>

      <!-- Table -->
      <div class="table-card" v-loading="loading">
        <el-table :data="circuits" style="width: 100%">
          <el-table-column prop="circuitNumber" :label="t('circuit.colNumber')" min-width="140">
            <template #default="{ row }">
              <span class="circuit-code">{{ row.circuitNumber }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="circuitName" :label="t('circuit.colName')" min-width="200" />
          <el-table-column prop="taipowerAccount" :label="t('circuit.colTaipower')" min-width="140">
            <template #default="{ row }">
              <span class="mono-text">{{ row.taipowerAccount || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="usageType" :label="t('circuit.colUsageType')" width="120" align="center">
            <template #default="{ row }">
              {{ getUsageLabel(row.usageType) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="t('circuit.colStatus')" width="100" align="center">
            <template #default="{ row }">
              <span class="status-badge" :class="'status-' + getStatusType(row.status)">
                {{ getStatusLabel(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.actions')" width="120" fixed="right">
            <template #default="{ row }">
              <div class="action-group">
                <el-button class="action-btn action-edit" size="small" @click="openEditDialog(row)">
                  <Pencil :size="14" />
                </el-button>
                <el-button class="action-btn action-delete" size="small" @click="handleDelete(row)">
                  <Trash2 :size="14" />
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row" v-if="pagination.totalElements > 0">
          <el-pagination
            background layout="total, sizes, prev, pager, next"
            :total="pagination.totalElements"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :current-page="pagination.page + 1"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? t('circuit.dialogCreateTitle') : t('circuit.dialogEditTitle')"
      width="500px" :close-on-click-modal="false" class="dark-dialog"
    >
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-position="top"
               @submit.prevent="handleDialogSubmit">
        <el-form-item :label="t('circuit.colNumber')" prop="circuitNumber">
          <el-input v-model="dialogForm.circuitNumber" placeholder="CKT-N-A" />
        </el-form-item>
        <el-form-item :label="t('circuit.colName')">
          <el-input v-model="dialogForm.circuitName" :placeholder="t('circuit.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('circuit.colTaipower')">
          <el-input v-model="dialogForm.taipowerAccount" placeholder="01-1001-0001" />
        </el-form-item>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px;">
          <el-form-item :label="t('circuit.colUsageType')">
            <el-select v-model="dialogForm.usageType" style="width: 100%">
              <el-option v-for="opt in usageTypeOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('circuit.colStatus')">
            <el-select v-model="dialogForm.status" style="width: 100%">
              <el-option v-for="opt in statusOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button class="submit-btn" :loading="dialogLoading" @click="handleDialogSubmit">
          {{ dialogMode === 'create' ? t('common.add') : t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 32px 24px;
  min-height: 100vh;
  background-color: var(--bg-base);
}

.page-content {
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon { color: #ffbc33; }

.page-title {
  font-family: 'Inter', sans-serif;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.15;
  color: var(--text-heading);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  margin: 0;
}

.create-btn {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none;
  border-radius: 86px;
  padding: 8px 24px;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.create-btn:hover {
  background: var(--btn-primary-hover);
  color: var(--btn-primary-text);
}

.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-btn {
  background: transparent;
  color: var(--text-primary);
  border: 1px solid var(--border-light);
  border-radius: 6px;
  padding: 8px 16px;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
}

.search-btn:hover { opacity: 0.6; }
.input-icon { color: var(--text-muted); }

.table-card {
  background-color: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.circuit-code {
  font-weight: 600;
  color: #55b3ff;
  font-size: 13px;
}

.mono-text { font-family: 'JetBrains Mono', monospace; font-size: 13px; }

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-danger { background: rgba(255, 99, 99, 0.15); color: #FF6363; }

.action-group { display: flex; gap: 6px; justify-content: center; }

.action-btn {
  padding: 4px 8px;
  min-width: auto;
  background: transparent;
  border-radius: 6px;
}

.action-edit { color: #ffbc33; border: 1px solid rgba(255, 188, 51, 0.2); }
.action-edit:hover { background: rgba(255, 188, 51, 0.15); color: #ffbc33; }
.action-delete { color: #FF6363; border: 1px solid rgba(255, 99, 99, 0.2); }
.action-delete:hover { background: rgba(255, 99, 99, 0.15); color: #FF6363; }

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--bg-active);
}

/* Dialog buttons */
.cancel-btn {
  background: transparent;
  color: var(--text-secondary);
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
}

.cancel-btn:hover { opacity: 0.6; color: var(--text-primary); }

.submit-btn {
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none;
  border-radius: 86px;
  padding: 8px 24px;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
}

.submit-btn:hover {
  background: var(--btn-primary-hover);
  color: var(--btn-primary-text);
}

/* Element Plus dark overrides */
:deep(.el-table) {
  --el-table-bg-color: var(--bg-surface);
  --el-table-tr-bg-color: var(--bg-surface);
  --el-table-header-bg-color: var(--bg-surface);
  --el-table-row-hover-bg-color: var(--bg-hover-subtle);
  --el-table-text-color: var(--text-primary);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-border-color: var(--bg-active);
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
}

:deep(.el-table th.el-table__cell) {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

:deep(.el-input__wrapper) {
  background-color: var(--bg-base);
  border: 1px solid var(--border-medium);
  border-radius: 8px;
  box-shadow: none;
}

:deep(.el-input__wrapper:hover) { border-color: var(--border-strong); }

:deep(.el-input__wrapper.is-focus) {
  border-color: rgba(85, 179, 255, 0.5);
  box-shadow: 0 0 0 3px rgba(85, 179, 255, 0.15);
}

:deep(.el-input__inner) {
  color: var(--text-primary);
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
}

:deep(.el-input__inner::placeholder) { color: var(--text-muted); }

:deep(.el-form-item__label) {
  color: var(--text-label);
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
}

:deep(.el-select .el-input__wrapper) {
  background-color: var(--bg-base);
  border: 1px solid var(--border-medium);
  border-radius: 8px;
  box-shadow: none;
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: var(--text-secondary);
  --el-pagination-button-bg-color: transparent;
  --el-pagination-hover-color: #55b3ff;
}
</style>
