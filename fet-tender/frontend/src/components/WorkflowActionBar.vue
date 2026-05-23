<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  instanceId: number
}>()

const emit = defineEmits<{
  submit: [payload: { action: string; comment: string }]
}>()

const visible = ref(false)
const action = ref('')
const comment = ref('')

function open() {
  visible.value = true
  action.value = ''
  comment.value = ''
}

function handleSubmit() {
  emit('submit', { action: action.value, comment: comment.value })
  visible.value = false
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="Workflow Action" width="500px">
    <el-form label-position="top">
      <el-form-item label="Action">
        <el-select v-model="action" placeholder="Select action">
          <el-option label="Approve" value="APPROVE" />
          <el-option label="Reject" value="REJECT" />
          <el-option label="Return" value="RETURN" />
        </el-select>
      </el-form-item>
      <el-form-item label="Comment">
        <el-input v-model="comment" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">Cancel</el-button>
      <el-button type="primary" :disabled="!action" @click="handleSubmit">Submit</el-button>
    </template>
  </el-dialog>
</template>
