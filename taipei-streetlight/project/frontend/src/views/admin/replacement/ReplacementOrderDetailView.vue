<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Repeat, Plus, Trash2, GitCompare } from 'lucide-vue-next'
import {
  getReplacementOrderById,
  dispatchReplacementOrder,
  startWorkReplacementOrder,
  submitReviewReplacementOrder,
  approveReplacementOrder,
  returnReplacementOrder,
  resubmitReplacementOrder,
  getReplacementItems,
  addReplacementItem,
  deleteReplacementItem,
} from '@/api/replacement'
import type {
  ReplacementOrderResponse,
  ReplacementItemResponse,
  ReplacementItemRequest,
  ReplacementOrderStatus,
  ReplacementOrderType,
} from '@/types/replacement'
import { formatDateTime } from '@/utils/datetime'
import ReplacementItemDialog from './ReplacementItemDialog.vue'
import BeforeAfterSpecComparison from '@/components/BeforeAfterSpecComparison.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

const loading = ref(false)
const order = ref<ReplacementOrderResponse | null>(null)
const items = ref<ReplacementItemResponse[]>([])

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

const itemStatusLabel = (s: string) => {
  const map: Record<string, string> = {
    PENDING: t('replacement.itemStatusPending'),
    IN_PROGRESS: t('replacement.itemStatusInProgress'),
    COMPLETED: t('replacement.itemStatusCompleted'),
    SKIPPED: t('replacement.itemStatusSkipped'),
  }
  return map[s] ?? s
}

const canEdit = computed(() => order.value?.status === 'DRAFT')
const canDispatch = computed(() => order.value?.status === 'DRAFT')
const canStartWork = computed(() => order.value?.status === 'DISPATCHED')
const canSelfCheck = computed(() => order.value?.status === 'IN_PROGRESS')
const canSubmitReview = computed(() => order.value?.status === 'SELF_CHECKED')
const canApprove = computed(() => order.value?.status === 'PENDING_REVIEW')
const canReturn = computed(() => order.value?.status === 'PENDING_REVIEW')
const canResubmit = computed(() => order.value?.status === 'RETURNED')

// ──────────── Data Fetch ────────────
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getReplacementOrderById(orderId)
    order.value = res.body
    const itemRes = await getReplacementItems(orderId)
    items.value = itemRes.body
  } finally {
    loading.value = false
  }
}

// ──────────── Actions ────────────
const confirmAction = async (actionFn: () => Promise<unknown>, label: string) => {
  await ElMessageBox.confirm(`${t('replacement.confirmAction')}`, label, { type: 'warning' })
  await actionFn()
  ElMessage.success(t('replacement.operationSuccess'))
  fetchData()
}

const handleDispatch = () => confirmAction(async () => {
  await dispatchReplacementOrder(orderId, {
    orderType: order.value!.orderType,
    assignedContractor: order.value!.assignedContractor ?? undefined,
    workPeriodStart: order.value!.workPeriodStart ?? undefined,
    workPeriodEnd: order.value!.workPeriodEnd ?? undefined,
  })
}, t('replacement.dispatch'))

const handleStartWork = () => confirmAction(
  () => startWorkReplacementOrder(orderId),
  t('replacement.startWork'),
)

const handleSelfCheck = () => {
  const pending = items.value.filter(i => i.status === 'PENDING' || i.status === 'IN_PROGRESS')
  if (pending.length === 0) {
    ElMessage.warning(t('replacement.noItemsToCheck'))
    return
  }
  router.push(`/admin/replacement/orders/${orderId}/self-check`)
}

const handleSubmitReview = () => confirmAction(
  () => submitReviewReplacementOrder(orderId),
  t('replacement.submitReview'),
)

const handleApprove = async () => {
  const { value: comment } = await ElMessageBox.prompt(t('replacement.notes') + ':', t('replacement.approve'), { type: 'info' })
  await approveReplacementOrder(orderId, comment)
  ElMessage.success(t('replacement.operationSuccess'))
  fetchData()
}

const handleReturn = async () => {
  const { value: comment } = await ElMessageBox.prompt(t('replacement.dispatchReason') + ':', t('replacement.returnOrder'), { type: 'warning' })
  if (!comment) return
  await returnReplacementOrder(orderId, comment)
  ElMessage.success(t('replacement.operationSuccess'))
  fetchData()
}

