import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type {
  TenderDashboardResponse,
  SearchKeywordRequest,
  SearchKeywordResponse,
  AgencyFilterRequest,
  AgencyFilterResponse,
  TenderAnnouncementQueryRequest,
  TenderAnnouncementResponse,
  TenderAnnouncementPageResponse,
  TenderAwardQueryRequest,
  TenderAwardResponse,
  TenderAwardPageResponse,
  TenderChatRequest,
  TenderChatResponse,
  VendorSuggestResponse,
  VendorOverviewResponse,
  VendorTrendResponse,
  VendorSolutionNode,
  VendorTopAgencyResponse,
  VendorProcurementProfileResponse,
  VendorCoVendorResponse,
  SolutionCompetitorSummaryResponse,
  SolutionVendorRankPageResponse,
  SolutionKeywordSummaryResponse,
  MailRecipientRequest,
  MailRecipientResponse,
  MailRecipientBatchResult,
} from '@/types/tender'

// ── Dashboard 總覽 ────────────────────────────────────────────
export const getTenderDashboard = () =>
  axiosIns.get<unknown, BaseResponse<TenderDashboardResponse>>('/tender/dashboard')

// ── 搜尋關鍵字 ────────────────────────────────────────────────
export const listSearchKeywords = (includeInactive = false, solution?: string) =>
  axiosIns.get<unknown, BaseResponse<SearchKeywordResponse[]>>(
    '/tender/announcement-keywords',
    { params: { includeInactive, ...(solution ? { solution } : {}) } },
  )

export const createSearchKeyword = (payload: SearchKeywordRequest) =>
  axiosIns.post<unknown, BaseResponse<SearchKeywordResponse>>(
    '/tender/announcement-keywords',
    payload,
  )

export const updateSearchKeyword = (id: number, payload: SearchKeywordRequest) =>
  axiosIns.put<unknown, BaseResponse<SearchKeywordResponse>>(
    `/tender/announcement-keywords/${id}`,
    payload,
  )

export const deleteSearchKeyword = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(
    `/tender/announcement-keywords/${id}`,
  )

export const listConfigSolutions = () =>
  axiosIns.get<unknown, BaseResponse<string[]>>(
    '/tender/announcement-keywords/solutions',
  )

// ── 機關過濾設定 ──────────────────────────────────────────────
export const listAgencyFilters = (includeInactive = false, solution?: string) =>
  axiosIns.get<unknown, BaseResponse<AgencyFilterResponse[]>>(
    '/tender/announcement-agency-filters',
    { params: { includeInactive, ...(solution ? { solution } : {}) } },
  )

export const createAgencyFilter = (payload: AgencyFilterRequest) =>
  axiosIns.post<unknown, BaseResponse<AgencyFilterResponse>>(
    '/tender/announcement-agency-filters',
    payload,
  )

export const updateAgencyFilter = (id: number, payload: AgencyFilterRequest) =>
  axiosIns.put<unknown, BaseResponse<AgencyFilterResponse>>(
    `/tender/announcement-agency-filters/${id}`,
    payload,
  )

export const deleteAgencyFilter = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(
    `/tender/announcement-agency-filters/${id}`,
  )

// ── 郵件收件人設定 ────────────────────────────────────────────
export const listMailRecipients = (includeInactive = false) =>
  axiosIns.get<unknown, BaseResponse<MailRecipientResponse[]>>(
    '/tender/mail-recipients',
    { params: { includeInactive } },
  )

export const createMailRecipient = (payload: MailRecipientRequest) =>
  axiosIns.post<unknown, BaseResponse<MailRecipientResponse>>(
    '/tender/mail-recipients',
    payload,
  )

export const updateMailRecipient = (id: number, payload: MailRecipientRequest) =>
  axiosIns.put<unknown, BaseResponse<MailRecipientResponse>>(
    `/tender/mail-recipients/${id}`,
    payload,
  )

export const deleteMailRecipient = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(
    `/tender/mail-recipients/${id}`,
  )

export const batchImportMailRecipients = (emails: string[]) =>
  axiosIns.post<unknown, BaseResponse<MailRecipientBatchResult>>(
    '/tender/mail-recipients/batch',
    { emails },
  )

// ── 採購公告 ──────────────────────────────────────────────────
export const searchTenderAnnouncements = (params: TenderAnnouncementQueryRequest) =>
  axiosIns.get<unknown, BaseResponse<TenderAnnouncementPageResponse>>(
    '/tender/announcements',
    { params },
  )

