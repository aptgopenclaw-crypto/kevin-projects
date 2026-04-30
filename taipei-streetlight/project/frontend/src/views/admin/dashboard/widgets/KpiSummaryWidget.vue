<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { getKpiSummary, getKpiTrend } from '@/api/dashboard'
import type { KpiIndicatorSummary, KpiTrendResponse } from '@/types/dashboard'
import dayjs from 'dayjs'

const { t } = useI18n()
const router = useRouter()
const loading = ref(false)
const indicators = ref<KpiIndicatorSummary[]>([])
const activeTab = ref<'table' | 'gauge' | 'trend'>('table')

const gaugeOption = ref({})
const trendOption = ref({})

function gradeColor(grade: string) {
  const map: Record<string, string> = { A: '#67c23a', B: '#409eff', C: '#e6a23c', D: '#f56c6c', F: '#909399' }
  return map[grade] || '#909399'
}

function buildGaugeChart(inds: KpiIndicatorSummary[]) {
  const top3 = inds.slice(0, 3)
  gaugeOption.value = {
    series: top3.map((ind, i) => ({
      type: 'gauge',
      center: [`${20 + i * 30}%`, '55%'],
      radius: '40%',
      startAngle: 210,
      endAngle: -30,
      min: 0,
      max: 100,
      detail: { formatter: `{value}%`, fontSize: 14, offsetCenter: [0, '70%'] },
      title: { offsetCenter: [0, '95%'], fontSize: 12 },
      data: [{ value: ind.achievement ?? 0, name: ind.name }],
      axisLine: { lineStyle: { width: 10, color: [[0.6, '#f56c6c'], [0.8, '#e6a23c'], [1, '#67c23a']] } },
      pointer: { length: '60%', width: 4 },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
    })),
  }
}

function buildTrendChart(trend: KpiTrendResponse) {
  const months = trend.months.map(m => m.month)
  const allCodes = new Set<string>()
  trend.months.forEach(m => m.indicators.forEach(ind => allCodes.add(ind.code)))
  const series = [...allCodes].map(code => ({
    name: code,
    type: 'line' as const,
    smooth: true,
    data: trend.months.map(m => {
      const found = m.indicators.find(i => i.code === code)
      return found ? found.value : null
    }),
  }))
  trendOption.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: [...allCodes], bottom: 0 },
    grid: { top: 10, right: 10, bottom: 30, left: 10, containLabel: true },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    series,
  }
}

async function fetchData() {
  loading.value = true
  try {
    const now = dayjs()
    const [summaryRes, trendRes] = await Promise.all([
      getKpiSummary({ year: now.year(), month: now.month() + 1 }),
      getKpiTrend({ year: now.year(), month: now.month() + 1, months: 6 }),
    ])
    indicators.value = summaryRes.body.indicators
    buildGaugeChart(summaryRes.body.indicators)
    buildTrendChart(trendRes.body)
  } catch {
    ElMessage.error(t('dashboard.loadFailed'))
  } finally {
    loading.value = false
  }
}

function drillDown() {
  router.push('/admin/kpi/reports')
}

onMounted(() => fetchData())
</script>

<template>
  <div v-loading="loading" class="kpi-widget">
    <el-tabs v-model="activeTab" class="chart-tabs">
      <el-tab-pane :label="t('dashboard.kpiTable')" name="table">
        <el-table :data="indicators" size="small" stripe max-height="250" @row-click="drillDown" style="cursor: pointer;">
          <el-table-column prop="name" :label="t('dashboard.kpiName')" />
          <el-table-column prop="value" :label="t('dashboard.kpiValue')" width="70" align="right" />
          <el-table-column prop="target" :label="t('dashboard.kpiTarget')" width="70" align="right" />
          <el-table-column :label="t('dashboard.kpiAchievement')" width="80" align="right">
            <template #default="{ row }">{{ row.achievement != null ? `${row.achievement}%` : '-' }}</template>
          </el-table-column>
          <el-table-column :label="t('dashboard.kpiGrade')" width="55" align="center">
            <template #default="{ row }">
              <el-tag :color="gradeColor(row.grade)" effect="dark" size="small">{{ row.grade }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane :label="t('dashboard.gaugeChart')" name="gauge">
        <VChart :option="gaugeOption" autoresize class="tab-chart" />
      </el-tab-pane>
      <el-tab-pane :label="t('dashboard.trendChart')" name="trend">
        <VChart :option="trendOption" autoresize class="tab-chart" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.kpi-widget { height: 100%; display: flex; flex-direction: column; }
.chart-tabs { flex: 1; display: flex; flex-direction: column; }
.chart-tabs :deep(.el-tabs__content) { flex: 1; }
.chart-tabs :deep(.el-tab-pane) { height: 100%; }
.tab-chart { width: 100%; height: 100%; min-height: 180px; }
</style>
