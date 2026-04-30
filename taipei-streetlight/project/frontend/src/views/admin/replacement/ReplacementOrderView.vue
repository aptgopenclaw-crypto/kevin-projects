<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Repeat, Plus, Search } from 'lucide-vue-next'
import { getReplacementOrders, createReplacementOrder } from '@/api/replacement'
import type {
  ReplacementOrderResponse,
  ReplacementOrderRequest,
  ReplacementOrderStatus,
  ReplacementOrderType,
} from '@/types/replacement'
import { formatDateTime } from '@/utils/datetime'

const { t } = useI18n()
const router = useRouter()

// ──────────── List State ────────────
const loading = ref(false)
const keyword = ref('')
const filterStatus = ref<ReplacementOrderStatus | ''>('')
const filterType = ref<ReplacementOrderType | ''>('')
const pageSize = ref(15)
const items = ref<ReplacementOrderResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

// ──────────── Create Dialog ────────────
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = ref<ReplacementOrderRequest>({ orderType: 'REPLACEMENT' })

const createRules = computed<FormRules>(() => ({
  orderType: [{ required: true, message: t('replacement.validation.orderTypeRequired'), trigger: 'change' }],
}))

// ──────────── Helpers ────────────
const statusLabel = (s: ReplacementOrderStatus) => {
  const map: Record<ReplacementOrderStatus, string> = {
    DRAFT: t('replacement.statusDraft'),
    DISPATCHED: t('replacement.statusDispatched'),
    IN_PROGRESS: t('replacement.statusInProgress'),
    SELF_CHECKED: t('replacement.statusSelfChecked'),
    PENDING_REVIEW: t('replacement.statusPendingReview'),
    RETURNED: t('replacement.statusReturned'),
    CLOSED: t('replacement.statusClosed'),
  }
  return map[s] ?? s
}

const statusClass = (s: ReplacementOrderStatus) => {
  const map: Record<string, string> = {
    DRAFT: 'status-info',
    DISPATCHED: 'status-info',
    IN_PROGRESS: 'status-primary',
    SELF_CHECKED: 'status-success',
    PENDING_REVIEW: 'status-warning',
    RETURNED: 'status-danger',
    CLOSED: 'status-success',
  }
  return map[s] ?? ''
}

const typeLabel = (val: ReplacementOrderType) => {
  const map: Record<ReplacementOrderType, string> = {
    NEW_INSTALL: t('replacement.typeNewInstall'),
    REPLACEMENT: t('replacement.typeReplacement'),
    RELOCATION: t('replacement.typeRelocation'),
    DECOMMISSION: t('replacement.typeDecommission'),
    ADJUSTMENT: t('replacement.typeAdjustment'),
    SHADE_INSTALL: t('replacement.typeShadeInstall'),
  }
  return map[val] ?? val
}

