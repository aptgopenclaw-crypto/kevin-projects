<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { FileText, Download } from 'lucide-vue-next'
import { getMonthlyReport, getYearlyReport, exportReportXls, exportReportCsv } from '@/api/kpi'
import type { MonthlyReportResponse, YearlyReportResponse } from '@/types/kpi'

const { t } = useI18n()

const loading = ref(false)
const reportType = ref<'monthly' | 'yearly'>('monthly')
const year = ref(new Date().getFullYear())
const month = ref(new Date().getMonth() + 1)
const contractId = ref<number | ''>('')

const monthlyReport = ref<MonthlyReportResponse | null>(null)
const yearlyReport = ref<YearlyReportResponse | null>(null)

async function fetchReport() {
  loading.value = true
  monthlyReport.value = null
  yearlyReport.value = null
  try {
    if (reportType.value === 'monthly') {
      const res = await getMonthlyReport(year.value, month.value, contractId.value || undefined)
      monthlyReport.value = res.body
    } else {
      const res = await getYearlyReport(year.value, contractId.value || undefined)
      yearlyReport.value = res.body
    }
  } catch {
    ElMessage.error(t('kpi.loadFailed'))
  } finally {
    loading.value = false
  }
}

function downloadBlob(data: Blob, filename: string) {
  const url = URL.createObjectURL(data)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function handleExportXls() {
  try {
    const res = await exportReportXls(year.value, month.value, contractId.value || undefined)
    downloadBlob(res.data, `kpi-report-${year.value}-${String(month.value).padStart(2, '0')}.xlsx`)
  } catch {
    ElMessage.error(t('kpi.exportFailed'))
  }
}

async function handleExportCsv() {
  try {
    const res = await exportReportCsv(year.value, month.value, contractId.value || undefined)
    downloadBlob(res.data, `kpi-report-${year.value}-${String(month.value).padStart(2, '0')}.csv`)
  } catch {
    ElMessage.error(t('kpi.exportFailed'))
  }
}

onMounted(() => fetchReport())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <FileText :size="22" />
        <h2>{{ t('kpi.reportTitle') }}</h2>
      </div>
      <div class="header-actions">
        <el-button @click="handleExportXls">
          <Download :size="16" />
          Excel
        </el-button>
        <el-button @click="handleExportCsv">
          <Download :size="16" />
          CSV
        </el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="reportType" @change="fetchReport">
        <el-radio-button value="monthly">{{ t('kpi.monthlyReport') }}</el-radio-button>
        <el-radio-button value="yearly">{{ t('kpi.yearlyReport') }}</el-radio-button>
      </el-radio-group>
      <el-input-number v-model="year" :min="2020" :max="2099" controls-position="right" style="width: 120px" />
      <el-select v-if="reportType === 'monthly'" v-model="month" style="width: 100px">
        <el-option v-for="m in 12" :key="m" :label="`${m} 月`" :value="m" />
      </el-select>
      <el-input-number v-model="contractId" :placeholder="t('kpi.contractId')" :min="0" controls-position="right" style="width: 140px" />
      <el-button type="primary" @click="fetchReport">{{ t('common.query') }}</el-button>
    </div>

    <!-- Monthly Report -->
    <template v-if="reportType === 'monthly' && monthlyReport">
      <el-descriptions :column="3" border style="margin-bottom: 16px">
        <el-descriptions-item :label="t('kpi.period')">{{ monthlyReport.periodYear }}/{{ String(monthlyReport.periodMonth).padStart(2, '0') }}</el-descriptions-item>
        <el-descriptions-item :label="t('kpi.contractId')">{{ monthlyReport.contractId ?? t('kpi.cityLevel') }}</el-descriptions-item>
        <el-descriptions-item :label="t('kpi.totalWeightedScore')">
          <strong>{{ monthlyReport.totalWeightedScore }}</strong>
        </el-descriptions-item>
      </el-descriptions>

      <el-table v-loading="loading" :data="monthlyReport.indicators" stripe>
        <el-table-column prop="indicatorCode" :label="t('kpi.indicatorCode')" width="140" />
        <el-table-column prop="indicatorName" :label="t('kpi.indicatorName')" min-width="180" />
        <el-table-column :label="t('kpi.rawValue')" width="110" align="right">
          <template #default="{ row }">{{ row.rawValue ?? '-' }}</template>
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
        <el-table-column prop="weight" :label="t('kpi.weight')" width="80" align="right" />
        <el-table-column prop="weightedScore" :label="t('kpi.weightedScore')" width="110" align="right" />
      </el-table>
    </template>

    <!-- Yearly Report -->
    <template v-if="reportType === 'yearly' && yearlyReport">
      <el-card shadow="never" style="margin-bottom: 16px">
        <template #header>{{ t('kpi.monthlyTrend') }}</template>
        <el-table :data="yearlyReport.months" stripe>
          <el-table-column :label="t('kpi.month')" width="80">
            <template #default="{ row }">{{ row.month }} 月</template>
          </el-table-column>
          <el-table-column prop="totalScore" :label="t('kpi.totalScore')" align="right" />
        </el-table>
      </el-card>

      <el-card shadow="never">
        <template #header>{{ t('kpi.indicatorTrend') }}</template>
        <el-table :data="yearlyReport.indicators" stripe>
          <el-table-column prop="indicatorCode" :label="t('kpi.indicatorCode')" width="140" />
          <el-table-column prop="indicatorName" :label="t('kpi.indicatorName')" min-width="160" />
          <el-table-column v-for="m in 12" :key="m" :label="`${m}月`" width="80" align="right">
            <template #default="{ row }">{{ row.monthlyValues[m - 1] ?? '-' }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 8px; }
.header-actions { display: flex; gap: 8px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.text-success { color: #059669; font-weight: 600; }
.text-danger { color: #dc2626; font-weight: 600; }
</style>