const handleResubmit = () => confirmAction(
  () => resubmitReplacementOrder(orderId),
  t('replacement.resubmit'),
)

// ──────────── Items ────────────
const addItemVisible = ref(false)
const specCompareItem = ref<ReplacementItemResponse | null>(null)

const openAddItem = () => {
  addItemVisible.value = true
}

const handleAddItem = async (data: ReplacementItemRequest) => {
  await addReplacementItem(orderId, data)
  ElMessage.success(t('replacement.operationSuccess'))
  fetchData()
}

const handleDeleteItem = async (itemId: number) => {
  await ElMessageBox.confirm(t('replacement.confirmAction'), { type: 'warning' })
  await deleteReplacementItem(orderId, itemId)
  ElMessage.success(t('replacement.operationSuccess'))
  fetchData()
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <el-button class="back-btn" @click="router.back()"><ArrowLeft :size="16" /></el-button>
        <div class="header-icon"><Repeat :size="20" /></div>
        <div>
          <h2 class="header-title">{{ order?.orderNumber ?? t('replacement.orderDetail') }}</h2>
          <p class="header-subtitle">{{ t('replacement.orderDetail') }}</p>
        </div>
        <span v-if="order" class="status-badge" :class="statusClass(order.status)" style="margin-left: 12px">
          {{ statusLabel(order.status) }}
        </span>
      </div>
    </div>

    <template v-if="order">
      <!-- Info Card -->
      <div class="detail-card">
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">{{ t('replacement.orderType') }}</span>
            <span class="detail-value">{{ typeLabel(order.orderType) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ t('replacement.location') }}</span>
            <span class="detail-value">{{ order.location ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ t('replacement.assignedContractor') }}</span>
            <span class="detail-value">{{ order.assignedContractor ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ t('replacement.expectedQuantity') }}</span>
            <span class="detail-value">{{ order.expectedQuantity ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ t('replacement.workPeriodStart') }}</span>
            <span class="detail-value">{{ order.workPeriodStart ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ t('replacement.workPeriodEnd') }}</span>
            <span class="detail-value">{{ order.workPeriodEnd ?? '-' }}</span>
          </div>
          <div class="detail-item" style="grid-column: 1 / -1">
            <span class="detail-label">{{ t('replacement.dispatchReason') }}</span>
            <span class="detail-value">{{ order.dispatchReason ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ t('replacement.createdAt') }}</span>
            <span class="detail-value">{{ formatDateTime(order.createdAt) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">{{ t('replacement.updatedAt') }}</span>
            <span class="detail-value">{{ formatDateTime(order.updatedAt) }}</span>
          </div>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="action-bar">
        <el-button v-if="canDispatch" class="action-btn primary" @click="handleDispatch">{{ t('replacement.dispatch') }}</el-button>
        <el-button v-if="canStartWork" class="action-btn primary" @click="handleStartWork">{{ t('replacement.startWork') }}</el-button>
        <el-button v-if="canSelfCheck" class="action-btn warning" @click="handleSelfCheck">{{ t('replacement.selfCheck') }}</el-button>
        <el-button v-if="canSubmitReview" class="action-btn primary" @click="handleSubmitReview">{{ t('replacement.submitReview') }}</el-button>
        <el-button v-if="canApprove" class="action-btn success" @click="handleApprove">{{ t('replacement.approve') }}</el-button>
        <el-button v-if="canReturn" class="action-btn danger" @click="handleReturn">{{ t('replacement.returnOrder') }}</el-button>
        <el-button v-if="canResubmit" class="action-btn warning" @click="handleResubmit">{{ t('replacement.resubmit') }}</el-button>
      </div>

      <!-- Items Section -->
      <div class="section-header">
        <h3 class="section-title">{{ t('replacement.itemTitle') }}</h3>
        <el-button v-if="canEdit || canSelfCheck" class="create-btn-sm" @click="openAddItem">
          <Plus :size="14" style="margin-right: 4px" /> {{ t('common.add') }}
        </el-button>
      </div>

      <div class="table-card">
        <el-table :data="items" stripe>
          <el-table-column :label="t('replacement.parentDevice')" width="140">
            <template #default="{ row }">{{ row.parentDeviceCode || '#' + row.parentDeviceId }}</template>
          </el-table-column>
          <el-table-column :label="t('replacement.oldDevice')" width="140">
            <template #default="{ row }">{{ row.oldDeviceCode || '#' + row.oldDeviceId }}</template>
          </el-table-column>
          <el-table-column :label="t('replacement.newDevice')" width="140">
            <template #default="{ row }">{{ row.newDeviceCode || (row.newDeviceId ? '#' + row.newDeviceId : '-') }}</template>
          </el-table-column>
          <el-table-column prop="beforeDeviceType" :label="t('replacement.beforeSpec')" />
          <el-table-column prop="afterDeviceType" :label="t('replacement.afterSpec')" />
          <el-table-column :label="t('replacement.filterStatus')" width="100">
            <template #default="{ row }">
              <span class="status-badge" :class="row.status === 'COMPLETED' ? 'status-success' : 'status-info'">
                {{ itemStatusLabel(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column width="80" align="center">
            <template #default="{ row }">
              <el-button v-if="row.status === 'COMPLETED'" text @click="specCompareItem = row">
                <GitCompare :size="14" style="color: #409eff" />
              </el-button>
              <el-button v-if="canEdit" text @click="handleDeleteItem(row.id)">
                <Trash2 :size="14" class="text-danger" />
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Spec Comparison Dialog -->
      <el-dialog v-model="specCompareItem" :title="t('replacement.specBefore') + ' / ' + t('replacement.specAfter')" width="640px" class="dark-dialog" @close="specCompareItem = null">
        <BeforeAfterSpecComparison
          v-if="specCompareItem"
          :old-device-code="specCompareItem.oldDeviceCode"
          :new-device-code="specCompareItem.newDeviceCode"
          :before-device-type="specCompareItem.beforeDeviceType"
          :after-device-type="specCompareItem.afterDeviceType"
          :before-spec="specCompareItem.beforeSpec"
          :after-spec="specCompareItem.afterSpec"
          :parent-device-code="specCompareItem.parentDeviceCode"
        />
      </el-dialog>
    </template>

    <!-- Add Item Dialog -->
    <ReplacementItemDialog v-model:visible="addItemVisible" @confirm="handleAddItem" />
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 14px; }
.back-btn { width: 36px; height: 36px; border-radius: 8px; padding: 0; display: flex; align-items: center; justify-content: center; }
.header-icon {
  width: 40px; height: 40px;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #409eff;
}
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); margin: 4px 0 0; }
.detail-card {
  border-radius: 12px;
  border: 1px solid var(--bg-active);
  background: var(--bg-surface);
  padding: 24px;
  margin-bottom: 20px;
}
.detail-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 20px; }
.detail-label { display: block; font-size: 12px; color: var(--text-secondary); margin-bottom: 4px; }
.detail-value { display: block; font-size: 14px; font-weight: 500; color: var(--text-heading); }
.action-bar { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
.action-btn { border-radius: 8px; font-weight: 600; }
.action-btn.primary { background: #409eff; color: #fff; border: none; }
.action-btn.success { background: #5fc992; color: #fff; border: none; }
.action-btn.warning { background: #e6a23c; color: #fff; border: none; }
.action-btn.danger { background: #ff6363; color: #fff; border: none; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font-size: 16px; font-weight: 700; margin: 0; color: var(--text-heading); }
.table-card { border-radius: 12px; overflow: hidden; border: 1px solid var(--bg-active); }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-warning { background: rgba(230, 162, 60, 0.15); color: #e6a23c; }
.status-danger { background: rgba(255, 99, 99, 0.15); color: #ff6363; }
.status-info { background: rgba(64, 158, 255, 0.15); color: #409eff; }
.status-primary { background: rgba(128, 100, 255, 0.15); color: #8064ff; }
.text-danger { color: #ff6363; }
:deep(.el-table) { --el-table-bg-color: var(--bg-surface); --el-table-tr-bg-color: var(--bg-surface); }
:deep(.el-table__row--striped td.el-table__cell) { background: var(--bg-base) !important; }
</style>
