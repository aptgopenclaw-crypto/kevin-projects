<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock, Unlock, Calendar } from 'lucide-vue-next'
import { getPeriods, lockPeriod, unlockPeriod } from '@/api/kpi'
import type { PeriodResponse } from '@/types/kpi'
import { formatDateTime } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const items = ref<PeriodResponse[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getPeriods()
    items.value = res.body
  } catch {
    ElMessage.error(t('kpi.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleLock(row: PeriodResponse) {
  try {
    await ElMessageBox.confirm(
      t('kpi.lockConfirm', { period: `${row.periodYear}/${String(row.periodMonth).padStart(2, '0')}` }),
      t('kpi.lockPeriod'),
      { type: 'warning' },
    )
    await lockPeriod(row.periodYear, row.periodMonth)
    ElMessage.success(t('kpi.lockSuccess'))
    fetchData()
  } catch {
    // cancelled or error
  }
}

async function handleUnlock(row: PeriodResponse) {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      t('kpi.unlockReasonPrompt'),
      t('kpi.unlockPeriod'),
      { type: 'warning', inputPlaceholder: t('kpi.unlockReasonPlaceholder') },
    )
    if (!reason) {
      ElMessage.warning(t('kpi.unlockReasonRequired'))
      return
    }
    await unlockPeriod(row.periodYear, row.periodMonth, reason)
    ElMessage.success(t('kpi.unlockSuccess'))
    fetchData()
  } catch {
    // cancelled or error
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <Calendar :size="22" />
        <h2>{{ t('kpi.periodTitle') }}</h2>
      </div>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column :label="t('kpi.period')" width="120">
        <template #default="{ row }">{{ row.periodYear }}/{{ String(row.periodMonth).padStart(2, '0') }}</template>
      </el-table-column>
      <el-table-column :label="t('kpi.lockStatus')" width="100">
        <template #default="{ row }">
          <span v-if="row.locked" class="status-tag status-danger">
            <Lock :size="14" /> {{ t('kpi.locked') }}
          </span>
          <span v-else class="status-tag status-success">
            <Unlock :size="14" /> {{ t('kpi.unlocked') }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="lockedBy" :label="t('kpi.lockedBy')" width="120">
        <template #default="{ row }">{{ row.lockedBy ?? '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('kpi.lockedAt')" width="170">
        <template #default="{ row }">{{ row.lockedAt ? formatDateTime(row.lockedAt) : '-' }}</template>
      </el-table-column>
      <el-table-column prop="unlockReason" :label="t('kpi.unlockReason')" min-width="200">
        <template #default="{ row }">{{ row.unlockReason ?? '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="140" fixed="right">
        <template #default="{ row }">
          <el-button v-if="!row.locked" link type="warning" @click="handleLock(row)">
            <Lock :size="14" />
            {{ t('kpi.lock') }}
          </el-button>
          <el-button v-else link type="primary" @click="handleUnlock(row)">
            <Unlock :size="14" />
            {{ t('kpi.unlock') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 8px; }
.status-tag { display: inline-flex; align-items: center; gap: 4px; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-success { background: #f0f9ff; color: #059669; }
.status-danger { background: #fef2f2; color: #dc2626; }
</style>
