<script setup lang="ts">
import { TrendingUp, TrendingDown, Minus } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  title: string
  value: string | number
  unit?: string
  color?: string
  trend?: 'up' | 'down' | 'flat'
  trendValue?: string
  clickable?: boolean
}>(), {
  color: 'var(--el-color-primary)',
  clickable: false,
})

const emit = defineEmits<{ click: [] }>()
</script>

<template>
  <div
    class="stat-card"
    :class="{ 'is-clickable': clickable }"
    @click="clickable && emit('click')"
  >
    <div class="stat-title">{{ title }}</div>
    <div class="stat-value" :style="{ color }">
      {{ value }}<span v-if="unit" class="stat-unit">{{ unit }}</span>
    </div>
    <div v-if="trend" class="stat-trend" :class="trend">
      <TrendingUp v-if="trend === 'up'" :size="14" />
      <TrendingDown v-if="trend === 'down'" :size="14" />
      <Minus v-if="trend === 'flat'" :size="14" />
      <span v-if="trendValue">{{ trendValue }}</span>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  text-align: center;
}
.stat-card.is-clickable { cursor: pointer; transition: box-shadow 0.2s; }
.stat-card.is-clickable:hover { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); }
.stat-title { font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: 700; line-height: 1.2; }
.stat-unit { font-size: 14px; font-weight: 400; margin-left: 2px; }
.stat-trend { display: flex; align-items: center; justify-content: center; gap: 2px; font-size: 12px; margin-top: 4px; }
.stat-trend.up { color: #67c23a; }
.stat-trend.down { color: #f56c6c; }
.stat-trend.flat { color: #909399; }
</style>
