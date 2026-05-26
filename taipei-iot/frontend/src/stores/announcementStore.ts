import { defineStore } from 'pinia'
import { getUnreadCount, listAnnouncements, markAsRead, markAllAsRead } from '@/api/announcement'
import type { AnnouncementResponse } from '@/types/announcement'

export const useAnnouncementStore = defineStore('announcement', {
  state: () => ({
    unreadCount: 0,
    popoverItems: [] as AnnouncementResponse[],
    // List view 共享狀態：下沉至 store 以集中已讀同步邏輯
    listItems: [] as AnnouncementResponse[],
    listLoading: false,
    listPagination: { page: 0, size: 10, total: 0 },
    /** 前台列表分類過濾；空串 / null = 全部 */
    listCategory: null as string | null,
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

    /** List view 分頁載入；由 view 觸發但狀態由 store 管理 */
    async fetchListPage(page?: number, size?: number) {
      if (typeof page === 'number') this.listPagination.page = page
      if (typeof size === 'number') this.listPagination.size = size
      this.listLoading = true
      try {
        const res = await listAnnouncements({
          page: this.listPagination.page,
          size: this.listPagination.size,
          category: this.listCategory ?? undefined,
        })
        if (res.errorCode === '00000') {
          this.listItems = res.body.content
          this.listPagination.total = res.body.totalElements
        }
      } finally {
        this.listLoading = false
      }
    },

    /** 切換分類過濾時重載第一頁 */
    setListCategory(category: string | null) {
      this.listCategory = category && category !== 'ALL' ? category : null
      this.listPagination.page = 0
      return this.fetchListPage()
    },

    /** 內部：同步所有快取列表的 isRead 狀態並遞減 unreadCount */
    _applyMarkRead(id: number) {
      let changed = false
      const sync = (list: AnnouncementResponse[]) => {
        const item = list.find(i => i.id === id)
        if (item && !item.isRead) {
          item.isRead = true
          changed = true
        }
      }
      sync(this.popoverItems)
      sync(this.listItems)
      if (changed) {
        this.decrementUnread()
      }
    },

    decrementUnread(by = 1) {
      this.unreadCount = Math.max(0, this.unreadCount - by)
    },

    async markRead(id: number) {
      try {
        await markAsRead(id)
        this._applyMarkRead(id)
      } catch (e) {
        console.warn('[announcementStore] markRead failed:', e)
      }
    },

    async markAllRead() {
      try {
        await markAllAsRead()
        this.unreadCount = 0
        this.popoverItems.forEach(i => { i.isRead = true })
        this.listItems.forEach(i => { i.isRead = true })
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
