<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps<{
  oldDeviceCode?: string | null
  newDeviceCode?: string | null
  beforeDeviceType?: string | null
  afterDeviceType?: string | null
  beforeSpec?: Record<string, unknown> | null
  afterSpec?: Record<string, unknown> | null
  parentDeviceCode?: string | null
  materialInfo?: string | null
}>()

const specKeys = computed(() => {
  const keys = new Set<string>()
  if (props.beforeSpec) Object.keys(props.beforeSpec).forEach(k => keys.add(k))
  if (props.afterSpec) Object.keys(props.afterSpec).forEach(k => keys.add(k))
  return [...keys]
})

const isChanged = (key: string) => {
  const before = props.beforeSpec?.[key]
  const after = props.afterSpec?.[key]
  return JSON.stringify(before) !== JSON.stringify(after)
}

const formatVal = (val: unknown): string => {
  if (val === null || val === undefined) return '-'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}
</script>

<template>
  <div class="spec-comparison">
    <div class="spec-columns">
      <!-- Before Column -->
      <div class="spec-col before">
        <div class="spec-col-header">
          <span class="spec-arrow">◀</span> {{ t('replacement.specBefore') }}
        </div>
        <div class="spec-row">
          <span class="spec-label">{{ t('replacement.specDeviceCode') }}</span>
          <span class="spec-value">{{ oldDeviceCode ?? '-' }}</span>
        </div>
        <div class="spec-row">
          <span class="spec-label">{{ t('replacement.specDeviceType') }}</span>
          <span class="spec-value">{{ beforeDeviceType ?? '-' }}</span>
        </div>
        <div v-for="key in specKeys" :key="'b-' + key" class="spec-row">
          <span class="spec-label">{{ key }}</span>
          <span class="spec-value">{{ formatVal(beforeSpec?.[key]) }}</span>
        </div>
      </div>

      <!-- After Column -->
      <div class="spec-col after">
        <div class="spec-col-header">
          {{ t('replacement.specAfter') }} <span class="spec-arrow">▶</span>
        </div>
        <div class="spec-row">
          <span class="spec-label">{{ t('replacement.specDeviceCode') }}</span>
          <span class="spec-value">
            {{ newDeviceCode ?? '-' }}
            <span v-if="newDeviceCode" class="badge-new">✦{{ t('replacement.specNew') }}</span>
          </span>
        </div>
        <div class="spec-row">
          <span class="spec-label">{{ t('replacement.specDeviceType') }}</span>
          <span class="spec-value">
            {{ afterDeviceType ?? '-' }}
            <span v-if="afterDeviceType && afterDeviceType !== beforeDeviceType" class="badge-changed">✦{{ t('replacement.specChanged') }}</span>
          </span>
        </div>
        <div v-for="key in specKeys" :key="'a-' + key" class="spec-row" :class="{ changed: isChanged(key) }">
          <span class="spec-label">{{ key }}</span>
          <span class="spec-value">
            {{ formatVal(afterSpec?.[key]) }}
            <span v-if="isChanged(key)" class="badge-changed">✦{{ t('replacement.specChanged') }}</span>
          </span>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div v-if="materialInfo || parentDeviceCode" class="spec-footer">
      <div v-if="materialInfo" class="spec-footer-row">
        <span class="spec-label">{{ t('replacement.specMaterial') }}：</span>
        <span class="spec-value">{{ materialInfo }}</span>
      </div>
      <div v-if="parentDeviceCode" class="spec-footer-row">
        <span class="spec-label">{{ t('replacement.specMountLocation') }}：</span>
        <span class="spec-value">{{ parentDeviceCode }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.spec-comparison {
  border: 1px solid var(--bg-active);
  border-radius: 12px;
  overflow: hidden;
  background: var(--bg-surface);
}
.spec-columns { display: grid; grid-template-columns: 1fr 1fr; }
.spec-col { padding: 0; }
.spec-col.before { border-right: 1px solid var(--bg-active); }
.spec-col-header {
  padding: 12px 16px;
  font-weight: 700;
  font-size: 14px;
  background: var(--bg-base);
  border-bottom: 1px solid var(--bg-active);
  color: var(--text-heading);
}
.spec-arrow { opacity: 0.5; }
.spec-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid var(--bg-active);
  font-size: 13px;
}
.spec-row:last-child { border-bottom: none; }
.spec-row.changed { background: rgba(64, 158, 255, 0.06); }
.spec-label { color: var(--text-secondary); }
.spec-value { color: var(--text-heading); font-weight: 500; }
.badge-new {
  display: inline-block;
  margin-left: 6px;
  font-size: 11px;
  color: #5fc992;
  font-weight: 600;
}
.badge-changed {
  display: inline-block;
  margin-left: 6px;
  font-size: 11px;
  color: #409eff;
  font-weight: 600;
}
.spec-footer {
  border-top: 1px solid var(--bg-active);
  padding: 12px 16px;
  background: var(--bg-base);
}
.spec-footer-row {
  display: flex;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 4px;
}
.spec-footer-row:last-child { margin-bottom: 0; }
</style>
