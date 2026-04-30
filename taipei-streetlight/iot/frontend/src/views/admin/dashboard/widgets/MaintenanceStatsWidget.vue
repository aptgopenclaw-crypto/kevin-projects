<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import StatCard from '@/views/admin/dashboard/components/StatCard.vue'
import { getMaintenanceStats, getMaintenanceTrend } from '@/api/dashboard'
import type { MaintenanceStatsResponse, MonthlyPoint } from '@/types/dashboard'
import dayjs from 'dayjs'

const { t } = useI18n()
const router = useRouter()
const loading = ref(false)
const stats = ref<MaintenanceStatsResponse | null>(null)
const activeTab = ref<'trend' | 'source' | 'fault'>('trend')

const trendOption = ref({})
const sourceOption = ref({})
const faultOption = ref({})

function buildTrendChart(months: MonthlyPoint[]) {
  trendOption.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: [t('dashboard.repairCount'), t('dashboard.completionRate')], bottom: 0 },
    grid: { top: 10, right: 40, bottom: 30, left: 40, containLabel: true },
    xAxis: { type: 'category', data: months.map(m => m.month) },
    yAxis: [
      { type: 'value', name: t('dashboard.repairCount'), splitLine: { lineStyle: { type: 'dashed' } } },
      { type: 'value', name: '%', max: 100, splitLine: { show: false } },
    ],
    series: [
      { name: t('dashboard.repairCount'), type: 'bar', data: months.map(m => m.repairCount), itemStyle: { borderRadius: [4, 4, 0, 0] } },
      { name: t('dashboard.completionRate'), type: 'line', yAxisIndex: 1, data: months.map(m => m.completionRate), smooth: true },
    ],
  }
}

function buildSourceChart(dist: Record<string, number>) {
  sourceOption.value = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center' },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['35%', '50%'],
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14 } },
      data: Object.entries(dist).map(([name, value]) => ({ name, value })),
    }],
  }
}

function buildFaultChart(dist: Record<string, number>) {
  const entries = Object.entries(dist)
  faultOption.value = {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 20, left: 10, containLabel: true },
    xAxis: { type: 'category', data: entries.map(([k]) => k), axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    series: [{ type: 'bar', data: entries.map(([, v]) => v), itemStyle: { borderRadius: [4, 4, 0, 0], color: '#e6a23c' } }],
  }
}

async function fetchData() {
  loading.value = true
  try {
    const end = dayjs().format('YYYY-MM-DD')
    const start = dayjs().subtract(6, 'month').startOf('month').format('YYYY-MM-DD')
    const [statsRes, trendRes] = await Promise.all([
      getMaintenanceStats(),
      getMaintenanceTrend({ startDate: start, endDate: end }),
    ])
    stats.value = statsRes.body
    buildTrendChart(trendRes.body.months)
    buildSourceChart(statsRes.body.sourceDistribution)
    buildFaultChart(statsRes.body.faultCategoryDistribution)
  } catch {
    ElMessage.error(t('dashboard.loadFailed'))
  } finally {
    loading.value = false
  }
}

function drillDown() {
  router.push('/admin/repair/list')
}

onMounted(() => fetchData())
</script>

<template>
  <div v-loading="loading" class="maintenance-widget">
    <template v-if="stats">
      <div class="stat-cards">
        <StatCard :title="t('dashboard.totalRepairs')" :value="stats.totalRepairs" clickable @click="drillDown" />
        <StatCard :title="t('dashboard.completedRepairs')" :value="stats.completedRepairs" color="#67c23a" />
        <StatCard :title="t('dashboard.pendingRepairs')" :value="stats.pendingRepairs" color="#e6a23c" />
        <StatCard :title="t('dashboard.completionRate')" :value="`${stats.completionRate}%`" />
        <StatCard :title="t('dashboard.avgRepairHours')" :value="stats.avgRepairHours" unit="hr" />
        <StatCard :title="t('dashboard.illuminationRate')" :value="`${stats.illuminationRate}%`" color="#67c23a" />
      </div>
      <el-tabs v-model="activeTab" class="chart-tabs">
        <el-tab-pane :label="t('dashboard.trendChart')" name="trend">
          <VChart :option="trendOption" autoresize class="tab-chart" />
        </el-tab-pane>
        <el-tab-pane :label="t('dashboard.sourceDistribution')" name="source">
          <VChart :option="sourceOption" autoresize class="tab-chart" />
        </el-tab-pane>
        <el-tab-pane :label="t('dashboard.faultDistribution')" name="fault">
          <VChart :option="faultOption" autoresize class="tab-chart" />
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<style scoped>
.maintenance-widget { height: 100%; display: flex; flex-direction: column; }
.stat-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 8px; }
.chart-tabs { flex: 1; display: flex; flex-direction: column; }
.chart-tabs :deep(.el-tabs__content) { flex: 1; }
.chart-tabs :deep(.el-tab-pane) { height: 100%; }
.tab-chart { width: 100%; height: 100%; min-height: 160px; }
</style>
