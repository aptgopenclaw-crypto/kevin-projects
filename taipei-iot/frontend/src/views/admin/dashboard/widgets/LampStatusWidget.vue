<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Wifi, WifiOff } from 'lucide-vue-next'
import VChart from 'vue-echarts'
import { getLampStatus } from '@/api/dashboard'
import type { LampStatusResponse } from '@/types/dashboard'
import { formatDateTime } from '@/utils/datetime'
import { useDashboardWsInject } from '@/views/admin/dashboard/composables/useDashboardWsInject'

const { t } = useI18n()
const loading = ref(false)
const data = ref<LampStatusResponse | null>(null)
const pulse = ref(false)

// ── WebSocket realtime ──
const ws = useDashboardWsInject()
let unsubscribe: (() => void) | undefined
if (ws) {
  unsubscribe = ws.onWidgetUpdate('lamp-status', (wsData) => {
    data.value = wsData as LampStatusResponse
    pulse.value = true
    setTimeout(() => { pulse.value = false }, 1500)
  })
}
onBeforeUnmount(() => { unsubscribe?.() })

const onlinePercent = computed(() => data.value ? Math.round(data.value.onlineRate * 100) / 100 : 0)

const gaugeOption = computed(() => {
  if (!data.value) return {}
  return {
    series: [{
      type: 'gauge',
      radius: '90%',
      startAngle: 210,
      endAngle: -30,
      min: 0,
      max: 100,
      detail: { formatter: '{value}%', fontSize: 18, offsetCenter: [0, '60%'] },
      title: { offsetCenter: [0, '85%'], fontSize: 13 },
      data: [{ value: onlinePercent.value, name: t('dashboard.onlineRate') }],
      axisLine: { lineStyle: { width: 12, color: [[0.6, '#f56c6c'], [0.8, '#e6a23c'], [1, '#67c23a']] } },
      pointer: { length: '55%', width: 5 },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
    }],
  }
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getLampStatus()
    data.value = res.body
  } catch {
    ElMessage.error(t('dashboard.loadFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div v-loading="loading" class="lamp-status-widget" :class="{ 'widget-pulse': pulse }">
    <template v-if="data">
      <div class="status-row">
        <div class="status-card online">
          <Wifi :size="20" />
          <div class="status-value">{{ data.online.toLocaleString() }}</div>
          <div class="status-label">{{ t('dashboard.online') }}</div>
        </div>
        <div class="status-card offline">
          <WifiOff :size="20" />
          <div class="status-value">{{ data.offline.toLocaleString() }}</div>
          <div class="status-label">{{ t('dashboard.offline') }}</div>
        </div>
      </div>
      <VChart :option="gaugeOption" autoresize class="gauge-chart" />
      <div class="updated-at">{{ t('dashboard.updatedAt') }}: {{ formatDateTime(data.updatedAt) }}</div>
    </template>
  </div>
</template>

<style scoped>
.lamp-status-widget { height: 100%; display: flex; flex-direction: column; }
.status-row { display: flex; gap: 12px; margin-bottom: 8px; }
.status-card { flex: 1; text-align: center; padding: 10px; border-radius: 8px; }
.status-card.online { background: #f0f9eb; color: #67c23a; }
.status-card.offline { background: #fef0f0; color: #f56c6c; }
.status-value { font-size: 22px; font-weight: 700; margin-top: 4px; }
.status-label { font-size: 12px; opacity: 0.8; }
.gauge-chart { flex: 1; min-height: 120px; }
.updated-at { font-size: 12px; color: var(--el-text-color-secondary); text-align: right; flex-shrink: 0; }
.widget-pulse { animation: widget-pulse-anim 1.5s ease-out; }
@keyframes widget-pulse-anim {
  0% { box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.5); }
  100% { box-shadow: 0 0 0 0 transparent; }
}
</style>
