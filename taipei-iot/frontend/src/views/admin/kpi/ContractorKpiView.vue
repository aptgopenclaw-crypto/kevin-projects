<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { UserCheck, Search } from 'lucide-vue-next'
import { getContractorResults } from '@/api/kpi'
import type { KpiResultResponse } from '@/types/kpi'
import { formatDateTime } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const year = ref(new Date().getFullYear())
const contractId = ref<number | ''>('')
const items = ref<KpiResultResponse[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getContractorResults({
      year: year.value || undefined,
      contractId: contractId.value || undefined,
    })
    items.value = res.body
  } catch {
    ElMessage.error(t('kpi.loadFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <UserCheck :size="22" />
        <h2>{{ t('kpi.contractorKpiTitle') }}</h2>
      </div>
    </div>

    <div class="filter-bar">
      <el-input-number v-model="year" :min="2020" :max="2099" controls-position="right" style="width: 120px" />
      <el-input-number v-model="contractId" :placeholder="t('kpi.contractId')" :min="0" controls-position="right" style="width: 140px" />
      <el-button type="primary" @click="fetchData">
        <Search :size="16" />
        {{ t('common.query') }}
      </el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="indicatorCode" :label="t('kpi.indicatorCode')" width="140" />
      <el-table-column prop="indicatorName" :label="t('kpi.indicatorName')" min-width="180" />
      <el-table-column :label="t('kpi.period')" width="110">
        <template #default="{ row }">{{ row.periodYear }}/{{ String(row.periodMonth).padStart(2, '0') }}</template>
      </el-table-column>
      <el-table-column prop="resultValue" :label="t('kpi.resultValue')" width="110" align="right" />
      <el-table-column :label="t('kpi.targetValue')" width="100" align="right">
        <template #default="{ row }">{{ row.targetValue ?? '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('kpi.achievement')" width="110" align="right">
        <template #default="{ row }">
          <span v-if="row.achievement != null" :class="row.achievement >= 100 ? 'text-success' : 'text-danger'">
            {{ row.achievement.toFixed(2) }}%
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('kpi.weight')" width="80" align="right">
        <template #default="{ row }">{{ row.weight ?? '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('kpi.calculatedAt')" width="170">
        <template #default="{ row }">{{ formatDateTime(row.calculatedAt) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 8px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.text-success { color: #059669; font-weight: 600; }
.text-danger { color: #dc2626; font-weight: 600; }
</style>
