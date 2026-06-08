<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { getTenderDashboard } from '@/api/tender'
import type { TenderDashboardResponse } from '@/types/tender'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, BarChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const { t } = useI18n()
const router = useRouter()

const loading = ref(false)
const data = ref<TenderDashboardResponse | null>(null)

async function fetchDashboard() {
  loading.value = true
  try {
    const res = await getTenderDashboard()
    if (res.errorCode === '00000') {
      data.value = res.body
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchDashboard)

// ── KPI 計算 ──────────────────────────────────────────────────
function changeRate(current: number, previous: number): string {
  if (previous === 0) return current > 0 ? '+∞' : '0%'
  const rate = ((current - previous) / previous * 100).toFixed(0)
  return Number(rate) > 0 ? `+${rate}%` : `${rate}%`
}

function formatAmount(val: number | null | undefined): string {
  if (!val) return '0'
  if (val >= 100000000) return (val / 100000000).toFixed(1) + ' 億'
  if (val >= 10000) return (val / 10000).toFixed(0) + ' 萬'
  return val.toLocaleString()
}

// ── 圖表選項 ──────────────────────────────────────────────────
const trendChartOption = computed(() => {
  if (!data.value) return {}
  const trend = data.value.announcementTrend
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: trend.map(d => d.date.slice(5)), // MM-DD
      axisLabel: { fontSize: 11 },
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'line',
      data: trend.map(d => d.count),
      smooth: true,
      areaStyle: { opacity: 0.15 },
      itemStyle: { color: '#409EFF' },
    }],
  }
})