export const getTenderAnnouncementById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<TenderAnnouncementResponse>>(
    `/tender/announcements/${id}`,
  )

export const deleteTenderAnnouncement = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(
    `/tender/announcements/${id}`,
  )

// ── AI 聊天 ───────────────────────────────────────────────────
export const tenderChat = (payload: TenderChatRequest, signal?: AbortSignal) =>
  axiosIns.post<unknown, BaseResponse<TenderChatResponse>>(
    '/tender/chat',
    payload,
    { timeout: 120000, signal }, // LLM 回應最多等 120 秒
  )

// ── 決標公告 ──────────────────────────────────────────────────
export const searchTenderAwards = (params: TenderAwardQueryRequest) =>
  axiosIns.get<unknown, BaseResponse<TenderAwardPageResponse>>(
    '/tender/awards',
    { params },
  )

export const getTenderAwardById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<TenderAwardResponse>>(
    `/tender/awards/${id}`,
  )

export const deleteTenderAward = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(
    `/tender/awards/${id}`,
  )

// ── 廠商 Dashboard ────────────────────────────────────────────
export const suggestVendors = (q: string) =>
  axiosIns.get<unknown, BaseResponse<VendorSuggestResponse[]>>(
    '/tender/vendor-dashboard/suggest',
    { params: { q } },
  )

export const getVendorOverview = (vendorTaxId: string | null, vendorName: string) =>
  axiosIns.get<unknown, BaseResponse<VendorOverviewResponse>>(
    '/tender/vendor-dashboard/overview',
    { params: { vendorTaxId, vendorName } },
  )

export const getVendorTrend = (vendorTaxId: string | null, vendorName: string) =>
  axiosIns.get<unknown, BaseResponse<VendorTrendResponse>>(
    '/tender/vendor-dashboard/trend',
    { params: { vendorTaxId, vendorName } },
  )

export const getVendorSolutionBreakdown = (vendorTaxId: string | null, vendorName: string) =>
  axiosIns.get<unknown, BaseResponse<VendorSolutionNode[]>>(
    '/tender/vendor-dashboard/solution-breakdown',
    { params: { vendorTaxId, vendorName } },
  )

export const getVendorTopAgencies = (vendorTaxId: string | null, vendorName: string, limit = 10) =>
  axiosIns.get<unknown, BaseResponse<VendorTopAgencyResponse[]>>(
    '/tender/vendor-dashboard/top-agencies',
    { params: { vendorTaxId, vendorName, limit } },
  )

export const getVendorProcurementProfile = (vendorTaxId: string | null, vendorName: string) =>
  axiosIns.get<unknown, BaseResponse<VendorProcurementProfileResponse>>(
    '/tender/vendor-dashboard/procurement-profile',
    { params: { vendorTaxId, vendorName } },
  )

export const getVendorCoVendors = (vendorTaxId: string | null, vendorName: string) =>
  axiosIns.get<unknown, BaseResponse<VendorCoVendorResponse[]>>(
    '/tender/vendor-dashboard/co-vendors',
    { params: { vendorTaxId, vendorName } },
  )

// ── Solution 競品分析 ─────────────────────────────────────────
export const listSolutionOptions = () =>
  axiosIns.get<unknown, BaseResponse<string[]>>(
    '/tender/solution-competitor/solutions',
  )

export const getSolutionCompetitorSummary = (
  solution: string,
  keyword?: string,
  dateFrom?: string,
  dateTo?: string,
) =>
  axiosIns.get<unknown, BaseResponse<SolutionCompetitorSummaryResponse>>(
    '/tender/solution-competitor/summary',
    { params: { solution, keyword, dateFrom, dateTo } },
  )

export const getSolutionVendorRank = (params: {
  solution: string
  keyword?: string
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<SolutionVendorRankPageResponse>>(
    '/tender/solution-competitor/vendor-rank',
    { params },
  )

export const getSolutionKeywordSummary = (
  solution: string,
  dateFrom?: string,
  dateTo?: string,
) =>
  axiosIns.get<unknown, BaseResponse<SolutionKeywordSummaryResponse[]>>(
    '/tender/solution-competitor/keyword-summary',
    { params: { solution, dateFrom, dateTo } },
  )

