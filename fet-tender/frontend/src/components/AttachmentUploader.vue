<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

const props = defineProps<{
  ticketId: number
}>()

const emit = defineEmits<{
  uploaded: []
}>()

const uploading = ref(false)

async function handleUpload(options: { file: File }) {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    formData.append('ticketId', String(props.ticketId))
    // TODO: integrate with actual upload API
    ElMessage.success('File uploaded')
    emit('uploaded')
  } catch {
    ElMessage.error('Upload failed')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <el-upload
    :http-request="handleUpload"
    :show-file-list="false"
    :disabled="uploading"
    drag
  >
    <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
    <div class="el-upload__text">Drop file here or <em>click to upload</em></div>
  </el-upload>
</template>
