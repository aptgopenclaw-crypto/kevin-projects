<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { PackagePlus, Plus } from 'lucide-vue-next'
import { getReceivingRecords, createReceiving, getActiveWarehouses } from '@/api/material'
import type { ReceivingResponse, ReceivingRequest, WarehouseResponse } from '@/types/material'
import { formatDateTime, formatDate } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const pageSize = ref(15)
const items = ref<ReceivingResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })
const warehouses = ref<WarehouseResponse[]>([])

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const formRef = ref<FormInstance>()
const form = ref<ReceivingRequest>({
  warehouseId: 0,
  materialSpecId: 0,
  quantity: 1,
})

const rules = computed<FormRules>(() => ({
  warehouseId: [{ required: true, message: t('material.errors.warehouseRequired'), trigger: 'change' }],
  materialSpecId: [{ required: true, message: t('material.errors.specRequired'), trigger: 'change' }],
  quantity: [{ required: true, message: t('material.errors.quantityRequired'), trigger: 'blur' }],
}))

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getReceivingRecords({ page, size: pageSize.value })
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

async function fetchWarehouses() {
  try {
    const res = await getActiveWarehouses()
    warehouses.value = res.body
  } catch {
    // silent
  }
}

function handlePageChange(p: number) {
  fetchData(p - 1)
}

function openCreate() {
  form.value = { warehouseId: 0, materialSpecId: 0, quantity: 1 }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  dialogLoading.value = true
  try {
    await createReceiving(form.value)
    ElMessage.success(t('material.receivingSuccess'))
    dialogVisible.value = false
    fetchData(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    dialogLoading.value = false
  }
}

onMounted(() => {
  fetchWarehouses()
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <PackagePlus :size="22" />
        <h2>{{ t('material.receivingTitle') }}</h2>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="16" />
        {{ t('material.newReceiving') }}
      </el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="poNumber" :label="t('material.poNumber')" width="180" />
      <el-table-column prop="warehouseName" :label="t('material.warehouseName')" width="150" />
      <el-table-column prop="specName" :label="t('material.specName')" min-width="200" />
      <el-table-column prop="quantity" :label="t('material.quantity')" width="100" align="right" />
      <el-table-column :label="t('material.receivedDate')" width="130">
        <template #default="{ row }">{{ formatDate(row.receivedDate) }}</template>
      </el-table-column>
      <el-table-column prop="receivedBy" :label="t('material.receivedBy')" width="120" />
      <el-table-column prop="deliveryNote" :label="t('material.deliveryNote')" min-width="150" />
      <el-table-column :label="t('common.createTime')" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
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

    <el-dialog v-model="dialogVisible" :title="t('material.newReceiving')" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="t('material.poNumber')">
          <el-input-number v-model="form.poId" :min="1" style="width: 100%" :placeholder="t('material.optional')" />
        </el-form-item>
        <el-form-item :label="t('material.warehouseName')" prop="warehouseId">
          <el-select v-model="form.warehouseId" filterable style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('material.specId')" prop="materialSpecId">
          <el-input-number v-model="form.materialSpecId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('material.quantity')" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('material.deliveryNote')">
          <el-input v-model="form.deliveryNote" type="textarea" :rows="2" />
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
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
