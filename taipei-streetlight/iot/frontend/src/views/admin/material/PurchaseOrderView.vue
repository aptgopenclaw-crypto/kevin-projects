<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ShoppingCart, Plus, Search, Send } from 'lucide-vue-next'
import {
  getPurchaseOrders,
  getPurchaseOrderById,
  createPurchaseOrder,
  updatePurchaseOrder,
  submitPurchaseOrder,
  getActiveSuppliers,
} from '@/api/material'
import type {
  PurchaseOrderResponse,
  PurchaseOrderRequest,
  PurchaseOrderStatus,
  SupplierResponse,
} from '@/types/material'
import { formatDateTime, formatDate } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const filterStatus = ref<PurchaseOrderStatus | ''>('')
const pageSize = ref(15)
const items = ref<PurchaseOrderResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

const suppliers = ref<SupplierResponse[]>([])

// ── Dialog ──
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = ref<PurchaseOrderRequest>({ supplierId: 0, items: [] })

// ── Detail Dialog ──
const detailVisible = ref(false)
const detailData = ref<PurchaseOrderResponse | null>(null)

const rules = computed<FormRules>(() => ({
  supplierId: [{ required: true, message: t('material.errors.supplierRequired'), trigger: 'change' }],
}))

const statusOptions: { value: PurchaseOrderStatus; label: string }[] = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'SUBMITTED', label: '已送審' },
  { value: 'APPROVED', label: '已核准' },
  { value: 'RECEIVING', label: '收料中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

const statusLabel = (s: PurchaseOrderStatus) =>
  statusOptions.find(o => o.value === s)?.label ?? s

const statusClass = (s: PurchaseOrderStatus) => {
  const map: Record<string, string> = {
    DRAFT: 'status-info',
    SUBMITTED: 'status-warning',
    APPROVED: 'status-primary',
    RECEIVING: 'status-primary',
    COMPLETED: 'status-success',
    CANCELLED: 'status-danger',
  }
  return map[s] ?? ''
}

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getPurchaseOrders({
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

async function fetchSuppliers() {
  try {
    const res = await getActiveSuppliers()
    suppliers.value = res.body
  } catch {
    // silent
  }
}

function handlePageChange(p: number) {
  fetchData(p - 1)
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  form.value = { supplierId: 0, items: [{ materialSpecId: 0, quantity: 1 }] }
  dialogVisible.value = true
}

function openEdit(row: PurchaseOrderResponse) {
  if (row.status !== 'DRAFT') return
  isEdit.value = true
  editId.value = row.id
  form.value = {
    supplierId: row.supplierId,
    contractId: row.contractId ?? undefined,
    notes: row.notes ?? undefined,
    items: row.items.map(i => ({
      materialSpecId: i.materialSpecId,
      quantity: i.quantity,
      unitPrice: i.unitPrice ?? undefined,
      notes: i.notes ?? undefined,
    })),
  }
  dialogVisible.value = true
}

async function openDetail(row: PurchaseOrderResponse) {
  try {
    const res = await getPurchaseOrderById(row.id)
    detailData.value = res.body
    detailVisible.value = true
  } catch {
    ElMessage.error(t('material.loadFailed'))
  }
}

function addItem() {
  form.value.items.push({ materialSpecId: 0, quantity: 1 })
}

function removeItem(index: number) {
  form.value.items.splice(index, 1)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  dialogLoading.value = true
  try {
    if (isEdit.value && editId.value) {
      await updatePurchaseOrder(editId.value, form.value)
      ElMessage.success(t('material.updatedSuccess'))
    } else {
      await createPurchaseOrder(form.value)
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

async function handleSubmitOrder(row: PurchaseOrderResponse) {
  try {
    await submitPurchaseOrder(row.id)
    ElMessage.success(t('material.poSubmitted'))
    fetchData(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  }
}

onMounted(() => {
  fetchSuppliers()
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <ShoppingCart :size="22" />
        <h2>{{ t('material.purchaseTitle') }}</h2>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="16" />
        {{ t('common.add') }}
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="filterStatus" :placeholder="t('common.status')" clearable @change="() => fetchData()">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-input v-model="keyword" :placeholder="t('common.search')" clearable style="width: 240px" @keyup.enter="() => fetchData()">
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <el-button @click="fetchData()">{{ t('common.query') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="poNumber" :label="t('material.poNumber')" width="180" />
      <el-table-column prop="supplierName" :label="t('material.supplierName')" min-width="180" />
      <el-table-column :label="t('material.orderDate')" width="130">
        <template #default="{ row }">{{ row.orderDate ? formatDate(row.orderDate) : '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('material.totalAmount')" width="120" align="right">
        <template #default="{ row }">{{ row.totalAmount ?? '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('common.status')" width="100">
        <template #default="{ row }">
          <span :class="['status-tag', statusClass(row.status)]">{{ statusLabel(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.createTime')" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{ t('material.detail') }}</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="success" @click="handleSubmitOrder(row)">
            <Send :size="14" />
            {{ t('material.submit') }}
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

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? t('material.dialogEditPO') : t('material.dialogCreatePO')"
      width="700px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('material.supplierName')" prop="supplierId">
          <el-select v-model="form.supplierId" filterable style="width: 100%">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('material.notes')">
          <el-input v-model="form.notes" type="textarea" :rows="2" />
        </el-form-item>

        <el-divider>{{ t('material.purchaseItems') }}</el-divider>

        <div v-for="(item, idx) in form.items" :key="idx" class="item-row">
          <el-input-number v-model="item.materialSpecId" :min="1" :placeholder="t('material.specId')" style="width: 120px" />
          <el-input-number v-model="item.quantity" :min="1" :placeholder="t('material.quantity')" style="width: 100px" />
          <el-input-number v-model="item.unitPrice" :min="0" :precision="2" :placeholder="t('material.unitPrice')" style="width: 120px" />
          <el-button link type="danger" @click="removeItem(idx)">{{ t('common.delete') }}</el-button>
        </div>
        <el-button type="dashed" style="width: 100%; margin-top: 8px" @click="addItem">
          <Plus :size="16" />
          {{ t('material.addItem') }}
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" :title="t('material.poDetail')" width="700px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="t('material.poNumber')">{{ detailData.poNumber }}</el-descriptions-item>
          <el-descriptions-item :label="t('material.supplierName')">{{ detailData.supplierName }}</el-descriptions-item>
          <el-descriptions-item :label="t('common.status')">
            <span :class="['status-tag', statusClass(detailData.status)]">{{ statusLabel(detailData.status) }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('material.totalAmount')">{{ detailData.totalAmount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('material.orderDate')">{{ detailData.orderDate ? formatDate(detailData.orderDate) : '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('common.createTime')">{{ formatDateTime(detailData.createdAt) }}</el-descriptions-item>
          <el-descriptions-item :label="t('material.notes')" :span="2">{{ detailData.notes ?? '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 16px 0 8px">{{ t('material.purchaseItems') }}</h4>
        <el-table :data="detailData.items" stripe size="small">
          <el-table-column prop="specCode" :label="t('material.specCode')" width="140" />
          <el-table-column prop="specName" :label="t('material.specName')" min-width="180" />
          <el-table-column prop="quantity" :label="t('material.quantity')" width="80" align="right" />
          <el-table-column prop="unitPrice" :label="t('material.unitPrice')" width="100" align="right" />
          <el-table-column prop="notes" :label="t('material.notes')" min-width="120" />
        </el-table>
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
.status-info { background: #f5f5f5; color: #6b7280; }
.status-danger { background: #fef2f2; color: #dc2626; }
</style>
