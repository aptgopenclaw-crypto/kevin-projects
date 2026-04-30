<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAnnouncementStore } from '@/stores/announcementStore'
import { listAnnouncements, markAsRead } from '@/api/announcement'
import type { AnnouncementResponse } from '@/types/announcement'

const { t } = useI18n()
const announcementStore = useAnnouncementStore()

const loading = ref(false)
const items = ref<AnnouncementResponse[]>([])
const pagination = reactive({ page: 0, size: 10, total: 0 })
const expandedId = ref<number | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res = await listAnnouncements({ page: pagination.page, size: pagination.size })
    if (res.errorCode === '00000') {
      items.value = res.body.content
      pagination.total = res.body.totalElements
    }
  } finally {
    loading.value = false
  }
}

async function toggleExpand(item: AnnouncementResponse) {
  if (expandedId.value === item.id) {
    expandedId.value = null
    return
  }
  expandedId.value = item.id

  // 標記已讀
  if (!item.isRead) {
    await markAsRead(item.id)
    item.isRead = true
    announcementStore.unreadCount = Math.max(0, announcementStore.unreadCount - 1)
  }
}

function handlePageChange(page: number) {
  pagination.page = page - 1
  expandedId.value = null
  fetchList()
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`
}

function getScopeLabel(item: AnnouncementResponse): string {
  return item.scope === 'ALL' ? t('announcement.scope.all') : t('announcement.scope.dept')
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ t('announcement.list.title') }}</h2>
      <p class="page-subtitle">{{ t('announcement.list.subtitle') }}</p>
    </div>

    <div v-loading="loading" class="announcement-list">
      <div v-if="items.length === 0 && !loading" class="empty-state">
        {{ t('announcement.bell.empty') }}
      </div>

      <div
        v-for="item in items"
        :key="item.id"
        class="announcement-card"
        :class="{ 'is-expanded': expandedId === item.id }"
        @click="toggleExpand(item)"
      >
        <div class="card-header">
          <div class="card-title-row">
            <span v-if="!item.isRead" class="unread-dot" />
            <span v-if="item.pinned" class="pin-icon">📌</span>
            <span class="card-title" :class="{ 'is-unread': !item.isRead }">
              {{ item.title }}
            </span>
            <el-tag size="small" type="info" class="scope-tag">{{ getScopeLabel(item) }}</el-tag>
          </div>
          <div class="card-meta">
            {{ formatDate(item.publishAt) }} ｜ {{ item.createdByName }}
          </div>
        </div>

        <transition name="expand">
          <div v-if="expandedId === item.id" class="card-content">
            <pre class="content-text">{{ item.content }}</pre>
          </div>
        </transition>
      </div>
    </div>

    <div class="pagination-bar">
      <el-pagination
        :current-page="pagination.page + 1"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.page-container {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-muted);
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-state {
  text-align: center;
  color: var(--text-muted);
  padding: 40px 0;
}

.announcement-card {
  border: 1px solid var(--border-divider);
  border-radius: 8px;
  padding: 14px 18px;
  cursor: pointer;
  transition: border-color 150ms ease, box-shadow 150ms ease;
  background: var(--bg-surface);
}

.announcement-card:hover {
  border-color: var(--border-medium);
}

.announcement-card.is-expanded {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-color-primary);
  flex-shrink: 0;
}

.pin-icon {
  font-size: 13px;
  flex-shrink: 0;
}

.card-title {
  font-size: 14px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-title.is-unread {
  color: var(--text-primary);
  font-weight: 600;
}

.scope-tag {
  flex-shrink: 0;
}

.card-meta {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
}

.card-content {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-divider);
}

.content-text {
  font-family: inherit;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 200ms ease;
  overflow: hidden;
}
.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
