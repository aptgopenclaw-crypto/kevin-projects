<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Maximize2, Minimize2, RefreshCw, X } from 'lucide-vue-next'
import { getWidgetMeta } from '@/views/admin/dashboard/widgetRegistry'
import type { WidgetType } from '@/types/dashboard'

const props = defineProps<{
  widgetType: WidgetType
  editing?: boolean
}>()

const emit = defineEmits<{
  remove: []
}>()

const { t } = useI18n()

const meta = computed(() => getWidgetMeta(props.widgetType))
const fullscreen = ref(false)
const refreshKey = ref(0)

function toggleFullscreen() {
  fullscreen.value = !fullscreen.value
}

function refresh() {
  refreshKey.value++
}
</script>

<template>
  <div class="widget-container" :class="{ 'widget-fullscreen': fullscreen }">
    <div class="widget-header">
      <span class="widget-title">{{ t(meta.labelKey) }}</span>
      <div class="widget-actions">
        <el-tooltip :content="t('dashboard.refresh')" placement="top">
          <el-button link size="small" @click="refresh">
            <RefreshCw :size="14" />
          </el-button>
        </el-tooltip>
        <el-tooltip :content="fullscreen ? t('dashboard.exitFullscreen') : t('dashboard.fullscreen')" placement="top">
          <el-button link size="small" @click="toggleFullscreen">
            <component :is="fullscreen ? Minimize2 : Maximize2" :size="14" />
          </el-button>
        </el-tooltip>
        <el-tooltip v-if="editing" :content="t('dashboard.removeWidget')" placement="top">
          <el-button link size="small" type="danger" @click="emit('remove')">
            <X :size="14" />
          </el-button>
        </el-tooltip>
      </div>
    </div>
    <div class="widget-body">
      <component :is="meta.component" :key="refreshKey" />
    </div>
  </div>
</template>

<style scoped>
.widget-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}

.widget-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 2000;
  border-radius: 0;
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
  flex-shrink: 0;
}

.widget-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.widget-actions {
  display: flex;
  gap: 2px;
}

.widget-body {
  flex: 1;
  overflow: auto;
  padding: 12px;
}
</style>
