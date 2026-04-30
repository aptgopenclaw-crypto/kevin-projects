<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { BarChart3, Plus, Search } from 'lucide-vue-next'
import {
  getIndicators,
  createIndicator,
  updateIndicator,
  deleteIndicator,
  testFormula,
} from '@/api/kpi'
import type {
  KpiIndicatorResponse,
  KpiIndicatorRequest,
  FormulaTestRequest,
  KpiCategory,
  KpiIndicatorStatus,
  FormulaType,
  KpiDataSource,
} from '@/types/kpi'
import { formatDateTime } from '@/utils/datetime'
import type { FormInstance, FormRules } from 'element-plus'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const filterCategory = ref<KpiCategory | ''>('')
const filterStatus = ref<KpiIndicatorStatus | ''>('')
const pageSize = ref(15)
const items = ref<KpiIndicatorResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// Dialog
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = ref<KpiIndicatorRequest>({
  indicatorCode: '',
  indicatorName: '',
  category: 'MAINTENANCE',
  formulaType: 'SPEL',
  formula: '',
  dataSource: 'SYSTEM_AUTO',
})

// Formula test
const testDialogVisible = ref(false)
const testResult = ref<string | null>(null)
const testError = ref<string | null>(null)
const testVariables = ref('{"value": 100, "target": 100}')

const categoryOptions: { label: string; value: KpiCategory }[] = [
  { label: '維護保養', value: 'MAINTENANCE' },
  { label: '用電管理', value: 'POWER' },
  { label: '回應效率', value: 'RESPONSE' },
  { label: '品質管理', value: 'QUALITY' },
  { label: '自訂', value: 'CUSTOM' },
]