const awardChartOption = computed(() => {
  if (!data.value) return {}
  const trend = data.value.awardAmountTrend
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}<br/>決標金額: ${formatAmount(p.value)} 元<br/>件數: ${trend[p.dataIndex]?.count ?? 0}`
      },
    },
    grid: { left: 60, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: trend.map(d => d.month) },
    yAxis: { type: 'value', axisLabel: { formatter: (v: number) => formatAmount(v) } },
    series: [{
      type: 'bar',
      data: trend.map(d => d.amount),
      itemStyle: { color: '#67C23A', borderRadius: [4, 4, 0, 0] },
    }],
  }
})

const pieChartOption = computed(() => {
  if (!data.value) return {}
  const dist = data.value.solutionDistribution
  return {
    color: ['#5470C6', '#91CC75', '#FAC858', '#EE6666', '#73C0DE', '#9A60B4', '#FC8452', '#3BA272', '#EA7CCC', '#36CFC9'],
    tooltip: { trigger: 'item', formatter: '{b}: {c} 件 ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['40%', '50%'],
      data: dist.map(d => ({ name: d.solution, value: d.count })),
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } },
    }],
  }
})

function goToAnnouncements() {
  router.push('/tender/announcements')
}
</script>

<template>
  <div v-loading="loading" class="dashboard-page">
    <h2 class="page-title">{{ $t('tender.dashboard.title') }}</h2>

    <!-- KPI Cards -->
    <div v-if="data" class="kpi-row">
      <el-card shadow="hover" class="kpi-card">
        <div class="kpi-label">{{ $t('tender.dashboard.annThisMonth') }}</div>
        <div class="kpi-value">{{ data.kpiCards.announcementCountThisMonth }}</div>
        <div class="kpi-change" :class="{ negative: data.kpiCards.announcementCountThisMonth < data.kpiCards.announcementCountLastMonth }">
          {{ changeRate(data.kpiCards.announcementCountThisMonth, data.kpiCards.announcementCountLastMonth) }}
          <span class="kpi-sub">vs {{ $t('tender.dashboard.lastMonth') }}</span>
        </div>
      </el-card>

      <el-card shadow="hover" class="kpi-card">
        <div class="kpi-label">{{ $t('tender.dashboard.awardThisMonth') }}</div>
        <div class="kpi-value">{{ data.kpiCards.awardCountThisMonth }}</div>
        <div class="kpi-change" :class="{ negative: data.kpiCards.awardCountThisMonth < data.kpiCards.awardCountLastMonth }">
          {{ changeRate(data.kpiCards.awardCountThisMonth, data.kpiCards.awardCountLastMonth) }}
          <span class="kpi-sub">vs {{ $t('tender.dashboard.lastMonth') }}</span>
        </div>
      </el-card>

      <el-card shadow="hover" class="kpi-card">
        <div class="kpi-label">{{ $t('tender.dashboard.awardAmountThisMonth') }}</div>
        <div class="kpi-value">{{ formatAmount(data.kpiCards.awardAmountThisMonth) }}</div>
        <div class="kpi-change" :class="{ negative: data.kpiCards.awardAmountThisMonth < data.kpiCards.awardAmountLastMonth }">
          {{ changeRate(data.kpiCards.awardAmountThisMonth, data.kpiCards.awardAmountLastMonth) }}
          <span class="kpi-sub">vs {{ $t('tender.dashboard.lastMonth') }}</span>
        </div>
      </el-card>

      <el-card shadow="hover" class="kpi-card">
        <div class="kpi-label">{{ $t('tender.dashboard.activeSolutions') }}</div>
        <div class="kpi-value">{{ data.kpiCards.activeSolutionCount }}</div>
        <div class="kpi-change neutral">{{ $t('tender.dashboard.trackedSolutions') }}</div>
      </el-card>
    </div>

    <!-- Charts Row 1 -->
    <div v-if="data" class="chart-row">
      <el-card shadow="hover" class="chart-card">
        <template #header>
          <span class="chart-title">{{ $t('tender.dashboard.announcementTrend') }}</span>
        </template>
        <v-chart class="chart" :option="trendChartOption" autoresize />
      </el-card>

      <el-card shadow="hover" class="chart-card">
        <template #header>
          <span class="chart-title">{{ $t('tender.dashboard.awardAmountTrend') }}</span>
        </template>
        <v-chart class="chart" :option="awardChartOption" autoresize />
      </el-card>
    </div>

    <!-- Charts Row 2 -->
    <div v-if="data" class="chart-row">
      <el-card shadow="hover" class="chart-card">
        <template #header>
          <span class="chart-title">{{ $t('tender.dashboard.solutionDistribution') }}</span>
        </template>
        <v-chart class="chart" :option="pieChartOption" autoresize />
      </el-card>

      <el-card shadow="hover" class="chart-card">
        <template #header>
          <span class="chart-title">{{ $t('tender.dashboard.recentAnnouncements') }}</span>
        </template>
        <el-table :data="data.recentAnnouncements" size="small" stripe style="width: 100%">
          <el-table-column prop="announcementDate" label="日期" width="100" />
          <el-table-column prop="tenderName" label="標案名稱" min-width="200" show-overflow-tooltip />
          <el-table-column prop="agencyName" label="機關" width="150" show-overflow-tooltip />
          <el-table-column prop="solution" label="方案" width="100" />
          <el-table-column label="預算" width="110" align="right">
            <template #default="{ row }">
              {{ row.budgetAmount ? formatAmount(row.budgetAmount) : '-' }}
            </template>
          </el-table-column>
        </el-table>
        <div class="view-all">
          <el-button type="primary" link size="small" @click="goToAnnouncements">
            {{ $t('tender.dashboard.viewAll') }} →
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page {
  padding: 20px;
}
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  font-weight: 600;
}

/* KPI */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.kpi-card {
  text-align: center;
}
.kpi-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}
.kpi-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}
.kpi-change {
  font-size: 12px;
  margin-top: 4px;
  color: #67C23A;
}
.kpi-change.negative {
  color: #F56C6C;
}
.kpi-change.neutral {
  color: var(--el-text-color-secondary);
}
.kpi-sub {
  color: var(--el-text-color-secondary);
  margin-left: 4px;
}

/* Charts */
.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}
.chart-card {
  min-height: 320px;
}
.chart-title {
  font-weight: 600;
  font-size: 14px;
}
.chart {
  width: 100%;
  height: 250px;
}
.view-all {
  text-align: right;
  margin-top: 8px;
}

@media (max-width: 1200px) {
  .kpi-row { grid-template-columns: repeat(2, 1fr); }
  .chart-row { grid-template-columns: 1fr; }
}
</style>
