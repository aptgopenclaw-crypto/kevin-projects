<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import StatCard from '@/views/admin/dashboard/components/StatCard.vue'
import { getAttachmentStats } from '@/api/dashboard'
import type { AttachmentStatsResponse } from '@/types/dashboard'

const { t } = useI18n()
const loading = ref(false)
const data = ref<AttachmentStatsResponse | null>(null)
const activeTab = ref<'bar' | 'pie'>('bar')

const barOption = ref({})
const pieOption = ref({})

function buildCharts(d: AttachmentStatsResponse) {
  const entries = Object.entries(d.byType)
  barOption.value = {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 20, left: 10, containLabel: true },
    xAxis: { type: 'category', data: entries.map(([k]) => k) },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    series: [{ type: 'bar', data: entries.map(([, v]) => v), itemStyle: { borderRadius: [4, 4, 0, 0] } }],
  }
  pieOption.value = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['35%', '60%'],
      label: { show: false }, emphasis: { label: { show: true } },
      data: entries.map(([name, value]) => ({ name, value })),
    }],
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAttachmentStats()
    data.value = res.body
    buildCharts(res.body)
  } catch {
    ElMessage.error(t('dashboard.loadFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div v-loading="loading" class="attachment-widget">
    <template v-if="data">
      <div class="summary-cards">
        <StatCard :title="t('dashboard.totalFiles')" :value="data.totalCount" />
        <StatCard :title="t('dashboard.totalSize')" :value="`${data.totalSizeMB}`" unit="MB" />
      </div>
      <el-tabs v-model="activeTab" class="chart-tabs">
        <el-tab-pane :label="t('dashboard.barChart')" name="bar">
          <VChart :option="barOption" autoresize class="tab-chart" />
        </el-tab-pane>
        <el-tab-pane :label="t('dashboard.pieChart')" name="pie">
          <VChart :option="pieOption" autoresize class="tab-chart" />
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<style scoped>
.attachment-widget { height: 100%; display: flex; flex-direction: column; }
.summary-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 8px; }
.chart-tabs { flex: 1; display: flex; flex-direction: column; }
.chart-tabs :deep(.el-tabs__content) { flex: 1; }
.chart-tabs :deep(.el-tab-pane) { height: 100%; }
.tab-chart { width: 100%; height: 100%; min-height: 120px; }
</style>
