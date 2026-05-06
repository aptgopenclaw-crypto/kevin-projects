<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { PackageCheck, Plus, Search, Upload } from 'lucide-vue-next'
import {
  getApprovedMaterials,
  createApprovedMaterial,
  updateApprovedMaterial,
  importApprovedMaterials,
} from '@/api/material'
import type {
  ApprovedMaterialResponse,
  ApprovedMaterialRequest,
  ApprovedMaterialStatus,
} from '@/types/material'
import { formatDateTime, formatDate } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const pageSize = ref(15)
const items = ref<ApprovedMaterialResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = ref<ApprovedMaterialRequest>({
  materialSpecId: 0,
  materialNumber: '',
  approvalDate: '',
})

const importVisible = ref(false)
const importLoading = ref(false)
const importFile = ref<File | null>(null)

const rules = computed<FormRules>(() => ({
  materialSpecId: [{ required: true, message: t('material.errors.specRequired'), trigger: 'change' }],
  materialNumber: [{ required: true, message: t('material.errors.materialNumberRequired'), trigger: 'blur' }],
  approvalDate: [{ required: true, message: t('material.errors.approvalDateRequired'), trigger: 'change' }],
}))

const statusLabel = (s: ApprovedMaterialStatus) => {
  const map: Record<ApprovedMaterialStatus, string> = {
    ACTIVE: t('common.enabled'),
    EXPIRED: t('material.statusExpired'),
    REVOKED: t('material.statusRevoked'),
  }
  return map[s] ?? s
}

const statusClass = (s: ApprovedMaterialStatus) => {
  const map: Record<string, string> = {
    ACTIVE: 'status-success',
    EXPIRED: 'status-warning',
    REVOKED: 'status-danger',
  }
  return map[s] ?? ''
}

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getApprovedMaterials({
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
    ElMessage.error(t('material.loadFailed'))
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
  form.value = { materialSpecId: 0, materialNumber: '', approvalDate: '' }
  dialogVisible.value = true
}

function openEdit(row: ApprovedMaterialResponse) {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    materialSpecId: row.materialSpecId,
    contractId: row.contractId ?? undefined,
    materialNumber: row.materialNumber,
    approvalDate: row.approvalDate,
    batchNumber: row.batchNumber ?? undefined,
    brand: row.brand ?? undefined,
    model: row.model ?? undefined,
    status: row.status,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  dialogLoading.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateApprovedMaterial(editId.value, form.value)
      ElMessage.success(t('material.updatedSuccess'))
    } else {
      await createApprovedMaterial(form.value)
      ElMessage.success(t('material.createdSuccess'))
    }
    dialogVisible.value = false
    fetchData(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    dialogLoading.value = false
  }
}

function handleFileChange(file: { raw: File }) {
  importFile.value = file.raw
}

async function handleImport() {
  if (!importFile.value) return
  importLoading.value = true
  try {
    const res = await importApprovedMaterials(importFile.value)
    const r = res.body
    ElMessage.success(t('material.importResult', { success: r.successCount, skipped: r.skippedCount, errors: r.errors.length }))
    importVisible.value = false
    importFile.value = null
    fetchData()
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    importLoading.value = false
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <PackageCheck :size="22" />
        <h2>{{ t('material.approvedTitle') }}</h2>
      </div>
      <div class="header-actions">
        <el-button @click="importVisible = true">
          <Upload :size="16" />
          {{ t('material.batchImport') }}
        </el-button>
        <el-button type="primary" @click="openCreate">
          <Plus :size="16" />
          {{ t('common.add') }}
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="keyword" :placeholder="t('common.search')" clearable style="width: 240px" @keyup.enter="() => fetchData()">
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <el-button @click="fetchData()">{{ t('common.query') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="materialNumber" :label="t('material.materialNumber')" width="160" />
      <el-table-column prop="specName" :label="t('material.specName')" min-width="180" />
      <el-table-column prop="brand" :label="t('material.brand')" width="120" />
      <el-table-column prop="model" :label="t('material.model')" width="120" />
      <el-table-column :label="t('material.approvalDate')" width="130">
        <template #default="{ row }">{{ formatDate(row.approvalDate) }}</template>
      </el-table-column>
      <el-table-column prop="batchNumber" :label="t('material.batchNumber')" width="120" />
      <el-table-column :label="t('common.status')" width="100">
        <template #default="{ row }">
          <span :class="['status-tag', statusClass(row.status)]">{{ statusLabel(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.createTime')" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
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
      :title="isEdit ? t('material.dialogEditApproved') : t('material.dialogCreateApproved')"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="t('material.specId')" prop="materialSpecId">
          <el-input-number v-model="form.materialSpecId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('material.materialNumber')" prop="materialNumber">
          <el-input v-model="form.materialNumber" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="t('material.approvalDate')" prop="approvalDate">
          <el-date-picker v-model="form.approvalDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('material.brand')">
          <el-input v-model="form.brand" />
        </el-form-item>
        <el-form-item :label="t('material.model')">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item :label="t('material.batchNumber')">
          <el-input v-model="form.batchNumber" />
        </el-form-item>
        <el-form-item v-if="isEdit" :label="t('common.status')">
          <el-select v-model="form.status" style="width: 100%">
            <el-option :label="t('common.enabled')" value="ACTIVE" />
            <el-option :label="t('material.statusExpired')" value="EXPIRED" />
            <el-option :label="t('material.statusRevoked')" value="REVOKED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- Import Dialog -->
    <el-dialog v-model="importVisible" :title="t('material.batchImport')" width="460px">
      <el-upload
        :auto-upload="false"
        :limit="1"
        accept=".csv"
        :on-change="handleFileChange"
        drag
      >
        <div style="padding: 20px">
          <Upload :size="32" />
          <p>{{ t('material.importHint') }}</p>
        </div>
      </el-upload>
      <template #footer>
        <el-button @click="importVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="importLoading" :disabled="!importFile" @click="handleImport">{{ t('material.startImport') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 8px; }
.header-actions { display: flex; gap: 8px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.status-tag { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-success { background: #f0f9ff; color: #059669; }
.status-warning { background: #fffbeb; color: #d97706; }
.status-danger { background: #fef2f2; color: #dc2626; }
</style>
