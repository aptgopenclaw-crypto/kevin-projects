<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Lock } from 'lucide-vue-next'

const { t } = useI18n()
</script>

<template>
  <div class="unavailable-widget">
    <Lock :size="40" class="lock-icon" />
    <p class="title">{{ t('dashboard.widgets.electricityCost') }}</p>
    <p class="message">{{ t('dashboard.moduleNotEnabled') }}</p>
    <p class="hint">{{ t('dashboard.moduleNotEnabledHint') }}</p>
  </div>
</template>

<style scoped>
.unavailable-widget {
  height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; text-align: center;
  color: var(--el-text-color-secondary);
}
.lock-icon { opacity: 0.4; margin-bottom: 12px; }
.title { font-size: 16px; font-weight: 600; margin: 0 0 4px; }
.message { font-size: 14px; margin: 0; }
.hint { font-size: 12px; opacity: 0.7; margin-top: 4px; }
</style>
