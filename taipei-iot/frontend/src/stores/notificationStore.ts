import { defineStore } from 'pinia'
import {
  listNotifications,
  getNotificationUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
} from '@/api/notification'
import type { NotificationItem } from '@/types/notification'
import { Client } from '@stomp/stompjs'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    unreadCount: 0,
    items: [] as NotificationItem[],
    pollTimer: null as ReturnType<typeof setInterval> | null,
    stompClient: null as Client | null,
  }),

  actions: {
    async fetchUnreadCount() {
      try {
        const res = await getNotificationUnreadCount()
        if (res.errorCode === '00000') {
          this.unreadCount = res.body.count
        }
      } catch {
        // silently fail
      }
    },

    async fetchItems() {
      try {
        const res = await listNotifications({ page: 0, size: 10 })
        if (res.errorCode === '00000') {
          this.items = res.body.content
        }
      } catch {
        // silently fail
      }
    },

    async markRead(id: number) {
      try {
        await markNotificationRead(id)
        const item = this.items.find(i => i.id === id)
        if (item && !item.read) {
          item.read = true
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        }
      } catch {
        // silently fail
      }
    },

    async markAllRead() {
      try {
        await markAllNotificationsRead()
        this.unreadCount = 0
        this.items.forEach(i => { i.read = true })
      } catch {
        // silently fail
      }
    },

    connectWebSocket(token: string) {
      if (this.stompClient?.active) return

      const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsUrl = `${wsProtocol}//${window.location.host}/ws`

      this.stompClient = new Client({
        brokerURL: wsUrl,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          this.stompClient?.subscribe('/user/queue/notifications', (message) => {
            const notification = JSON.parse(message.body) as NotificationItem
            this.items.unshift(notification)
            if (this.items.length > 10) this.items.pop()
            this.unreadCount += 1
          })
        },
        onStompError: (frame) => {
          console.warn('STOMP error:', frame.headers['message'])
        },
      })

      this.stompClient.activate()
    },

    disconnectWebSocket() {
      if (this.stompClient?.active) {
        this.stompClient.deactivate()
        this.stompClient = null
      }
    },

    startPolling(token?: string) {
      this.fetchUnreadCount()
      this.fetchItems()

      // Try WebSocket first, fallback to polling
      if (token) {
        this.connectWebSocket(token)
      }

      // Keep polling as fallback (reduced interval when WS is active)
      if (this.pollTimer) clearInterval(this.pollTimer)
      this.pollTimer = setInterval(() => {
        this.fetchUnreadCount()
      }, 60 * 1000)
    },

    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
      this.disconnectWebSocket()
    },
  },
})