const statusOptions: { label: string; value: KpiIndicatorStatus }[] = [
  { label: '啟用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
  { label: '草稿', value: 'DRAFT' },
]

const formulaTypeOptions: { label: string; value: FormulaType }[] = [
  { label: 'SpEL', value: 'SPEL' },
  { label: 'JavaScript', value: 'JS' },
]

const dataSourceOptions: { label: string; value: KpiDataSource }[] = [
  { label: '系統自動', value: 'SYSTEM_AUTO' },
  { label: '手動匯入', value: 'MANUAL_IMPORT' },
  { label: 'IoT 裝置', value: 'IOT_DEVICE' },
]

const rules = computed<FormRules>(() => ({
  indicatorCode: [{ required: true, message: t('kpi.errors.codeRequired'), trigger: 'blur' }],
  indicatorName: [{ required: true, message: t('kpi.errors.nameRequired'), trigger: 'blur' }],
  category: [{ required: true, message: t('kpi.errors.categoryRequired'), trigger: 'change' }],
  formula: [{ required: true, message: t('kpi.errors.formulaRequired'), trigger: 'blur' }],
}))

const categoryLabel = (c: KpiCategory) => categoryOptions.find(o => o.value === c)?.label ?? c
const statusLabel = (s: KpiIndicatorStatus) => statusOptions.find(o => o.value === s)?.label ?? s
const statusClass = (s: KpiIndicatorStatus) =>
  s === 'ACTIVE' ? 'status-success' : s === 'INACTIVE' ? 'status-danger' : 'status-info'

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getIndicators({
      category: filterCategory.value || undefined,
      status: filterStatus.value || undefined,
      keyword: keyword.value || undefined,
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
    ElMessage.error(t('kpi.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) {
  fetchData(p - 1)
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  form.value = {
    indicatorCode: '',
    indicatorName: '',
    category: 'MAINTENANCE',
    formulaType: 'SPEL',
    formula: '',
    dataSource: 'SYSTEM_AUTO',
  }
  dialogVisible.value = true
}

function openEdit(row: KpiIndicatorResponse) {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    indicatorCode: row.indicatorCode,
    indicatorName: row.indicatorName,
    category: row.category,
    description: row.description ?? undefined,
    formulaType: row.formulaType,
    formula: row.formula,
    targetValue: row.targetValue ?? undefined,
    weight: row.weight ?? undefined,
    dataSource: row.dataSource,
    unit: row.unit ?? undefined,
    bonusCondition: row.bonusCondition ?? undefined,
    penaltyCondition: row.penaltyCondition ?? undefined,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  dialogLoading.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateIndicator(editId.value, form.value)
      ElMessage.success(t('kpi.updatedSuccess'))
    } else {
      await createIndicator(form.value)
      ElMessage.success(t('kpi.createdSuccess'))
    }
    dialogVisible.value = false
    fetchData(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    dialogLoading.value = false
  }
}

async function handleDelete(row: KpiIndicatorResponse) {
  try {
    await ElMessageBox.confirm(t('kpi.deleteConfirm'), t('common.confirmDelete'), { type: 'warning' })
    await deleteIndicator(row.id)
    ElMessage.success(t('kpi.deletedSuccess'))
    fetchData(pagination.value.page)
  } catch {
    // cancelled
  }
}

function openTestFormula() {
  testResult.value = null
  testError.value = null
  testDialogVisible.value = true
}

async function handleTestFormula() {
  try {
    const vars = JSON.parse(testVariables.value)
    const payload: FormulaTestRequest = {
      formulaType: form.value.formulaType,
      formula: form.value.formula,
      variables: vars,
    }
    const res = await testFormula(payload)
    if (res.body.success) {
      testResult.value = String(res.body.result)
      testError.value = null
    } else {
      testResult.value = null
      testError.value = res.body.error ?? t('kpi.formulaTestFailed')
    }
  } catch (e: any) {
    testError.value = e.message ?? t('kpi.formulaTestFailed')
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <BarChart3 :size="22" />
        <h2>{{ t('kpi.indicatorTitle') }}</h2>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="16" />
        {{ t('common.add') }}
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="filterCategory" :placeholder="t('kpi.category')" clearable @change="() => fetchData()">
        <el-option v-for="o in categoryOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-select v-model="filterStatus" :placeholder="t('common.status')" clearable @change="() => fetchData()">
        <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-input v-model="keyword" :placeholder="t('common.search')" clearable style="width: 240px" @keyup.enter="() => fetchData()">
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <el-button @click="fetchData()">{{ t('common.query') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="indicatorCode" :label="t('kpi.indicatorCode')" width="140" />
      <el-table-column prop="indicatorName" :label="t('kpi.indicatorName')" min-width="200" />
      <el-table-column :label="t('kpi.category')" width="120">
        <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
      </el-table-column>
      <el-table-column :label="t('kpi.targetValue')" width="100" align="right">
        <template #default="{ row }">{{ row.targetValue ?? '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('kpi.weight')" width="80" align="right">
        <template #default="{ row }">{{ row.weight ?? '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('common.status')" width="80">
        <template #default="{ row }">
          <span :class="['status-tag', statusClass(row.status)]">{{ statusLabel(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.updateTime')" width="170">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button link type="danger" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
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

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? t('kpi.editIndicator') : t('kpi.createIndicator')"
      width="650px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="t('kpi.indicatorCode')" prop="indicatorCode">
          <el-input v-model="form.indicatorCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="t('kpi.indicatorName')" prop="indicatorName">
          <el-input v-model="form.indicatorName" />
        </el-form-item>
        <el-form-item :label="t('kpi.category')" prop="category">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="o in categoryOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('kpi.description')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('kpi.formulaType')">
          <el-select v-model="form.formulaType" style="width: 100%">
            <el-option v-for="o in formulaTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('kpi.formula')" prop="formula">
          <el-input v-model="form.formula" type="textarea" :rows="3" placeholder="#value / #target * 100" />
          <el-button link type="primary" style="margin-top: 4px" @click="openTestFormula">{{ t('kpi.testFormula') }}</el-button>
        </el-form-item>
        <el-form-item :label="t('kpi.targetValue')">
          <el-input-number v-model="form.targetValue" :min="0" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('kpi.weight')">
          <el-input-number v-model="form.weight" :min="0" :max="1" :step="0.01" :precision="4" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('kpi.dataSource')">
          <el-select v-model="form.dataSource" style="width: 100%">
            <el-option v-for="o in dataSourceOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('kpi.unit')">
          <el-input v-model="form.unit" />
        </el-form-item>
        <el-form-item :label="t('kpi.bonusCondition')">
          <el-input v-model="form.bonusCondition" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('kpi.penaltyCondition')">
          <el-input v-model="form.penaltyCondition" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- Formula Test Dialog -->
    <el-dialog v-model="testDialogVisible" :title="t('kpi.testFormula')" width="500px" destroy-on-close>
      <p style="margin-bottom: 8px">{{ t('kpi.formulaTypeLabel') }}: <strong>{{ form.formulaType }}</strong></p>
      <el-input :model-value="form.formula" type="textarea" :rows="2" disabled style="margin-bottom: 12px" />
      <p style="margin-bottom: 4px">{{ t('kpi.testVariables') }} (JSON):</p>
      <el-input v-model="testVariables" type="textarea" :rows="3" />
      <div v-if="testResult !== null" style="margin-top: 12px; color: #059669; font-weight: bold">
        {{ t('kpi.testResultLabel') }}: {{ testResult }}
      </div>
      <div v-if="testError" style="margin-top: 12px; color: #dc2626">
        {{ t('kpi.testErrorLabel') }}: {{ testError }}
      </div>
      <template #footer>
        <el-button @click="testDialogVisible = false">{{ t('common.close') }}</el-button>
        <el-button type="primary" @click="handleTestFormula">{{ t('kpi.runTest') }}</el-button>
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
.status-tag { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-success { background: #f0f9ff; color: #059669; }
.status-danger { background: #fef2f2; color: #dc2626; }
.status-info { background: #f5f5f5; color: #6b7280; }
</style>
