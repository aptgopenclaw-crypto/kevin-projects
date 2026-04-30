<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'

type RangeKey = 'today' | 'week' | 'month' | 'quarter' | 'year' | 'custom'

const props = withDefaults(defineProps<{
  modelValue?: [string, string]
}>(), {})

const emit = defineEmits<{
  'update:modelValue': [value: [string, string]]
}>()

const { t } = useI18n()

const presets: { key: RangeKey; label: string }[] = [
  { key: 'today', label: t('dashboard.rangeToday') },
  { key: 'week', label: t('dashboard.rangeWeek') },
  { key: 'month', label: t('dashboard.rangeMonth') },
  { key: 'quarter', label: t('dashboard.rangeQuarter') },
  { key: 'year', label: t('dashboard.rangeYear') },
]

function applyPreset(key: RangeKey) {
  const end = dayjs().format('YYYY-MM-DD')
  let start: string
  switch (key) {
    case 'today': start = end; break
    case 'week': start = dayjs().startOf('week').format('YYYY-MM-DD'); break
    case 'month': start = dayjs().startOf('month').format('YYYY-MM-DD'); break
    case 'quarter': start = dayjs().startOf('quarter').format('YYYY-MM-DD'); break
    case 'year': start = dayjs().startOf('year').format('YYYY-MM-DD'); break
    default: return
  }
  emit('update:modelValue', [start, end])
}

function onCustomChange(val: [string, string] | null) {
  if (val) emit('update:modelValue', val)
}
</script>

<template>
  <div class="time-range-filter">
    <el-button-group size="small">
      <el-button
        v-for="p in presets"
        :key="p.key"
        @click="applyPreset(p.key)"
      >
        {{ p.label }}
      </el-button>
    </el-button-group>
    <el-date-picker
      type="daterange"
      size="small"
      :start-placeholder="t('dashboard.startDate')"
      :end-placeholder="t('dashboard.endDate')"
      value-format="YYYY-MM-DD"
      @change="onCustomChange"
      style="width: 220px; margin-left: 8px;"
    />
  </div>
</template>

<style scoped>
.time-range-filter {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
