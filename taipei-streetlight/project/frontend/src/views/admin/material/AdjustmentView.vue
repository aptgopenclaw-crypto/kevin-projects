<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ClipboardList } from 'lucide-vue-next'
import {
  getAdjustments,
  countInventory,
  transferInventory,
  correctInventory,
} from '@/api/material'
import type {
  InventoryAdjustmentResponse,
  InventoryAdjustmentRequest,
  AdjustmentType,
} from '@/types/material'
import { formatDateTime } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const pageSize = ref(15)
const items = ref<InventoryAdjustmentResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const actionType = ref<AdjustmentType>('COUNT')
const form = ref<InventoryAdjustmentRequest>({ inventoryId: 0 })

const typeLabel = (t: AdjustmentType) => {
  const map: Record<AdjustmentType, string> = {
    COUNT: '盤點',
    TRANSFER: '轉庫',
    CORRECTION: '修正',
  }
  return map[t] ?? t
}

const typeClass = (t: AdjustmentType) => {
  const map: Record<string, string> = {
    COUNT: 'status-primary',
    TRANSFER: 'status-warning',
    CORRECTION: 'status-info',
  }
  return map[t] ?? ''
}

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getAdjustments({ page, size: pageSize.value })
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

function openDialog(type: AdjustmentType) {
  actionType.value = type
  form.value = { inventoryId: 0 }
  dialogVisible.value = true
}

async function handleSubmit() {
  dialogLoading.value = true
  try {
    if (actionType.value === 'COUNT') {
      await countInventory(form.value)
    } else if (actionType.value === 'TRANSFER') {
      await transferInventory(form.value)
    } else {
      await correctInventory(form.value)
    }
    ElMessage.success(t('material.adjustmentSuccess'))
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
        <ClipboardList :size="22" />
        <h2>{{ t('material.adjustmentTitle') }}</h2>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openDialog('COUNT')">{{ t('material.doCount') }}</el-button>
        <el-button @click="openDialog('TRANSFER')">{{ t('material.doTransfer') }}</el-button>
        <el-button @click="openDialog('CORRECTION')">{{ t('material.doCorrection') }}</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="inventoryId" :label="t('material.inventoryId')" width="120" />
      <el-table-column :label="t('material.adjustmentType')" width="100">
        <template #default="{ row }">
          <span :class="['status-tag', typeClass(row.adjustmentType)]">{{ typeLabel(row.adjustmentType) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="quantityChange" :label="t('material.quantityChange')" width="120" align="right">
        <template #default="{ row }">
          <span :style="{ color: row.quantityChange >= 0 ? '#059669' : '#dc2626' }">
            {{ row.quantityChange >= 0 ? '+' : '' }}{{ row.quantityChange }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="reason" :label="t('material.reason')" min-width="200" />
      <el-table-column prop="adjustedBy" :label="t('material.adjustedBy')" width="120" />
      <el-table-column :label="t('material.adjustedAt')" width="170">
        <template #default="{ row }">{{ formatDateTime(row.adjustedAt) }}</template>
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
      :title="typeLabel(actionType)"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="110px">
        <el-form-item :label="t('material.inventoryId')">
          <el-input-number v-model="form.inventoryId" :min="1" style="width: 100%" />
        </el-form-item>

        <el-form-item v-if="actionType === 'COUNT'" :label="t('material.actualQuantity')">
          <el-input-number v-model="form.actualQuantity" :min="0" style="width: 100%" />
        </el-form-item>

        <el-form-item v-if="actionType === 'TRANSFER'" :label="t('material.toWarehouse')">
          <el-input-number v-model="form.toWarehouseId" :min="1" style="width: 100%" />
        </el-form-item>

        <el-form-item v-if="actionType !== 'COUNT'" :label="t('material.quantity')">
          <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
        </el-form-item>

        <el-form-item :label="t('material.reason')">
          <el-input v-model="form.reason" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 8px; }
.header-actions { display: flex; gap: 8px; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.status-tag { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-primary { background: #eff6ff; color: #2563eb; }
.status-warning { background: #fffbeb; color: #d97706; }
.status-info { background: #f5f5f5; color: #6b7280; }
</style>
