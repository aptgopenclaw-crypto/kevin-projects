<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import StatCard from '@/views/admin/dashboard/components/StatCard.vue'
import { getLampCount } from '@/api/dashboard'
import type { LampCountResponse } from '@/types/dashboard'

const { t } = useI18n()
const router = useRouter()
const loading = ref(false)
const data = ref<LampCountResponse | null>(null)
const activeTab = ref<'type' | 'contractor' | 'lightSource' | 'facility'>('type')

const typeOption = ref({})
const contractorOption = ref({})
const lightSourceOption = ref({})
const facilityOption = ref({})

function buildPie(dist: Record<string, number>, center?: string[]) {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center' },
    series: [{
      type: 'pie', radius: ['35%', '65%'], center: center || ['35%', '50%'],
      label: { show: false }, emphasis: { label: { show: true, fontSize: 14 } },
      data: Object.entries(dist).map(([name, value]) => ({ name, value })),
    }],
  }
}

function buildBar(dist: Record<string, number>, color: string) {
  const entries = Object.entries(dist)
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 20, left: 10, containLabel: true },
    xAxis: { type: 'category', data: entries.map(([k]) => k), axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    series: [{ type: 'bar', data: entries.map(([, v]) => v), itemStyle: { borderRadius: [4, 4, 0, 0], color } }],
  }
}

function buildCharts(d: LampCountResponse) {
  typeOption.value = buildPie(d.byType)
  contractorOption.value = buildPie(d.byContractor)
  lightSourceOption.value = buildBar(d.byLightSource, '#409eff')
  facilityOption.value = buildBar(d.byFacilityType, '#67c23a')
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getLampCount()
    data.value = res.body
    buildCharts(res.body)
  } catch {
    ElMessage.error(t('dashboard.loadFailed'))
  } finally {
    loading.value = false
  }
}

function drillDown() {
  router.push('/admin/asset/devices')
}

onMounted(() => fetchData())
</script>

<template>
  <div v-loading="loading" class="lamp-count-widget">
    <template v-if="data">
      <StatCard
        :title="t('dashboard.totalLamps')"
        :value="data.total.toLocaleString()"
        clickable
        @click="drillDown"
        class="total-card"
      />
      <el-tabs v-model="activeTab" class="chart-tabs">
        <el-tab-pane :label="t('dashboard.byType')" name="type">
          <VChart :option="typeOption" autoresize class="tab-chart" />
        </el-tab-pane>
        <el-tab-pane :label="t('dashboard.byContractor')" name="contractor">
          <VChart :option="contractorOption" autoresize class="tab-chart" />
        </el-tab-pane>
        <el-tab-pane :label="t('dashboard.byLightSource')" name="lightSource">
          <VChart :option="lightSourceOption" autoresize class="tab-chart" />
        </el-tab-pane>
        <el-tab-pane :label="t('dashboard.byFacilityType')" name="facility">
          <VChart :option="facilityOption" autoresize class="tab-chart" />
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<style scoped>
.lamp-count-widget { height: 100%; display: flex; flex-direction: column; }
.total-card { margin-bottom: 8px; }
.chart-tabs { flex: 1; display: flex; flex-direction: column; }
.chart-tabs :deep(.el-tabs__content) { flex: 1; }
.chart-tabs :deep(.el-tab-pane) { height: 100%; }
.tab-chart { width: 100%; height: 100%; min-height: 160px; }
</style>
