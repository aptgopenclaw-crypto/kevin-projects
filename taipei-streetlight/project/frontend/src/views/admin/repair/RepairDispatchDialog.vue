<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { dispatchRepairTicket } from '@/api/repair'
import type { DispatchRequest, DispatchResponse } from '@/types/repair'

const emit = defineEmits<{ dispatched: [dispatch: DispatchResponse] }>()
const { t } = useI18n()

const visible = ref(false)
const loading = ref(false)
const ticketId = ref(0)
const formRef = ref<FormInstance>()
const form = ref<DispatchRequest>({ contractId: 0 })

const rules = computed<FormRules>(() => ({
  contractId: [{ required: true, message: t('repair.dispatch.contractRequired'), trigger: 'change' }],
}))

function open(id: number) {
  ticketId.value = id
  form.value = { contractId: 0 }
  visible.value = true
}

async function handleSubmit() {
  const f = formRef.value
  if (!f) return
  const valid = await f.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await dispatchRepairTicket(ticketId.value, form.value)
    ElMessage.success(t('repair.dispatch.success'))
    visible.value = false
    emit('dispatched', res.body)
  } catch {
    ElMessage.error(t('repair.dispatch.failed'))
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" :title="t('repair.dispatch.title')" width="500px" class="dark-dialog">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="t('repair.dispatch.assignedOrg')">
        <el-input v-model="form.assignedOrg" :placeholder="t('repair.dispatch.assignedOrgPlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('repair.dispatch.contractId')" prop="contractId">
        <el-input-number v-model="form.contractId" :min="1" style="width: 100%" />
      </el-form-item>
      <el-form-item :label="t('repair.dispatch.dueDate')">
        <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item :label="t('repair.dispatch.note')">
        <el-input v-model="form.note" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button class="cancel-btn" @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button class="submit-btn" @click="handleSubmit" :loading="loading">{{ t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>
