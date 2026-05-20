import { defineStore } from 'pinia'
import { getUnreadCount, listAnnouncements, markAsRead, markAllAsRead } from '@/api/announcement'
import type { AnnouncementResponse } from '@/types/announcement'

export const useAnnouncementStore = defineStore('announcement', {
  state: () => ({
    unreadCount: 0,
    popoverItems: [] as AnnouncementResponse[],
    pollTimer: null as ReturnType<typeof setInterval> | null,
  }),

  actions: {
    async fetchUnreadCount() {
      try {
        const res = await getUnreadCount()
        if (res.errorCode === '00000') {
          this.unreadCount = res.body.count
        }
      } catch (e) {
        console.warn('[announcementStore] fetchUnreadCount failed:', e)
      }
    },

    async fetchPopoverItems() {
      try {
        const res = await listAnnouncements({ page: 0, size: 10 })
        if (res.errorCode === '00000') {
          this.popoverItems = res.body.content
        }
      } catch (e) {
        console.warn('[announcementStore] fetchPopoverItems failed:', e)
      }
    },

    async markRead(id: number) {
      try {
        await markAsRead(id)
        // Update local state
        const item = this.popoverItems.find(i => i.id === id)
        if (item && !item.isRead) {
          item.isRead = true
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        }
      } catch (e) {
        console.warn('[announcementStore] markRead failed:', e)
      }
    },

    async markAllRead() {
      try {
        await markAllAsRead()
        this.unreadCount = 0
        this.popoverItems.forEach(i => { i.isRead = true })
      } catch (e) {
        console.warn('[announcementStore] markAllRead failed:', e)
      }
    },

    startPolling() {
      this.fetchUnreadCount()
      this.fetchPopoverItems()
      if (this.pollTimer) clearInterval(this.pollTimer)
      this.pollTimer = setInterval(() => {
        this.fetchUnreadCount()
      }, 5 * 60 * 1000) // 5 分鐘
    },

    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
  },
})
