export interface NotificationItem {
  id: number
  type: 'TODO' | 'ALERT' | 'INFO'
  title: string
  content: string | null
  refType: string | null
  refId: string | null
  read: boolean
  readAt: string | null
  createdAt: string
}

export interface NotificationPageResponse {
  content: NotificationItem[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface NotificationUnreadCount {
  count: number
}
