<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ClipboardList, Plus } from 'lucide-vue-next'
import {
  getInspectionTasks,
  createInspectionTask,
  updateInspectionTask,
  deactivateInspectionTask,
} from '@/api/inspection'
import type { InspectionTaskResponse, InspectionTaskRequest, InspectionTaskType, InspectionTaskStatus } from '@/types/repair'

const { t } = useI18n()
const router = useRouter()

// ──────────── List ────────────
const loading = ref(false)
const pageSize = ref(15)
const items = ref<InspectionTaskResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// ──────────── Dialog ────────────
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = ref<InspectionTaskRequest>({ taskName: '', taskType: 'ONE_TIME' })

const rules = computed<FormRules>(() => ({
  taskName: [{ required: true, message: t('inspection.errors.taskNameRequired'), trigger: 'blur' }],
  taskType: [{ required: true, message: t('inspection.errors.taskTypeRequired'), trigger: 'change' }],
}))

// ──────────── Helpers ────────────
const taskTypeLabel = (ty: InspectionTaskType) => {
  const map: Record<InspectionTaskType, string> = {
    ONE_TIME: t('inspection.typeOneTime'),
    RECURRING: t('inspection.typeRecurring'),
  }
  return map[ty] ?? ty
}

const taskStatusLabel = (s: InspectionTaskStatus) => {
  const map: Record<InspectionTaskStatus, string> = {
    ACTIVE: t('common.enabled'),
    INACTIVE: t('common.disabled'),
  }
  return map[s] ?? s
}

const taskStatusClass = (s: InspectionTaskStatus) =>
  s === 'ACTIVE' ? 'status-success' : 'status-info'

// ──────────── Data ────────────
async function loadData(page = 0) {
  loading.value = true
  try {
    const res = await getInspectionTasks({ page, size: pageSize.value })
    items.value = res.body.content
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

function handlePageChange(p: number) { loadData(p - 1) }
function handleSizeChange(s: number) { pageSize.value = s; loadData(0) }

function openCreate() {
  dialogMode.value = 'create'
  editId.value = null
  form.value = { taskName: '', taskType: 'ONE_TIME' }
  dialogVisible.value = true
}

function openEdit(row: InspectionTaskResponse) {
  dialogMode.value = 'edit'
  editId.value = row.id
  form.value = {
    taskName: row.taskName,
    taskType: row.taskType,
    scheduleCron: row.scheduleCron ?? undefined,
    startDate: row.startDate ?? undefined,
    endDate: row.endDate ?? undefined,
    deptId: row.deptId ?? undefined,
    assignedTo: row.assignedTo ?? undefined,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const f = formRef.value
  if (!f) return
  const valid = await f.validate().catch(() => false)
  if (!valid) return
  dialogLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createInspectionTask(form.value)
      ElMessage.success(t('inspection.createdSuccess'))
    } else {
      await updateInspectionTask(editId.value!, form.value)
      ElMessage.success(t('inspection.updatedSuccess'))
    }
    dialogVisible.value = false
    loadData(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    dialogLoading.value = false
  }
}

async function handleDeactivate(row: InspectionTaskResponse) {
  try {
    await ElMessageBox.confirm(t('inspection.deactivateConfirm'), t('common.confirmDelete'), { type: 'warning' })
    await deactivateInspectionTask(row.id)
    ElMessage.success(t('inspection.deactivatedSuccess'))
    loadData(pagination.value.page)
  } catch {
    // cancelled
  }
}

function goRecords(row: InspectionTaskResponse) {
  router.push(`/admin/repair/inspection/${row.id}/records`)
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><ClipboardList :size="20" /></div>
        <div>
          <h2 class="header-title">{{ t('inspection.title') }}</h2>
          <p class="header-subtitle">{{ t('inspection.subtitle') }}</p>
        </div>
      </div>
      <el-button class="create-btn" @click="openCreate">
        <Plus :size="16" style="margin-right: 6px" /> {{ t('inspection.addBtn') }}
      </el-button>
    </div>

    <!-- Table -->
    <div class="table-card" v-loading="loading">
      <el-table :data="items" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskName" :label="t('inspection.colTaskName')" />
        <el-table-column :label="t('inspection.colTaskType')" width="120">
          <template #default="{ row }">{{ taskTypeLabel(row.taskType) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="100">
          <template #default="{ row }">
            <span class="status-badge" :class="taskStatusClass(row.status)">{{ taskStatusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click="goRecords(row)">{{ t('inspection.viewRecords') }}</el-button>
            <el-button size="small" text @click="openEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button v-if="row.status === 'ACTIVE'" size="small" text type="danger" @click="handleDeactivate(row)">{{ t('common.disabled') }}</el-button>
          </template>
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

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? t('inspection.dialogCreateTitle') : t('inspection.dialogEditTitle')" width="520px" class="dark-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('inspection.colTaskName')" prop="taskName">
          <el-input v-model="form.taskName" />
        </el-form-item>
        <el-form-item :label="t('inspection.colTaskType')" prop="taskType">
          <el-select v-model="form.taskType" style="width: 100%">
            <el-option value="ONE_TIME" :label="t('inspection.typeOneTime')" />
            <el-option value="RECURRING" :label="t('inspection.typeRecurring')" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.taskType === 'RECURRING'" :label="t('inspection.scheduleCron')">
          <el-input v-model="form.scheduleCron" placeholder="0 0 8 * * ?" />
        </el-form-item>
        <el-form-item :label="t('inspection.startDate')">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('inspection.endDate')">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
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
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 14px; }
.header-icon {
  width: 40px; height: 40px;
  background: rgba(95, 201, 146, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #5fc992;
}
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); margin: 4px 0 0; }
.table-card { border-radius: 12px; overflow: hidden; border: 1px solid var(--bg-active); }
.pagination-row { display: flex; justify-content: flex-end; margin-top: 16px; }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-info { background: rgba(144, 147, 153, 0.15); color: #909399; }
:deep(.el-table) { --el-table-bg-color: var(--bg-surface); --el-table-tr-bg-color: var(--bg-surface); }
:deep(.el-table__row--striped td.el-table__cell) { background: var(--bg-base) !important; }
</style>
