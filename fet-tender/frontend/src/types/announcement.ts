export interface AnnouncementResponse {
  id: number
  title: string
  content: string
  status: string
  scope: string
  targetDeptIds: number[]
  targetDeptNames: string[]
  pinned: boolean
  publishAt: string | null
  expireAt: string | null
  createdBy: string
  createdByName: string
  createdAt: string
  updatedAt: string
  isRead: boolean
  editable: boolean
}

export interface AnnouncementRequest {
  title: string
  content: string
  status: string
  scope: string
  targetDeptIds?: number[]
  pinned?: boolean
  publishAt?: string | null
  expireAt?: string | null
}

export interface UnreadCountResponse {
  count: number
}

// Use PageResponse<AnnouncementResponse> from @/types/common instead