// ──────────── Data Fetch ────────────
const fetchData = async (page = 0) => {
  loading.value = true
  try {
    const res = await getReplacementOrders({
      status: filterStatus.value || undefined,
      orderType: filterType.value || undefined,
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
  } finally {
    loading.value = false
  }
}

const handleSearch = () => fetchData(0)
const handlePageChange = (p: number) => fetchData(p - 1)
const handleSizeChange = (s: number) => { pageSize.value = s; fetchData(0) }
const goDetail = (row: ReplacementOrderResponse) => router.push(`/admin/replacement/orders/${row.id}`)

// ──────────── Create ────────────
const openCreateDialog = () => {
  createForm.value = { orderType: 'REPLACEMENT' }
  createVisible.value = true
}

const handleCreate = async () => {
  await createFormRef.value?.validate()
  createLoading.value = true
  try {
    await createReplacementOrder(createForm.value)
    ElMessage.success(t('replacement.createdSuccess'))
    createVisible.value = false
    fetchData(0)
  } finally {
    createLoading.value = false
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon"><Repeat :size="20" /></div>
        <div>
          <h2 class="header-title">{{ t('replacement.orderTitle') }}</h2>
          <p class="header-subtitle">{{ t('replacement.orderSubtitle') }}</p>
        </div>
      </div>
      <el-button class="create-btn" @click="openCreateDialog">
        <Plus :size="16" style="margin-right: 6px" /> {{ t('replacement.create') }}
      </el-button>
    </div>

    <!-- Filter -->
    <div class="filter-bar">
      <el-select v-model="filterStatus" clearable :placeholder="t('replacement.filterStatus')" style="width: 150px" @change="handleSearch">
        <el-option v-for="s in (['DRAFT','DISPATCHED','IN_PROGRESS','SELF_CHECKED','PENDING_REVIEW','RETURNED','CLOSED'] as ReplacementOrderStatus[])" :key="s" :value="s" :label="statusLabel(s)" />
      </el-select>
      <el-select v-model="filterType" clearable :placeholder="t('replacement.filterType')" style="width: 140px" @change="handleSearch">
        <el-option v-for="val in (['NEW_INSTALL','REPLACEMENT','RELOCATION','DECOMMISSION','ADJUSTMENT','SHADE_INSTALL'] as ReplacementOrderType[])" :key="val" :value="val" :label="typeLabel(val)" />
      </el-select>
      <el-input v-model="keyword" clearable :placeholder="t('replacement.searchPlaceholder')" style="width: 220px" @keyup.enter="handleSearch" />
      <el-button class="search-btn" @click="handleSearch"><Search :size="16" /></el-button>
    </div>

    <!-- Table -->
    <div class="table-card" v-loading="loading">
      <el-table :data="items" stripe @row-click="goDetail" style="cursor: pointer">
        <el-table-column prop="orderNumber" :label="t('replacement.orderNumber')" width="180" />
        <el-table-column :label="t('replacement.orderType')" width="120">
          <template #default="{ row }">{{ typeLabel(row.orderType) }}</template>
        </el-table-column>
        <el-table-column prop="location" :label="t('replacement.location')" show-overflow-tooltip />
        <el-table-column prop="assignedContractor" :label="t('replacement.assignedContractor')" width="140" />
        <el-table-column :label="t('replacement.filterStatus')" width="120">
          <template #default="{ row }">
            <span class="status-badge" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('replacement.createdAt')" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
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

    <!-- Create Dialog -->
    <el-dialog v-model="createVisible" :title="t('replacement.dialogCreateTitle')" width="560px" class="dark-dialog">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item :label="t('replacement.orderType')" prop="orderType">
          <el-select v-model="createForm.orderType" style="width: 100%">
            <el-option v-for="val in (['NEW_INSTALL','REPLACEMENT','RELOCATION','DECOMMISSION','ADJUSTMENT','SHADE_INSTALL'] as ReplacementOrderType[])" :key="val" :value="val" :label="typeLabel(val)" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('replacement.location')">
          <el-input v-model="createForm.location" />
        </el-form-item>
        <el-form-item :label="t('replacement.dispatchReason')">
          <el-input v-model="createForm.dispatchReason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="t('replacement.expectedQuantity')">
          <el-input-number v-model="createForm.expectedQuantity" :min="1" />
        </el-form-item>
        <el-form-item :label="t('replacement.assignedContractor')">
          <el-input v-model="createForm.assignedContractor" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="createVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button class="submit-btn" @click="handleCreate" :loading="createLoading">{{ t('common.confirm') }}</el-button>
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
  background: rgba(64, 158, 255, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #409eff;
}
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); margin: 4px 0 0; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.table-card { border-radius: 12px; overflow: hidden; border: 1px solid var(--bg-active); }
.pagination-row { display: flex; justify-content: flex-end; margin-top: 16px; }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-warning { background: rgba(230, 162, 60, 0.15); color: #e6a23c; }
.status-danger { background: rgba(255, 99, 99, 0.15); color: #ff6363; }
.status-info { background: rgba(64, 158, 255, 0.15); color: #409eff; }
.status-primary { background: rgba(128, 100, 255, 0.15); color: #8064ff; }
:deep(.el-table) { --el-table-bg-color: var(--bg-surface); --el-table-tr-bg-color: var(--bg-surface); }
:deep(.el-table__row--striped td.el-table__cell) { background: var(--bg-base) !important; }
</style>
