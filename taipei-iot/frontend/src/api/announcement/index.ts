import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type { PageResponse } from '@/types/common'
import type {
  AnnouncementRequest,
  AnnouncementResponse,
  UnreadCountResponse,
} from '@/types/announcement'

// 前台查詢（已發佈 + 未過期 + 受眾符合）
export const listAnnouncements = (params: {
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<AnnouncementResponse>>>('/auth/announcements', { params })

// 管理端查詢（需 ANNOUNCEMENT_MANAGE 權限）
export const listAnnouncementsAdmin = (params: {
  statusFilter?: string
  keyword?: string
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<AnnouncementResponse>>>('/auth/announcements/admin', { params })

// 單筆公告詳情
export const getAnnouncement = (id: number) =>
  axiosIns.get<unknown, BaseResponse<AnnouncementResponse>>(`/auth/announcements/${id}`)

// 未讀數量
export const getUnreadCount = () =>
  axiosIns.get<unknown, BaseResponse<UnreadCountResponse>>('/auth/announcements/unread-count')

// 標記某則已讀
export const markAsRead = (id: number) =>
  axiosIns.post<unknown, BaseResponse<void>>(`/auth/announcements/${id}/read`)

// 全部已讀
export const markAllAsRead = () =>
  axiosIns.post<unknown, BaseResponse<void>>('/auth/announcements/read-all')

// 新增公告
export const createAnnouncement = (payload: AnnouncementRequest) =>
  axiosIns.post<unknown, BaseResponse<AnnouncementResponse>>('/auth/announcements', payload)

// 編輯公告
export const updateAnnouncement = (id: number, payload: AnnouncementRequest) =>
  axiosIns.put<unknown, BaseResponse<AnnouncementResponse>>(`/auth/announcements/${id}`, payload)

// 刪除公告
export const deleteAnnouncement = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/announcements/${id}`)
