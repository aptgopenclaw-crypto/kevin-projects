import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Client } from '@stomp/stompjs'
import { useAuthStore } from '@/stores/authStore'
import type { WidgetType } from '@/types/dashboard'

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'reconnecting'

export interface DashboardPushMessage {
  widget: string
  data: unknown
  timestamp: number
}

type WidgetCallback = (data: unknown) => void

/**
 * 儀表板 WebSocket composable — 透過 STOMP 接收即時推送。
 *
 * - 自動連線 / 斷線重連（指數退避）
 * - 訊息依 widget 字段分發到註冊的 callback
 * - 連線狀態指示
 */
export function useDashboardWebSocket() {
  const status = ref<ConnectionStatus>('disconnected')
  const callbacks = new Map<string, Set<WidgetCallback>>()

  let client: Client | null = null

  function getWsUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}/ws`
  }

  function connect() {
    const authStore = useAuthStore()
    const token = authStore.accessToken
    if (!token) return

    status.value = 'connecting'

    client = new Client({
      brokerURL: getWsUrl(),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: () => {
        status.value = 'connected'
        // Subscribe to tenant dashboard topic
        // tenantId is embedded in JWT — backend routes by tenant
        client?.subscribe('/user/topic/dashboard', (message) => {
          try {
            const payload: DashboardPushMessage = JSON.parse(message.body)
            dispatch(payload)
          } catch { /* ignore malformed */ }
        })
        // Also subscribe to broadcast tenant topic
        client?.subscribe('/topic/dashboard', (message) => {
          try {
            const payload: DashboardPushMessage = JSON.parse(message.body)
            dispatch(payload)
          } catch { /* ignore malformed */ }
        })
      },

      onStompError: (frame) => {
        console.warn('[Dashboard WS] STOMP error:', frame.headers['message'])
        status.value = 'reconnecting'
      },

      onWebSocketClose: () => {
        if (status.value !== 'disconnected') {
          status.value = 'reconnecting'
        }
      },

      onDisconnect: () => {
        status.value = 'disconnected'
      },
    })

    client.activate()
  }

  function disconnect() {
    status.value = 'disconnected'
    client?.deactivate()
    client = null
  }

  function dispatch(message: DashboardPushMessage) {
    const widgetCallbacks = callbacks.get(message.widget)
    if (widgetCallbacks) {
      widgetCallbacks.forEach(cb => cb(message.data))
    }
  }

  /**
   * 註冊 widget callback — 收到對應 widget 推送時呼叫。
   * 返回 unsubscribe function。
   */
  function onWidgetUpdate(widgetType: WidgetType, callback: WidgetCallback): () => void {
    if (!callbacks.has(widgetType)) {
      callbacks.set(widgetType, new Set())
    }
    callbacks.get(widgetType)!.add(callback)

    return () => {
      callbacks.get(widgetType)?.delete(callback)
    }
  }

  onMounted(() => {
    connect()
  })

  onBeforeUnmount(() => {
    disconnect()
  })

  return {
    status,
    connect,
    disconnect,
    onWidgetUpdate,
  }
}
