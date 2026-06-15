export type AssetTransferStatus = 'DRAFT' | 'PROCESSING' | 'COMPLETED' | 'REJECTED' | 'CANCELLED'

export interface AssetTransferApplicationDto {
  id: number
  applicationNo: string
  applicantId: string
  applicantName: string | null
  departmentId: number
  departmentName: string | null
  assetCode: string
  assetName: string
  transferType: string
  targetDepartmentId: number | null
  reason: string | null
  assetValue: number | null
  status: AssetTransferStatus
  currentAssignee: string | null
  currentAssigneeName: string | null
  createdAt: string
  createdBy: string | null
  updatedAt: string
  approvedAt: string | null
  approvedBy: string | null
  rejectReason: string | null
}

export interface AssetTransferCreateRequest {
  assetCode: string
  assetName: string
  transferType: string
  departmentId: number
  targetDepartmentId?: number | null
  reason?: string | null
  assetValue?: number | null
}

export interface AssetTransferActionRequest {
  comment?: string | null
}

export interface AssetTransferRejectRequest {
  comment?: string | null
  targetStepId: string
}

export interface RejectTargetOption {
  stepId: string
  stepName: string
}
