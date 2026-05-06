<script setup lang="ts">
import { ref } from 'vue'
import VChart from 'vue-echarts'
import type { EChartsOption } from 'echarts'
import { useEchartsTheme } from '@/views/admin/dashboard/composables/useDashboardTheme'

const props = withDefaults(defineProps<{
  option: EChartsOption
  loading?: boolean
  height?: string
}>(), {
  loading: false,
  height: '100%',
})

const echartsTheme = useEchartsTheme()
const chartRef = ref<InstanceType<typeof VChart> | null>(null)

defineExpose({ chartRef })
</script>

<template>
  <div class="chart-wrapper" :style="{ height }">
    <VChart
      ref="chartRef"
      :option="option"
      :theme="echartsTheme"
      :loading="loading"
      autoresize
      class="chart-inner"
    />
  </div>
</template>

<style scoped>
.chart-wrapper { width: 100%; position: relative; }
.chart-inner { width: 100%; height: 100%; min-height: 120px; }
</style>
