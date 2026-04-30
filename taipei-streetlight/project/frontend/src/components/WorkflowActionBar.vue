<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import type { WorkflowTransitionRequest, WorkflowAction } from '@/types/workflow'

defineProps<{ instanceId: number }>()
const emit = defineEmits<{
  submit: [payload: WorkflowTransitionRequest]
}>()

const { t } = useI18n()

const visible = ref(false)
const formRef = ref<FormInstance>()
const form = ref<WorkflowTransitionRequest>({
  targetStep: '',
  action: '',
  comment: '',
})

const actions: WorkflowAction[] = [
  'SUBMIT', 'APPROVE', 'REJECT', 'RETURN', 'DISPATCH', 'MERGE', 'COMPLETE', 'CANCEL',
]

const actionLabel = (a: WorkflowAction) => {
  const map: Record<WorkflowAction, string> = {
    SUBMIT: t('workflow.actionSubmit'),
    APPROVE: t('workflow.actionApprove'),
    REJECT: t('workflow.actionReject'),
    RETURN: t('workflow.actionReturn'),
    DISPATCH: t('workflow.actionDispatch'),
    MERGE: t('workflow.actionMerge'),
    COMPLETE: t('workflow.actionComplete'),
    CANCEL: t('workflow.actionCancel'),
  }
  return map[a] ?? a
}

const rules = computed<FormRules>(() => ({
  targetStep: [{ required: true, message: t('workflow.errors.targetStepRequired'), trigger: 'blur' }],
  action: [{ required: true, message: t('workflow.errors.actionRequired'), trigger: 'change' }],
}))

function open() {
  form.value = { targetStep: '', action: '', comment: '' }
  visible.value = true
}

async function handleSubmit() {
  const f = formRef.value
  if (!f) return
  const valid = await f.validate().catch(() => false)
  if (!valid) return
  emit('submit', { ...form.value })
  visible.value = false
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" :title="t('workflow.actionDialogTitle')" width="480px" class="dark-dialog">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="t('workflow.targetStepLabel')" prop="targetStep">
        <el-input v-model="form.targetStep" :placeholder="t('workflow.targetStepPlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('workflow.actionLabel')" prop="action">
        <el-select v-model="form.action" style="width: 100%">
          <el-option v-for="a in actions" :key="a" :value="a" :label="actionLabel(a)" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('workflow.commentLabel')">
        <el-input v-model="form.comment" type="textarea" :rows="3" :placeholder="t('workflow.commentPlaceholder')" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button class="cancel-btn" @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button class="submit-btn" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.cancel-btn {
  background: transparent; color: var(--text-secondary);
  border: none; border-radius: 6px; padding: 8px 16px;
  font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 600;
}
.cancel-btn:hover { opacity: 0.6; color: var(--text-primary); }
.submit-btn {
  background: var(--btn-primary-bg); color: var(--btn-primary-text);
  border: none; border-radius: 86px; padding: 8px 24px;
  font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 600;
}
.submit-btn:hover { background: var(--btn-primary-hover); color: var(--btn-primary-text); }

:deep(.el-input__wrapper) { background-color: var(--bg-base); border: 1px solid var(--border-medium); border-radius: 8px; box-shadow: none; }
:deep(.el-input__wrapper:hover) { border-color: var(--border-strong); }
:deep(.el-input__wrapper.is-focus) { border-color: rgba(85, 179, 255, 0.5); box-shadow: 0 0 0 3px rgba(85, 179, 255, 0.15); }
:deep(.el-input__inner) { color: var(--text-primary); font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500; }
:deep(.el-input__inner::placeholder) { color: var(--text-muted); }
:deep(.el-form-item__label) { color: var(--text-label); font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500; }
:deep(.el-select .el-input__wrapper) { background-color: var(--bg-base); border: 1px solid var(--border-medium); border-radius: 8px; box-shadow: none; }
</style>
