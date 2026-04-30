<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import TimeRangeFilter from '@/views/admin/dashboard/components/TimeRangeFilter.vue'
import { getFaultCategory } from '@/api/dashboard'
import type { FaultCategoryResponse, CategoryItem } from '@/types/dashboard'
import dayjs from 'dayjs'

const { t } = useI18n()
const router = useRouter()
const loading = ref(false)
const categories = ref<CategoryItem[]>([])
const dateRange = ref<[string, string]>([
  dayjs().subtract(3, 'month').startOf('month').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD'),
])

const pieOption = ref({})
const barOption = ref({})
const activeTab = ref<'pie' | 'bar' | 'table'>('pie')

function buildCharts(cats: CategoryItem[]) {
  pieOption.value = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center' },
    series: [{
      type: 'pie', radius: ['35%', '65%'], center: ['35%', '50%'],
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14 } },
      data: cats.map(c => ({ name: c.category, value: c.count })),
    }],
  }
  barOption.value = {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 20, left: 10, containLabel: true },
    xAxis: { type: 'category', data: cats.map(c => c.category), axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    series: [{ type: 'bar', data: cats.map(c => c.count), itemStyle: { borderRadius: [4, 4, 0, 0], color: '#f56c6c' } }],
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getFaultCategory({ startDate: dateRange.value[0], endDate: dateRange.value[1] })
    categories.value = res.body.categories
    buildCharts(res.body.categories)
  } catch {
    ElMessage.error(t('dashboard.loadFailed'))
  } finally {
    loading.value = false
  }
}

watch(dateRange, () => fetchData())

function drillDown() {
  router.push('/admin/repair/list')
}

onMounted(() => fetchData())
</script>

<template>
  <div v-loading="loading" class="fault-widget">
    <TimeRangeFilter v-model="dateRange" />
    <el-tabs v-model="activeTab" class="chart-tabs">
      <el-tab-pane :label="t('dashboard.pieChart')" name="pie">
        <VChart :option="pieOption" autoresize class="tab-chart" />
      </el-tab-pane>
      <el-tab-pane :label="t('dashboard.barChart')" name="bar">
        <VChart :option="barOption" autoresize class="tab-chart" />
      </el-tab-pane>
      <el-tab-pane :label="t('dashboard.detailTable')" name="table">
        <el-table :data="categories" size="small" stripe max-height="250">
          <el-table-column prop="category" :label="t('dashboard.faultCategory')" />
          <el-table-column prop="count" :label="t('dashboard.count')" width="80" align="right" />
          <el-table-column :label="t('dashboard.percentage')" width="100" align="right">
            <template #default="{ row }">{{ row.percentage }}%</template>
          </el-table-column>
        </el-table>
        <div class="drill-link">
          <el-button link type="primary" size="small" @click="drillDown">
            {{ t('dashboard.viewAllFaults') }}
          </el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.fault-widget { height: 100%; display: flex; flex-direction: column; }
.chart-tabs { flex: 1; display: flex; flex-direction: column; margin-top: 4px; }
.chart-tabs :deep(.el-tabs__content) { flex: 1; }
.chart-tabs :deep(.el-tab-pane) { height: 100%; }
.tab-chart { width: 100%; height: 100%; min-height: 180px; }
.drill-link { text-align: right; margin-top: 8px; }
</style>
