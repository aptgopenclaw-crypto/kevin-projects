<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Upload, ImageIcon, MapPin } from 'lucide-vue-next'
import { uploadAttachment } from '@/api/repair'
import type { AttachmentPhase, AttachmentResponse } from '@/types/repair'

const props = defineProps<{
  ticketId: number
  phase?: AttachmentPhase
}>()

const emit = defineEmits<{
  uploaded: [attachment: AttachmentResponse]
}>()

const { t } = useI18n()
const uploading = ref(false)
const fileList = ref<File[]>([])
const gpsLat = ref<number | undefined>()
const gpsLng = ref<number | undefined>()
const description = ref('')

// ──────────── GPS ────────────
const gpsLoading = ref(false)
const gpsLabel = computed(() => {
  if (gpsLat.value != null && gpsLng.value != null) {
    return `${gpsLat.value.toFixed(5)}, ${gpsLng.value.toFixed(5)}`
  }
  return t('repair.attachment.noGps')
})

function readGps() {
  if (!navigator.geolocation) {
    ElMessage.warning(t('repair.attachment.gpsNotSupported'))
    return
  }
  gpsLoading.value = true
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      gpsLat.value = pos.coords.latitude
      gpsLng.value = pos.coords.longitude
      gpsLoading.value = false
    },
    () => {
      ElMessage.warning(t('repair.attachment.gpsFailed'))
      gpsLoading.value = false
    },
    { enableHighAccuracy: true, timeout: 10000 },
  )
}

// ──────────── Upload ────────────
function handleFileChange(file: { raw: File }) {
  fileList.value.push(file.raw)
}

function handleRemove(index: number) {
  fileList.value.splice(index, 1)
}

async function handleUpload() {
  if (fileList.value.length === 0) {
    ElMessage.warning(t('repair.attachment.selectFile'))
    return
  }

  uploading.value = true
  try {
    for (const file of fileList.value) {
      const res = await uploadAttachment(props.ticketId, file, {
        phase: props.phase,
        description: description.value || undefined,
        gpsLat: gpsLat.value,
        gpsLng: gpsLng.value,
      })
      emit('uploaded', res.body)
    }
    ElMessage.success(t('repair.attachment.uploadSuccess'))
    fileList.value = []
    description.value = ''
  } catch {
    ElMessage.error(t('repair.attachment.uploadFailed'))
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="attachment-uploader">
    <!-- File select area -->
    <el-upload
      :auto-upload="false"
      :show-file-list="false"
      multiple
      accept="image/*,video/*,.pdf"
      @change="handleFileChange"
    >
      <div class="upload-trigger">
        <Upload :size="24" />
        <span>{{ t('repair.attachment.dropOrClick') }}</span>
      </div>
    </el-upload>

    <!-- File preview list -->
    <div v-if="fileList.length > 0" class="file-preview-list">
      <div v-for="(file, idx) in fileList" :key="idx" class="file-preview-item">
        <ImageIcon :size="14" />
        <span class="file-name">{{ file.name }}</span>
        <el-button text size="small" type="danger" @click="handleRemove(idx)">×</el-button>
      </div>
    </div>

    <!-- Description -->
    <el-input
      v-model="description"
      :placeholder="t('repair.attachment.descPlaceholder')"
      style="margin-top: 8px"
    />

    <!-- GPS -->
    <div class="gps-row">
      <el-button size="small" @click="readGps" :loading="gpsLoading">
        <MapPin :size="14" style="margin-right: 4px" />
        {{ t('repair.attachment.readGps') }}
      </el-button>
      <span class="gps-label">{{ gpsLabel }}</span>
    </div>

    <!-- Upload button -->
    <el-button
      class="submit-btn"
      style="margin-top: 12px; width: 100%"
      :loading="uploading"
      @click="handleUpload"
    >
      {{ t('repair.attachment.uploadBtn') }}
    </el-button>
  </div>
</template>

<style scoped>
.attachment-uploader {
  padding: 12px;
}
.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px;
  border: 2px dashed var(--bg-active);
  border-radius: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: border-color 0.2s;
}
.upload-trigger:hover {
  border-color: var(--accent);
}
.file-preview-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.file-preview-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: var(--bg-base);
  border-radius: 6px;
  font-size: 13px;
}
.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gps-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}
.gps-label {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
