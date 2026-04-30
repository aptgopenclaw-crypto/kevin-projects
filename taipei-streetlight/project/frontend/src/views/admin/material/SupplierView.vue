<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Truck, Plus, Search } from 'lucide-vue-next'
import { getSuppliers, createSupplier, updateSupplier } from '@/api/material'
import type { SupplierResponse, SupplierRequest, SupplierStatus } from '@/types/material'
import { formatDateTime } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const pageSize = ref(15)
const items = ref<SupplierResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = ref<SupplierRequest>({ supplierCode: '', supplierName: '' })

const rules = computed<FormRules>(() => ({
  supplierCode: [{ required: true, message: t('material.errors.supplierCodeRequired'), trigger: 'blur' }],
  supplierName: [{ required: true, message: t('material.errors.supplierNameRequired'), trigger: 'blur' }],
}))

const statusLabel = (s: SupplierStatus) =>
  s === 'ACTIVE' ? t('common.enabled') : t('common.disabled')

const statusClass = (s: SupplierStatus) =>
  s === 'ACTIVE' ? 'status-success' : 'status-info'

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getSuppliers({
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
  form.value = { supplierCode: '', supplierName: '' }
  dialogVisible.value = true
}

function openEdit(row: SupplierResponse) {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    supplierCode: row.supplierCode,
    supplierName: row.supplierName,
    contactName: row.contactName ?? undefined,
    contactPhone: row.contactPhone ?? undefined,
    contactEmail: row.contactEmail ?? undefined,
    address: row.address ?? undefined,
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
      await updateSupplier(editId.value, form.value)
      ElMessage.success(t('material.updatedSuccess'))
    } else {
      await createSupplier(form.value)
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

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <Truck :size="22" />
        <h2>{{ t('material.supplierTitle') }}</h2>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="16" />
        {{ t('common.add') }}
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="keyword" :placeholder="t('common.search')" clearable style="width: 240px" @keyup.enter="() => fetchData()">
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <el-button @click="fetchData()">{{ t('common.query') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="supplierCode" :label="t('material.supplierCode')" width="150" />
      <el-table-column prop="supplierName" :label="t('material.supplierName')" min-width="180" />
      <el-table-column prop="contactName" :label="t('material.contactName')" width="120" />
      <el-table-column prop="contactPhone" :label="t('material.contactPhone')" width="140" />
      <el-table-column prop="contactEmail" :label="t('material.contactEmail')" width="200" />
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? t('material.dialogEditSupplier') : t('material.dialogCreateSupplier')"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('material.supplierCode')" prop="supplierCode">
          <el-input v-model="form.supplierCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="t('material.supplierName')" prop="supplierName">
          <el-input v-model="form.supplierName" />
        </el-form-item>
        <el-form-item :label="t('material.contactName')">
          <el-input v-model="form.contactName" />
        </el-form-item>
        <el-form-item :label="t('material.contactPhone')">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item :label="t('material.contactEmail')">
          <el-input v-model="form.contactEmail" />
        </el-form-item>
        <el-form-item :label="t('material.address')">
          <el-input v-model="form.address" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item v-if="isEdit" :label="t('common.status')">
          <el-select v-model="form.status" style="width: 100%">
            <el-option :label="t('common.enabled')" value="ACTIVE" />
            <el-option :label="t('common.disabled')" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleSubmit">{{ t('common.save') }}</el-button>
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
.status-info { background: #f5f5f5; color: #6b7280; }
</style>
