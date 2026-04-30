<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { LayoutDashboard, ArrowRight, ClipboardList, Inbox } from 'lucide-vue-next'
import { getPendingTasks } from '@/api/workflow'
import type { WorkflowInstanceResponse, WorkflowType, TicketType } from '@/types/workflow'

const { t } = useI18n()
const router = useRouter()

const loading = ref(false)
const tasks = ref<WorkflowInstanceResponse[]>([])
const totalPending = ref(0)

const wfTypeLabel = (wt: WorkflowType) => {
  const map: Record<WorkflowType, string> = {
    FAULT_REVIEW: t('workflow.typeFaultReview'),
    REPAIR_DISPATCH: t('workflow.typeRepairDispatch'),
    REPAIR_CLOSE: t('workflow.typeRepairClose'),
    REPLACEMENT_REVIEW: t('workflow.typeReplacementReview'),
    ASSET_CHANGE: t('workflow.typeAssetChange'),
  }
  return map[wt] ?? wt
}

function resolveDetailRoute(row: WorkflowInstanceResponse): string {
  const tt: TicketType = row.ticketType
  const id = row.ticketId
  switch (tt) {
    case 'REPAIR_TICKET':       return `/admin/repair/tickets/${id}`
    case 'REPLACEMENT_ORDER':   return `/admin/replacement/orders/${id}`
    case 'FAULT_TICKET':        return `/admin/asset/faults`
    default:                    return `/admin/workflow/pending`
  }
}

function goToDetail(row: WorkflowInstanceResponse) {
  router.push(resolveDetailRoute(row))
}

async function loadPending() {
  loading.value = true
  try {
    const res = await getPendingTasks({ page: 0, size: 10 })
    tasks.value = res.body.content
    totalPending.value = res.body.totalElements
  } catch {
    ElMessage.error(t('home.pendingLoadFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(() => loadPending())
</script>

<template>
  <div class="page-container">
    <div class="page-content">
      <!-- Header -->
      <div class="page-header">
        <div class="header-left">
          <div class="header-icon"><LayoutDashboard :size="20" /></div>
          <div>
            <h2 class="header-title">{{ t('home.title') }}</h2>
            <p class="header-subtitle">{{ t('home.subtitle') }}</p>
          </div>
        </div>
      </div>

      <!-- Pending Tasks Card -->
      <div class="dashboard-card">
        <div class="card-header">
          <div class="card-header-left">
            <ClipboardList :size="18" class="card-icon" />
            <h3 class="card-title">{{ t('home.pendingTitle') }}</h3>
            <span v-if="totalPending > 0" class="badge">{{ totalPending }}</span>
          </div>
          <router-link to="/admin/workflow/pending" class="view-all-link">
            {{ t('home.pendingViewAll') }} <ArrowRight :size="14" />
          </router-link>
        </div>

        <div v-loading="loading" class="card-body">
          <!-- Empty state -->
          <div v-if="!loading && tasks.length === 0" class="empty-state">
            <Inbox :size="40" class="empty-icon" />
            <p>{{ t('home.pendingEmpty') }}</p>
          </div>

          <!-- Task table -->
          <el-table v-else :data="tasks" stripe :show-header="true">
            <el-table-column prop="ticketId" :label="t('home.colTicketId')" width="100" />
            <el-table-column :label="t('home.colType')" width="140">
              <template #default="{ row }">{{ wfTypeLabel(row.workflowType) }}</template>
            </el-table-column>
            <el-table-column prop="currentStep" :label="t('home.colStep')" width="130">
              <template #default="{ row }"><span class="code-text">{{ row.currentStep }}</span></template>
            </el-table-column>
            <el-table-column prop="startedAt" :label="t('home.colTime')" min-width="160" />
            <el-table-column :label="t('common.actions')" width="120" fixed="right">
              <template #default="{ row }">
                <el-button class="action-btn action-go" size="small" @click="goToDetail(row)">
                  {{ t('home.actionGo') }} <ArrowRight :size="12" />
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }
.page-content { max-width: 1200px; margin: 0 auto; }

.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon {
  width: 40px; height: 40px;
  background: rgba(167, 139, 250, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #a78bfa;
}
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); font-family: 'Inter', sans-serif; margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); font-family: 'Inter', sans-serif; margin: 2px 0 0; }

.dashboard-card {
  background: var(--bg-surface);
  border: 1px solid var(--bg-active);
  border-radius: 12px;
  overflow: hidden;
}

.card-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--bg-active);
}
.card-header-left { display: flex; align-items: center; gap: 8px; }
.card-icon { color: #a78bfa; }
.card-title { font-size: 15px; font-weight: 600; color: var(--text-heading); margin: 0; }

.badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 22px; height: 22px; padding: 0 6px;
  border-radius: 11px;
  background: rgba(255, 99, 99, 0.15); color: #FF6363;
  font-size: 12px; font-weight: 700;
}

.view-all-link {
  display: flex; align-items: center; gap: 4px;
  font-size: 13px; color: #a78bfa; text-decoration: none; font-weight: 500;
}
.view-all-link:hover { color: #c4b5fd; }

.card-body { min-height: 120px; }

.empty-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 48px 20px; color: var(--text-secondary);
}
.empty-icon { color: var(--text-secondary); opacity: 0.4; margin-bottom: 12px; }
.empty-state p { font-size: 14px; margin: 0; }

.code-text { color: #55b3ff; font-weight: 600; font-family: 'JetBrains Mono', monospace; font-size: 13px; }

.action-btn { padding: 4px 12px; min-width: auto; background: transparent; border-radius: 6px; }
.action-go {
  color: #a78bfa; border: 1px solid rgba(167, 139, 250, 0.25);
  display: inline-flex; align-items: center; gap: 4px; font-size: 12px;
}
.action-go:hover { background: rgba(167, 139, 250, 0.15); color: #c4b5fd; }
</style>
