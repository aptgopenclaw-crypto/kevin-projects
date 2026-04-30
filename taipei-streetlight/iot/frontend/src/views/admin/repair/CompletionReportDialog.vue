<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { completeRepairTicket } from '@/api/repair'
import type { CompletionReportRequest } from '@/types/repair'

const emit = defineEmits<{ completed: [] }>()
const { t } = useI18n()

const visible = ref(false)
const loading = ref(false)
const ticketId = ref(0)
const formRef = ref<FormInstance>()
const form = ref<CompletionReportRequest>({ repairDescription: '' })

const rules = computed<FormRules>(() => ({
  repairDescription: [{ required: true, message: t('repair.completion.descRequired'), trigger: 'blur' }],
}))

function open(id: number) {
  ticketId.value = id
  form.value = { repairDescription: '', faultCause: '' }
  visible.value = true
}

async function handleSubmit() {
  const f = formRef.value
  if (!f) return
  const valid = await f.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await completeRepairTicket(ticketId.value, form.value)
    ElMessage.success(t('repair.completion.success'))
    visible.value = false
    emit('completed')
  } catch {
    ElMessage.error(t('repair.completion.failed'))
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" :title="t('repair.completion.title')" width="540px" class="dark-dialog">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="t('repair.completion.repairDesc')" prop="repairDescription">
        <el-input v-model="form.repairDescription" type="textarea" :rows="4" />
      </el-form-item>
      <el-form-item :label="t('repair.completion.faultCause')">
        <el-input v-model="form.faultCause" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button class="cancel-btn" @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button class="submit-btn" @click="handleSubmit" :loading="loading">{{ t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>
