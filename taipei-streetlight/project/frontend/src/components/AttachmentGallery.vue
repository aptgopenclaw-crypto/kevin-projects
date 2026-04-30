<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Download, MapPin, Image as ImageIcon } from 'lucide-vue-next'
import { downloadAttachment } from '@/api/repair'
import type { AttachmentResponse, AttachmentPhase } from '@/types/repair'

const props = defineProps<{
  attachments: AttachmentResponse[]
}>()

const { t } = useI18n()

const phaseLabel = (phase: AttachmentPhase | null) => {
  const map: Record<string, string> = {
    BEFORE: t('repair.attachment.phaseBefore'),
    DURING: t('repair.attachment.phaseDuring'),
    AFTER: t('repair.attachment.phaseAfter'),
    REPORT: t('repair.attachment.phaseReport'),
  }
  return phase ? map[phase] ?? phase : ''
}

const phaseColor = (phase: AttachmentPhase | null) => {
  const map: Record<string, string> = {
    BEFORE: '#e6a23c',
    DURING: '#409eff',
    AFTER: '#5fc992',
    REPORT: '#909399',
  }
  return phase ? map[phase] ?? '#909399' : '#909399'
}

const grouped = computed(() => {
  const groups: Record<string, AttachmentResponse[]> = {}
  for (const att of props.attachments) {
    const key = att.phase ?? 'OTHER'
    if (!groups[key]) groups[key] = []
    groups[key].push(att)
  }
  return groups
})

const isImage = (att: AttachmentResponse) =>
  att.fileType === 'PHOTO' || att.fileName?.match(/\.(jpg|jpeg|png|gif|webp)$/i)

async function handleDownload(att: AttachmentResponse) {
  try {
    const res = await downloadAttachment(att.id)
    const blob = res as unknown as Blob
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = att.fileName ?? `attachment-${att.id}`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    // silently fail
  }
}
</script>

<template>
  <div class="attachment-gallery">
    <div v-if="attachments.length === 0" class="gallery-empty">
      {{ t('repair.attachment.noAttachments') }}
    </div>

    <div v-for="(items, phase) in grouped" :key="phase" class="phase-group">
      <div class="phase-header">
        <span class="phase-badge" :style="{ background: phaseColor(phase as AttachmentPhase) }">
          {{ phaseLabel(phase as AttachmentPhase) || t('repair.attachment.phaseOther') }}
        </span>
        <span class="phase-count">{{ items.length }}</span>
      </div>

      <div class="gallery-grid">
        <div v-for="att in items" :key="att.id" class="gallery-item">
          <div class="gallery-thumb">
            <img
              v-if="isImage(att)"
              :src="att.fileUrl"
              :alt="att.description || att.fileName || ''"
              class="thumb-img"
            />
            <div v-else class="thumb-placeholder">
              <ImageIcon :size="24" />
              <span>{{ att.fileType }}</span>
            </div>
          </div>
          <div class="gallery-info">
            <span class="gallery-name">{{ att.fileName || `#${att.id}` }}</span>
            <div class="gallery-meta">
              <span v-if="att.gpsLat != null && att.gpsLng != null" class="gps-tag">
                <MapPin :size="12" /> {{ att.gpsLat.toFixed(4) }}, {{ att.gpsLng.toFixed(4) }}
              </span>
              <el-button text size="small" @click="handleDownload(att)">
                <Download :size="14" />
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.attachment-gallery {
  padding: 8px 0;
}
.gallery-empty {
  text-align: center;
  color: var(--text-secondary);
  padding: 32px 0;
}
.phase-group {
  margin-bottom: 16px;
}
.phase-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.phase-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}
.phase-count {
  font-size: 12px;
  color: var(--text-secondary);
}
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}
.gallery-item {
  border: 1px solid var(--bg-active);
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-surface);
}
.gallery-thumb {
  width: 100%;
  aspect-ratio: 4/3;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-base);
}
.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.thumb-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}
.gallery-info {
  padding: 8px;
}
.gallery-name {
  font-size: 12px;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gallery-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}
.gps-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: var(--text-secondary);
}
</style>
