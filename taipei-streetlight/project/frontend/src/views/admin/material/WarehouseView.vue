<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Warehouse, Plus, Search } from 'lucide-vue-next'
import {
  getWarehouses,
  createWarehouse,
  updateWarehouse,
  deleteWarehouse,
} from '@/api/material'
import type {
  WarehouseResponse,
  WarehouseRequest,
  WarehouseStatus,
} from '@/types/material'
import { formatDateTime } from '@/utils/datetime'
import type { FormInstance, FormRules } from 'element-plus'
import { computed } from 'vue'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const filterStatus = ref<WarehouseStatus | ''>('')
const pageSize = ref(15)
const items = ref<WarehouseResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = ref<WarehouseRequest>({ warehouseCode: '', warehouseName: '' })

const rules = computed<FormRules>(() => ({
  warehouseCode: [{ required: true, message: t('material.errors.warehouseCodeRequired'), trigger: 'blur' }],
  warehouseName: [{ required: true, message: t('material.errors.warehouseNameRequired'), trigger: 'blur' }],
}))

const statusLabel = (s: WarehouseStatus) =>
  s === 'ACTIVE' ? t('common.enabled') : t('common.disabled')

const statusClass = (s: WarehouseStatus) =>
  s === 'ACTIVE' ? 'status-success' : 'status-info'

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getWarehouses({
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
  form.value = { warehouseCode: '', warehouseName: '' }
  dialogVisible.value = true
}

function openEdit(row: WarehouseResponse) {
  isEdit.value = true
  editId.value = row.id
  form.value = {
    warehouseCode: row.warehouseCode,
    warehouseName: row.warehouseName,
    location: row.location ?? undefined,
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
      await updateWarehouse(editId.value, form.value)
      ElMessage.success(t('material.updatedSuccess'))
    } else {
      await createWarehouse(form.value)
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

async function handleDelete(row: WarehouseResponse) {
  try {
    await ElMessageBox.confirm(t('material.deleteWarehouseConfirm'), t('common.confirmDelete'), { type: 'warning' })
    await deleteWarehouse(row.id)
    ElMessage.success(t('material.deletedSuccess'))
    fetchData(pagination.value.page)
  } catch {
    // cancelled
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <Warehouse :size="22" />
        <h2>{{ t('material.warehouseTitle') }}</h2>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="16" />
        {{ t('common.add') }}
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="filterStatus" :placeholder="t('common.status')" clearable @change="() => fetchData()">
        <el-option :label="t('common.enabled')" value="ACTIVE" />
        <el-option :label="t('common.disabled')" value="INACTIVE" />
      </el-select>
      <el-input v-model="keyword" :placeholder="t('common.search')" clearable style="width: 240px" @keyup.enter="() => fetchData()">
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <el-button @click="fetchData()">{{ t('common.query') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="warehouseCode" :label="t('material.warehouseCode')" width="150" />
      <el-table-column prop="warehouseName" :label="t('material.warehouseName')" min-width="200" />
      <el-table-column prop="location" :label="t('material.location')" min-width="200" />
      <el-table-column :label="t('common.status')" width="100">
        <template #default="{ row }">
          <span :class="['status-tag', statusClass(row.status)]">{{ statusLabel(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.createTime')" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? t('material.dialogEditWarehouse') : t('material.dialogCreateWarehouse')"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('material.warehouseCode')" prop="warehouseCode">
          <el-input v-model="form.warehouseCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="t('material.warehouseName')" prop="warehouseName">
          <el-input v-model="form.warehouseName" />
        </el-form-item>
        <el-form-item :label="t('material.location')">
          <el-input v-model="form.location" />
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
