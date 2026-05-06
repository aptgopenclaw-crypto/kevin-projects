<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calculator, Search } from 'lucide-vue-next'
import { calculateKpi, getResults, getIndicators } from '@/api/kpi'
import type { KpiResultResponse, KpiIndicatorResponse } from '@/types/kpi'
import { formatDateTime } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const calcLoading = ref(false)
const filterYear = ref<number>(new Date().getFullYear())
const filterMonth = ref<number>(new Date().getMonth() + 1)
const filterIndicatorId = ref<number | ''>('')
const pageSize = ref(20)
const items = ref<KpiResultResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })
const indicatorOptions = ref<KpiIndicatorResponse[]>([])

// Calculate form
const calcYear = ref(new Date().getFullYear())
const calcMonth = ref(new Date().getMonth())  // last month
const calcIndicatorId = ref<number | ''>('')

async function fetchIndicators() {
  try {
    const res = await getIndicators({ size: 200 })
    indicatorOptions.value = res.body.content
  } catch { /* ignore */ }
}

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getResults({
      periodYear: filterYear.value || undefined,
      periodMonth: filterMonth.value || undefined,
      indicatorId: filterIndicatorId.value || undefined,
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
    ElMessage.error(t('kpi.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) {
  fetchData(p - 1)
}

async function handleCalculate() {
  try {
    await ElMessageBox.confirm(
      t('kpi.calculateConfirm', { year: calcYear.value, month: calcMonth.value }),
      t('kpi.calculate'),
      { type: 'info' },
    )
  } catch { return }

  calcLoading.value = true
  try {
    const res = await calculateKpi(
      calcYear.value,
      calcMonth.value,
      calcIndicatorId.value || undefined,
    )
    ElMessage.success(t('kpi.calculateSuccess', { count: res.body }))
    // Refresh results
    filterYear.value = calcYear.value
    filterMonth.value = calcMonth.value
    fetchData()
  } catch {
    ElMessage.error(t('kpi.calculateFailed'))
  } finally {
    calcLoading.value = false
  }
}

onMounted(() => {
  fetchIndicators()
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <Calculator :size="22" />
        <h2>{{ t('kpi.calculateTitle') }}</h2>
      </div>
    </div>

    <!-- Calculate Panel -->
    <el-card shadow="never" style="margin-bottom: 20px">
      <template #header>
        <span>{{ t('kpi.triggerCalculation') }}</span>
      </template>
      <div class="calc-bar">
        <el-input-number v-model="calcYear" :min="2020" :max="2099" controls-position="right" style="width: 120px" />
        <el-select v-model="calcMonth" :placeholder="t('kpi.month')" style="width: 100px">
          <el-option v-for="m in 12" :key="m" :label="`${m} 月`" :value="m" />
        </el-select>
        <el-select v-model="calcIndicatorId" :placeholder="t('kpi.allIndicators')" clearable style="width: 200px">
          <el-option v-for="ind in indicatorOptions" :key="ind.id" :label="ind.indicatorName" :value="ind.id" />
        </el-select>
        <el-button type="primary" :loading="calcLoading" @click="handleCalculate">
          <Calculator :size="16" />
          {{ t('kpi.doCalculate') }}
        </el-button>
      </div>
    </el-card>

    <!-- Results Table -->
    <div class="filter-bar">
      <el-input-number v-model="filterYear" :min="2020" :max="2099" controls-position="right" style="width: 120px" />
      <el-select v-model="filterMonth" :placeholder="t('kpi.month')" style="width: 100px">
        <el-option v-for="m in 12" :key="m" :label="`${m} 月`" :value="m" />
      </el-select>
      <el-select v-model="filterIndicatorId" :placeholder="t('kpi.indicator')" clearable style="width: 200px">
        <el-option v-for="ind in indicatorOptions" :key="ind.id" :label="ind.indicatorName" :value="ind.id" />
      </el-select>
      <el-button @click="fetchData()">
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
      <el-table-column :label="t('kpi.contractId')" width="100" align="right">
        <template #default="{ row }">{{ row.contractId ?? t('kpi.cityLevel') }}</template>
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

    <div class="pagination-bar">
      <el-pagination
        :current-page="pagination.page + 1"
        :page-size="pageSize"
        :total="pagination.totalElements"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 8px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.calc-bar { display: flex; gap: 12px; align-items: center; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.text-success { color: #059669; font-weight: 600; }
.text-danger { color: #dc2626; font-weight: 600; }
</style>
