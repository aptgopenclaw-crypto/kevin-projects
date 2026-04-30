<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { WorkflowStepLogResponse } from '@/types/workflow'

defineProps<{ logs: WorkflowStepLogResponse[] }>()

const { t } = useI18n()

const actionLabel = (action: string) => {
  const map: Record<string, string> = {
    SUBMIT: t('workflow.actionSubmit'),
    APPROVE: t('workflow.actionApprove'),
    REJECT: t('workflow.actionReject'),
    RETURN: t('workflow.actionReturn'),
    DISPATCH: t('workflow.actionDispatch'),
    MERGE: t('workflow.actionMerge'),
    COMPLETE: t('workflow.actionComplete'),
    CANCEL: t('workflow.actionCancel'),
  }
  return map[action] ?? action
}

const actionColor = (action: string) => {
  const map: Record<string, string> = {
    APPROVE: '#5fc992',
    COMPLETE: '#5fc992',
    SUBMIT: '#55b3ff',
    DISPATCH: '#55b3ff',
    REJECT: '#FF6363',
    RETURN: '#ffbc33',
    CANCEL: '#FF6363',
    MERGE: '#a78bfa',
  }
  return map[action] ?? '#8b8fa3'
}
</script>

<template>
  <div class="stepper">
    <div v-if="logs.length === 0" class="stepper-empty">{{ t('workflow.logsEmpty') }}</div>
    <div v-for="(log, idx) in logs" :key="log.id" class="step-item">
      <div class="step-indicator">
        <div class="step-dot" :style="{ borderColor: actionColor(log.action) }">
          <div class="step-dot-inner" :style="{ background: actionColor(log.action) }" />
        </div>
        <div v-if="idx < logs.length - 1" class="step-line" />
      </div>
      <div class="step-body">
        <div class="step-header">
          <span class="step-action" :style="{ color: actionColor(log.action) }">{{ actionLabel(log.action) }}</span>
          <span class="step-code">{{ log.stepCode }}</span>
          <span v-if="log.isDelegated" class="step-delegated">{{ t('workflow.logDelegated') }}</span>
        </div>
        <div class="step-meta">
          <span>{{ log.actorName || log.actorId }}</span>
          <span class="step-time">{{ log.actedAt }}</span>
        </div>
        <div v-if="log.comment" class="step-comment">{{ log.comment }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stepper { padding: 4px 0; }
.stepper-empty { color: var(--text-muted); font-size: 14px; text-align: center; padding: 24px 0; }

.step-item { display: flex; gap: 14px; }

.step-indicator {
  display: flex; flex-direction: column; align-items: center;
  width: 20px; flex-shrink: 0;
}

.step-dot {
  width: 20px; height: 20px;
  border-radius: 50%; border: 2px solid;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.step-dot-inner { width: 8px; height: 8px; border-radius: 50%; }

.step-line {
  width: 2px; flex: 1;
  background: var(--bg-active);
  margin: 4px 0;
}

.step-body { flex: 1; padding-bottom: 20px; }

.step-header {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 4px;
}

.step-action { font-weight: 700; font-size: 13px; }
.step-code { color: var(--text-muted); font-size: 12px; font-family: 'JetBrains Mono', monospace; }
.step-delegated { color: #a78bfa; font-size: 11px; font-weight: 600; }

.step-meta { font-size: 12px; color: var(--text-secondary); display: flex; gap: 12px; }
.step-time { color: var(--text-muted); }

.step-comment {
  margin-top: 6px; padding: 8px 12px;
  background: var(--bg-base); border-radius: 6px;
  font-size: 13px; color: var(--text-primary);
  border-left: 3px solid var(--bg-active);
}
</style>
