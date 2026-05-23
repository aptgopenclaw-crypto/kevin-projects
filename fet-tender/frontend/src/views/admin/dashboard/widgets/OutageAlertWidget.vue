<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { AlertTriangle } from 'lucide-vue-next'
import VChart from 'vue-echarts'
import StatCard from '@/views/admin/dashboard/components/StatCard.vue'
import { getOutageAlert, getOutageTrend } from '@/api/dashboard'
import type { OutageAlertResponse, OutageTrendResponse } from '@/types/dashboard'
import { formatDateTime } from '@/utils/datetime'
import { useDashboardWsInject } from '@/views/admin/dashboard/composables/useDashboardWsInject'
import dayjs from 'dayjs'

const { t } = useI18n()
const loading = ref(false)
const data = ref<OutageAlertResponse | null>(null)
const activeTab = ref<'alerts' | 'trend'>('alerts')
const trendOption = ref({})
const pulse = ref(false)

// ── WebSocket realtime ──
const ws = useDashboardWsInject()
let unsubscribe: (() => void) | undefined
if (ws) {
  unsubscribe = ws.onWidgetUpdate('outage-alert', (wsData) => {
    data.value = wsData as OutageAlertResponse
    triggerPulse()
  })
}

function triggerPulse() {
  pulse.value = true
  setTimeout(() => { pulse.value = false }, 1500)
}

onBeforeUnmount(() => { unsubscribe?.() })

function buildTrendChart(trend: OutageTrendResponse) {
  trendOption.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: [t('dashboard.outageCount'), t('dashboard.avgRecoveryHours')], bottom: 0 },
    grid: { top: 10, right: 40, bottom: 30, left: 40, containLabel: true },
    xAxis: { type: 'category', data: trend.months.map(m => m.month) },
    yAxis: [
      { type: 'value', name: t('dashboard.outageCount'), splitLine: { lineStyle: { type: 'dashed' } } },
      { type: 'value', name: 'hr', splitLine: { show: false } },
    ],
    series: [
      { name: t('dashboard.outageCount'), type: 'bar', data: trend.months.map(m => m.outageCount), itemStyle: { borderRadius: [4, 4, 0, 0], color: '#f56c6c' } },
      { name: t('dashboard.avgRecoveryHours'), type: 'line', yAxisIndex: 1, data: trend.months.map(m => m.avgRecoveryHours), smooth: true, lineStyle: { color: '#e6a23c' }, itemStyle: { color: '#e6a23c' } },
    ],
  }
}

async function fetchData() {
  loading.value = true
  try {
    const end = dayjs().format('YYYY-MM-DD')
    const start = dayjs().subtract(6, 'month').startOf('month').format('YYYY-MM-DD')
    const [alertRes, trendRes] = await Promise.all([
      getOutageAlert(),
      getOutageTrend({ startDate: start, endDate: end }),
    ])
    data.value = alertRes.body
    buildTrendChart(trendRes.body)
  } catch {
    ElMessage.error(t('dashboard.loadFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div v-loading="loading" class="outage-widget" :class="{ 'widget-pulse': pulse }">
    <template v-if="data">
      <div class="alert-header">
        <AlertTriangle :size="20" class="alert-icon" />
        <StatCard
          :title="t('dashboard.currentOutages')"
          :value="data.currentOutageCount"
          :color="data.currentOutageCount > 0 ? '#f56c6c' : '#67c23a'"
        />
      </div>
      <el-tabs v-model="activeTab" class="chart-tabs">
        <el-tab-pane :label="t('dashboard.alertList')" name="alerts">
          <el-table :data="data.outageZones" size="small" stripe max-height="200">
            <el-table-column prop="zone" :label="t('dashboard.zone')" />
            <el-table-column prop="affectedCount" :label="t('dashboard.affectedCount')" width="80" align="center" />
            <el-table-column :label="t('dashboard.since')" width="160">
              <template #default="{ row }">{{ formatDateTime(row.since) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="t('dashboard.trendChart')" name="trend">
          <VChart :option="trendOption" autoresize class="tab-chart" />
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<style scoped>
.outage-widget { height: 100%; display: flex; flex-direction: column; }
.alert-header { margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }
.alert-icon { color: var(--el-color-danger); flex-shrink: 0; }
.alert-header :deep(.stat-card) { flex: 1; }
.chart-tabs { flex: 1; display: flex; flex-direction: column; }
.chart-tabs :deep(.el-tabs__content) { flex: 1; }
.chart-tabs :deep(.el-tab-pane) { height: 100%; }
.tab-chart { width: 100%; height: 100%; min-height: 160px; }
.widget-pulse { animation: widget-pulse-anim 1.5s ease-out; }
@keyframes widget-pulse-anim {
  0% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.5); }
  100% { box-shadow: 0 0 0 0 transparent; }
}
</style>
