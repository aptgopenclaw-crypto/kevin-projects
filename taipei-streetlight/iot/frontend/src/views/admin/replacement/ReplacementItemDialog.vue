<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ReplacementItemRequest } from '@/types/replacement'

const { t } = useI18n()

defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'confirm', data: ReplacementItemRequest): void
}>()

const step = ref(0)
const form = ref<ReplacementItemRequest>({ parentDeviceId: 0, oldDeviceId: 0 })

const steps = computed(() => [
  t('replacement.stepSelectPole'),
  t('replacement.stepSelectDevice'),
  t('replacement.stepSelectMaterial'),
  t('replacement.stepConfirm'),
])

const canNext = computed(() => {
  if (step.value === 0) return form.value.parentDeviceId > 0
  if (step.value === 1) return form.value.oldDeviceId > 0
  return true
})

const handleNext = () => {
  if (step.value < 3) step.value++
}
const handlePrev = () => {
  if (step.value > 0) step.value--
}

const handleConfirm = () => {
  emit('confirm', { ...form.value })
  handleClose()
}

const handleClose = () => {
  step.value = 0
  form.value = { parentDeviceId: 0, oldDeviceId: 0 }
  emit('update:visible', false)
}

const skipMaterial = () => {
  form.value.approvedMaterialId = undefined
  form.value.materialSpecId = undefined
  step.value = 3
}
</script>

<template>
  <el-dialog :model-value="visible" :title="t('replacement.dialogAddItem')" width="580px" class="dark-dialog" @close="handleClose">
    <!-- Steps indicator -->
    <el-steps :active="step" finish-status="success" simple style="margin-bottom: 24px">
      <el-step v-for="(s, i) in steps" :key="i" :title="s" />
    </el-steps>

    <!-- Step 0: Select Pole -->
    <div v-if="step === 0" class="step-content">
      <p class="step-hint">{{ t('replacement.stepSelectPole') }}</p>
      <el-form label-position="top">
        <el-form-item :label="t('replacement.parentDevice') + ' ID'">
          <el-input-number v-model="form.parentDeviceId" :min="1" style="width: 100%" :placeholder="t('replacement.searchDevice')" />
        </el-form-item>
      </el-form>
      <div v-if="form.parentDeviceId > 0" class="selected-info">
        {{ t('replacement.selectedPole') }}: #{{ form.parentDeviceId }}
      </div>
    </div>

    <!-- Step 1: Select Old Device -->
    <div v-if="step === 1" class="step-content">
      <p class="step-hint">{{ t('replacement.stepSelectDevice') }}</p>
      <el-form label-position="top">
        <el-form-item :label="t('replacement.oldDevice') + ' ID'">
          <el-input-number v-model="form.oldDeviceId" :min="1" style="width: 100%" :placeholder="t('replacement.searchDevice')" />
        </el-form-item>
        <el-form-item :label="t('replacement.afterSpec')">
          <el-input v-model="form.afterDeviceType" :placeholder="t('replacement.specDeviceType')" />
        </el-form-item>
      </el-form>
      <div v-if="form.oldDeviceId > 0" class="selected-info">
        {{ t('replacement.selectedDevice') }}: #{{ form.oldDeviceId }}
      </div>
    </div>

    <!-- Step 2: Select Material -->
    <div v-if="step === 2" class="step-content">
      <p class="step-hint">{{ t('replacement.stepSelectMaterial') }}</p>
      <el-form label-position="top">
        <el-form-item :label="t('replacement.approvedMaterial') + ' ID'">
          <el-input-number v-model="form.approvedMaterialId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('replacement.materialSpec') + ' ID'">
          <el-input-number v-model="form.materialSpecId" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <el-button text @click="skipMaterial" style="color: var(--text-secondary)">{{ t('replacement.skipMaterial') }}</el-button>
      <div v-if="form.approvedMaterialId" class="selected-info">
        {{ t('replacement.selectedMaterial') }}: #{{ form.approvedMaterialId }}
      </div>
    </div>

    <!-- Step 3: Confirm -->
    <div v-if="step === 3" class="step-content">
      <p class="step-hint">{{ t('replacement.stepConfirm') }}</p>
      <div class="confirm-grid">
        <div class="confirm-row">
          <span class="confirm-label">{{ t('replacement.parentDevice') }}</span>
          <span class="confirm-value">#{{ form.parentDeviceId }}</span>
        </div>
        <div class="confirm-row">
          <span class="confirm-label">{{ t('replacement.oldDevice') }}</span>
          <span class="confirm-value">#{{ form.oldDeviceId }}</span>
        </div>
        <div class="confirm-row">
          <span class="confirm-label">{{ t('replacement.afterSpec') }}</span>
          <span class="confirm-value">{{ form.afterDeviceType || '-' }}</span>
        </div>
        <div class="confirm-row">
          <span class="confirm-label">{{ t('replacement.approvedMaterial') }}</span>
          <span class="confirm-value">{{ form.approvedMaterialId ? '#' + form.approvedMaterialId : '-' }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button class="cancel-btn" @click="handleClose">{{ t('common.cancel') }}</el-button>
        <el-button v-if="step > 0" @click="handlePrev">{{ t('common.back') }}</el-button>
        <el-button v-if="step < 3" class="submit-btn" :disabled="!canNext" @click="handleNext">{{ t('common.next') }}</el-button>
        <el-button v-if="step === 3" class="submit-btn" @click="handleConfirm">{{ t('common.confirm') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.step-content { min-height: 180px; }
.step-hint { font-size: 13px; color: var(--text-secondary); margin-bottom: 16px; }
.selected-info {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(64, 158, 255, 0.08);
  color: #409eff;
  font-size: 13px;
  font-weight: 500;
}
.confirm-grid { display: flex; flex-direction: column; gap: 12px; }
.confirm-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--bg-active); }
.confirm-label { color: var(--text-secondary); font-size: 13px; }
.confirm-value { color: var(--text-heading); font-weight: 600; font-size: 13px; }
.dialog-footer { display: flex; gap: 8px; justify-content: flex-end; }
</style>
