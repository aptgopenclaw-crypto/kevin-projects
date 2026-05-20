// ── 關鍵字設定 ────────────────────────────────────────────────
export interface SearchKeywordResponse {
  id: number
  solution: string
  keyword: string
  isActive: boolean
  createdAt: string
}

export interface SearchKeywordRequest {
  solution: string
  keyword: string
  isActive: boolean
}

// ── 機關過濾設定 ──────────────────────────────────────────────
export interface AgencyFilterResponse {
  id: number
  solution: string
  agencyKeyword: string
  isOrgOnlySearch: boolean
  isActive: boolean
  createdAt: string
}

export interface AgencyFilterRequest {
  solution: string
  agencyKeyword: string
  isOrgOnlySearch: boolean
  isActive: boolean
}

// ── 採購公告 ──────────────────────────────────────────────────
export interface TenderAnnouncementResponse {
  id: number
  solution: string
  matchedKeyword: string
  agencyName: string
  tenderNumber: string
  tenderName: string
  transmissionCount: number
  tenderMethod: string
  procurementType: string
  announcementDate: string
  deadline: string
  budgetAmountRaw: string
  budgetAmount: number | null
  detailUrl: string
  agencyCode: string
  unitName: string
  agencyAddress: string
  contactPerson: string
  contactPhone: string
  contactEmail: string
  tenderCategory: string
  procurementAmountRange: string
  handlingMethod: string
  awardMethod: string
  tenderStatus: string
  openingTime: string
  openingLocation: string
  hasBasePrice: boolean | null
  performanceLocation: string
  scrapedAt: string
}

export interface TenderAnnouncementQueryRequest {
  solution?: string
  keyword?: string
  agency?: string
  name?: string
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
}

export interface TenderAnnouncementPageResponse {
  content: TenderAnnouncementResponse[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

// ── 決標公告 ──────────────────────────────────────────────────────
export interface TenderAwardResponse {
  id: number
  solution: string
  matchedKeyword: string
  agencyName: string
  tenderNumber: string
  tenderName: string
  tenderMethod: string
  procurementType: string
  awardAnnounceDate: string
  awardAmountRaw: string
  awardAmount: number | null
  awardAnnounceSeq: string
  detailUrl: string
  agencyCode: string
  unitName: string
  agencyAddress: string
  contactPerson: string
  contactPhone: string
  contactEmail: string
  tenderCategory: string
  procurementAmountRange: string
  awardMethod: string
  hasBasePrice: boolean | null
  awardDate: string | null
  performancePeriod: string
  performanceLocation: string
  vendorOrderSeq: number
  vendorName: string
  vendorTaxId: string
  vendorAddress: string
  vendorPhone: string
  vendorAwardAmountRaw: string
  vendorAwardAmount: number | null
  scrapedAt: string
}

export interface TenderAwardQueryRequest {
  solution?: string
  keyword?: string
  agency?: string
  name?: string
  vendorName?: string
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
}

export interface TenderAwardPageResponse {
  content: TenderAwardResponse[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

// ── AI 聊天 ──────────────────────────────────────────────────────
export interface TenderChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface TenderChatRequest {
  message: string
  history?: TenderChatMessage[]
}

export interface TenderChatResponse {
  message: string
  functionCalled: string | null
  data?: any
}

// ── 廠商 Dashboard ────────────────────────────────────────────────────────────

export interface VendorSuggestResponse {
  vendorName: string
  vendorTaxId: string | null
  winCount: number
}

export interface VendorOverviewResponse {
  vendorName: string
  vendorTaxId: string | null
  totalWins: number
  totalAmount: number
  agencyCount: number
  solutionCount: number
  firstAwardDate: string | null
  latestAwardDate: string | null
}

export interface VendorTrendPoint {
  period: string
  count: number
  totalAmount: number
}

export interface VendorTrendResponse {
  granularity: 'DAY' | 'MONTH' | 'QUARTER'
  points: VendorTrendPoint[]
}

export interface VendorKeywordNode {
  keyword: string
  count: number
  totalAmount: number
}

export interface VendorSolutionNode {
  solution: string
  count: number
  totalAmount: number
  keywords: VendorKeywordNode[]
}

export interface VendorTopAgencyResponse {
  agencyName: string
  agencyCode: string | null
  count: number
  totalAmount: number
}

export interface VendorNameCount {
  name: string
  count: number
}

export interface VendorProcurementProfileResponse {
  tenderMethods: VendorNameCount[]
  procurementTypes: VendorNameCount[]
  awardMethods: VendorNameCount[]
}

export interface VendorCoVendorResponse {
  vendorName: string
  vendorTaxId: string | null
  coCount: number
}

// ── Solution 競品分析 ─────────────────────────────────────────────────────────

export interface SolutionCompetitorSummaryResponse {
  totalTenders: number
  totalAmount: number
  vendorCount: number
  keywordCount: number
}

export interface SolutionVendorRankResponse {
  rank: number
  vendorName: string
  vendorTaxId: string | null
  winCount: number
  totalAmount: number
}

export interface SolutionKeywordSummaryResponse {
  keyword: string
  vendorCount: number
  winCount: number
  totalAmount: number
}

export interface SolutionVendorRankPageResponse {
  content: SolutionVendorRankResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

